package com.webtic.qrprint.ui

import android.app.ProgressDialog
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.webtic.qrprint.models.QuantityDetails
import com.webtic.qrprint.util.AppConstants.DB_QTY_TBL_NAME
import com.webtic.qrprint.util.ConnectionManager
import com.webtic.qrprint.util.QueryError
import com.webtic.qrprint.util.QuerySuccess
import com.webtic.qrprint.viewmodels.LoginViewModel
import javax.inject.Inject

open class BaseFragment: Fragment() {
    @Inject
    lateinit var connectionManager: ConnectionManager
    val viewModel: LoginViewModel by activityViewModels()
    lateinit var progressDialog: ProgressDialog
    fun showLoadingSpinner(title: String, message: String){
        activity?.runOnUiThread {
            progressDialog = ProgressDialog(requireActivity())
            progressDialog.setTitle(title)
            progressDialog.setMessage(message)
            progressDialog.setCancelable(false)
            progressDialog.show()
            Log.e("Fragment", "Show Loading Dailog")
        }

    }
    fun hideLoadingSpinner(){
        if(this::progressDialog.isInitialized){
            progressDialog.dismiss()
            Log.e("Fragment", "Hide Loading Dailog")
        }
    }
    fun saveQuantityDetails(quantityDetails: QuantityDetails): Boolean{
        var result  = checkQuantityTable()
        if (result){
            var insertOrUpdateQuantityQuery = "MERGE INTO ${DB_QTY_TBL_NAME} AS Target\n" +
                    "USING (VALUES (${quantityDetails.kbid}, ${quantityDetails.SZ}, ${quantityDetails.MZ}, ${quantityDetails.LZ}, ${quantityDetails.SD}, ${quantityDetails.MD}, ${quantityDetails.LD}, ${quantityDetails.SR}, ${quantityDetails.MR}, ${quantityDetails.LR}, ${quantityDetails.SP}, ${quantityDetails.MP}, ${quantityDetails.LP})) AS Source (kbid, SZ, MZ, LZ, SD, MD, LD, SR, MR, LR, SP, MP, LP)\n" +
                    "    ON Target.kbid = Source.kbid -- Specify the primary key column for matching\n" +
                    "WHEN MATCHED THEN\n" +
                    "    UPDATE SET Target.SZ = Source.SZ, Target.MZ = Source.MZ, Target.LZ = Source.LZ, Target.SD = Source.SD, Target.MD = Source.MD, Target.LD = Source.LD, Target.SR = Source.SR, Target.MR = Source.MR, Target.LR = Source.LR, Target.SP = Source.SP, Target.MP = Source.MP, Target.LP = Source.LP\n" +
                    "WHEN NOT MATCHED THEN\n" +
                    "    INSERT (kbid, SZ, MZ, LZ, SD, MD, LD, SR, MR, LR, SP, MP, LP)\n" +
                    "    VALUES (${quantityDetails.kbid}, ${quantityDetails.SZ}, ${quantityDetails.MZ}, ${quantityDetails.LZ}, ${quantityDetails.SD}, ${quantityDetails.MD}, ${quantityDetails.LD}, ${quantityDetails.SR}, ${quantityDetails.MR}, ${quantityDetails.LR}, ${quantityDetails.SP}, ${quantityDetails.MP}, ${quantityDetails.LP});"
            Log.e("QUery1", insertOrUpdateQuantityQuery)
            val result1 = connectionManager.execute(insertOrUpdateQuantityQuery)
            if(result1) {
                Log.e("QuerySuccess", "Success")
                return true
            }
            else {
                Log.e("QueryError", "Error:")
                return false
            }
            return false
        }
        else {
            Log.e("QueryError", "Query Error!! ")
            return false
        }

    }

    private fun checkQuantityTable(): Boolean {
        var createQuantitiesTableQuery = "IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = '${DB_QTY_TBL_NAME}')\n" +
                "BEGIN\n" +
                "    CREATE TABLE ${DB_QTY_TBL_NAME} (\n" +
                "    kbid bigint,\n" +
                "    SZ  int,\n" +
                "    MZ   int,\n" +
                "    LZ   int,\n" +
                "    SD   int,\n" +
                "    MD   int,\n" +
                "    LD   int,\n" +
                "    SR   int,\n" +
                "    MR   int,\n" +
                "    LR   int,\n" +
                "    SP   int,\n" +
                "    MP   int,\n" +
                "    LP   int,\n" +
                "        CONSTRAINT PK_${DB_QTY_TBL_NAME} PRIMARY KEY (kbid)\n" +
                "    );\n" +
                "END;"

        val result = connectionManager.execute(createQuantitiesTableQuery)
        return result
    }

    fun getQuantityDetails(kbid: String): QuantityDetails {
        var result = checkQuantityTable()
        var quantityDetails = QuantityDetails(kbid = kbid.toLong())
        var getQuantityDetailsQuery = "SELECT * FROM [ZETAEPRMENNY] where kbid = ${kbid}"
        when (val result = connectionManager.executeQuery(getQuantityDetailsQuery)) {
            is QuerySuccess -> {
                if(result.resultSet.next()){
                    quantityDetails.SZ = result.resultSet.getInt("SZ")
                    quantityDetails.MZ = result.resultSet.getInt("MZ")
                    quantityDetails.LZ = result.resultSet.getInt("LZ")
                    quantityDetails.SD = result.resultSet.getInt("SD")
                    quantityDetails.MD = result.resultSet.getInt("MD")
                    quantityDetails.LD = result.resultSet.getInt("LD")
                    quantityDetails.SR = result.resultSet.getInt("SR")
                    quantityDetails.MR = result.resultSet.getInt("MR")
                    quantityDetails.LR = result.resultSet.getInt("LR")
                    quantityDetails.SP = result.resultSet.getInt("SP")
                    quantityDetails.MP = result.resultSet.getInt("MP")
                    quantityDetails.LP = result.resultSet.getInt("LP")
                }
            }
            is QueryError -> Log.e("QueryError", "main " + result.exception.message.toString())
        }
        return quantityDetails
    }

    fun updateTable(tblName: String, columnName: String, value: String, whereCond: String): Boolean{
        var updateQuery = """
            UPDATE $tblName
            SET $columnName = '$value'
            WHERE $whereCond
        """
        Log.e("UDPATE_TABLE", updateQuery)
        return  connectionManager.execute(updateQuery)
    }
    fun insertTable(tblName: String, columnName: String, value: String, whereCond: String): Boolean{
        val columnRegex = Regex("\\s*(\\S+)\\s*=\\s*(\\S+)") // Regex pattern to match the column name and value

        val columnMatchResult = columnRegex.find(whereCond)
        val whereColName = columnMatchResult?.groupValues?.get(1)?.trim()
        val whereColValue = columnMatchResult?.groupValues?.get(2)?.trim()
        Log.e("WHERE_", "$whereColName")
        Log.e("WHERE_", "$whereColValue")
        val insertQuery = "INSERT INTO $tblName ($columnName, $whereColName) VALUES ('$value', '$whereColValue')"
        Log.e("INSERT_QUERY", insertQuery)

        return connectionManager.execute(insertQuery)
    }
    fun insertOrUpdateTable(tblName: String, columnName: String, value: String, whereCond: String): Boolean {
        val selectQuery = "SELECT COUNT(*) as count_result FROM $tblName WHERE $whereCond"
        when (val result = connectionManager.executeQuery(selectQuery)) {
            is QuerySuccess -> {
                if(result.resultSet.next()){
                    if(result.resultSet.getInt("count_result") > 0){
                        return updateTable(tblName, columnName, value, whereCond)
                    }
                    else{
                        return insertTable(tblName, columnName, value, whereCond)
                    }
                }
                else{
                    return insertTable(tblName, columnName, value, whereCond)
                }
            }
            is QueryError -> Log.e("QueryError", "main " + result.exception.message.toString())
        }
        return false

    }

    fun checkMarkingTable(tableName: String): Boolean {
        var createQuantitiesTableQuery = "IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = '${tableName}_marking')\n" +
                "BEGIN\n" +
                "    CREATE TABLE ${tableName}_marking (\n" +
                "    id bigint,\n" +
                "    CHECKED  bit,\n" +
                "        CONSTRAINT PK_${tableName}_marking PRIMARY KEY (id)\n" +
                "    );\n" +
                "END;"

        val result = connectionManager.execute(createQuantitiesTableQuery)
        return result
    }
    fun checkColumnForTable(columnName: String, tableName: String): Boolean{
        val checkColumnExistsQuery = """
                SELECT COLUMN_NAME
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME = '$tableName' AND COLUMN_NAME = '$columnName'
            """

        val resultSet = connectionManager.executeQuery(checkColumnExistsQuery)
        val columnExists = resultSet is QuerySuccess && resultSet.resultSet.next()
        Log.e("CHECK_COL", "${columnExists}")
        if (!columnExists) {
            val addColumnQuery = """
                    ALTER TABLE $tableName
                    ADD $columnName BIT NOT NULL DEFAULT 0
                """

            val result = connectionManager.execute(addColumnQuery)
            return if (result ) {
                Log.e("CHECK_COL", "TRUE")
                true
            } else {
                Log.e("CHECK_COL", "FALSE")
                false
            }
        }
        return true
    }
}