package com.webtic.qrprint.ui.bills

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableLayout
import android.widget.TextView
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.webtic.qrprint.R
import com.webtic.qrprint.util.AppConstants.DB_NAME
import com.webtic.qrprint.util.ConnectionManager
import com.webtic.qrprint.util.QueryError
import com.webtic.qrprint.util.QuerySuccess
import dagger.hilt.android.AndroidEntryPoint
import java.sql.Date
import java.sql.ResultSet
import javax.inject.Inject

@AndroidEntryPoint
class BillsFragment : Fragment() {

    @Inject
    lateinit var connectionManager: ConnectionManager
    private lateinit var table: TableLayout
    private lateinit var header: ViewGroup
    private val rows = mutableListOf<Pair<BillItem, View>>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_bills, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        table = view.findViewById(R.id.billsTable)
        header = table.children.first() as ViewGroup
        initRowSort()
    }

    override fun onStart() {
        super.onStart()
        try {
            when (val result = connectionManager.executeQuery(BILLS_SQL)) {
                is QuerySuccess -> updateTable(result.resultSet)
                is QueryError -> Log.e(
                    "QueryError",
                    "main " + result.exception.message.toString()
                )
            }
        } catch (e: Exception) {
            Log.e("FETCH_OR_DISPLAY_DATA", e.message.toString())
        }
    }

    private fun initRowSort() {
        header.children.forEach { children ->
            children.setOnClickListener { clickedView ->
                table.removeViews(1, table.childCount - 1)
                rows.sortedBy {
                    when (clickedView.id) {
                        R.id.ugyfelRovidNev -> it.first.ugyfelRovidNev
                        R.id.szallitRovidNev -> it.first.szallitRovidNev
                        R.id.szamlaszam -> it.first.szamlaszam
                        R.id.kelte -> it.first.kelte
                        R.id.netto -> it.first.netto
                        R.id.suly -> it.first.suly
                        else -> it.first.sziktszam
                    } as Comparable<Any>
                }.forEach { pair ->
                    if (pair.second.parent != null)
                        (pair.second.parent as ViewGroup).removeView(pair.second)
                    table.addView(pair.second)
                }
            }
            children.setOnLongClickListener { clickedView ->
                table.removeViews(1, table.childCount - 1)
                rows.sortedByDescending {
                    when (clickedView.id) {
                        R.id.ugyfelRovidNev -> it.first.ugyfelRovidNev
                        R.id.szallitRovidNev -> it.first.szallitRovidNev
                        R.id.szamlaszam -> it.first.szamlaszam
                        R.id.kelte -> it.first.kelte
                        R.id.netto -> it.first.netto
                        R.id.suly -> it.first.suly
                        else -> it.first.sziktszam
                    } as Comparable<Any>
                }.forEach { pair ->
                    if (pair.second.parent != null)
                        (pair.second.parent as ViewGroup).removeView(pair.second)
                    table.addView(pair.second)
                }
                true
            }
        }
    }

    private fun updateTable(result: ResultSet) {
        rows.clear()
        while (result.next()) {
            val row = BillItem(
                ugyfelRovidNev = result.getString("Ügyfél_rövid_név"),
                szallitRovidNev = result.getString("Szállít_rövid_név"),
                sziktszam = result.getInt("SZIKTSZAM"),
                szamlaszam = result.getString("Számlaszám"),
                kelte = result.getDate("Kelte") ?: Date(0L),
                netto = result.getDouble("Nettó"),
                suly = result.getDouble("Br_suly"),
            )
            val rowView = layoutInflater.inflate(R.layout.template_bill_table_row, null, false)
            rows.add(Pair(row, rowView))

            rowView.findViewById<TextView>(R.id.ugyfelRovidNev).text = row.ugyfelRovidNev
            rowView.findViewById<TextView>(R.id.szallitRovidNev).text = row.szallitRovidNev
            rowView.findViewById<TextView>(R.id.szamlaszam).run {
                text = row.szamlaszam
                setOnClickListener {
                    findNavController().navigate(
                        BillsFragmentDirections.actionBillsFragmentToBillDetails(
                            row.sziktszam.toString()
                        )
                    )
                }
            }
            rowView.findViewById<TextView>(R.id.kelte).text =
                if (row.kelte.time != 0L) row.kelte.toString().split(" ").first() else ""
            rowView.findViewById<TextView>(R.id.netto).text = row.netto.toString()
            rowView.findViewById<TextView>(R.id.suly).text = row.suly.toString()

            table.addView(rowView)
        }
        Log.d("SQLRESULT", "rows fetched: ${rows.size}")
    }

    private companion object {
        var BILLS_SQL = "select VC.UGYFNEV Ügyfél_rövid_név, \n" +
                "SZC.UGYFNEV Szállít_rövid_név, \n" +
                "SZIKTSZAM, \n" +
                "SZLASZAM Számlaszám, \n" +
                "SZKELTE Kelte, \n" +
                "round(SZNETTOERT,2) Nettó,\n" +
                "round(( SELECT sum(tetelmenny*convert(float,replace(replace(jellemzo,',','.'),' ',''))) Súly \n" +
                "  FROM ${DB_NAME}.dbo.jellemzok, ${DB_NAME}.dbo.tetel  \n" +
                "  WHERE tetel.sziktszam =SZAMLA.sziktszam  and jellemzok.etk = tetel.etk \n" +
                "    and jellemzok.jelmegnev1 in ('Bruttó kg','Bruttó súly') \n" +
                "    and not jellemzok.jellemzo is null and isnumeric(jellemzo)=1),2) Br_suly\n" +
                " from ${DB_NAME}.dbo.SZAMLA,${DB_NAME}.dbo.UGYFEL SZC,${DB_NAME}.dbo.UGYFEL VC \n" +
                " where SZC.UGYFELKOD=SZAMLA.ATVEVOKOD \n" +
                " and VC.UGYFELKOD=SZAMLA.UGYFELKOD \n" +
                " and szkelte>dateadd(day,-4,SYSDATETIME()) " +
                "order by SZIKTSZAM desc"
    }
}

