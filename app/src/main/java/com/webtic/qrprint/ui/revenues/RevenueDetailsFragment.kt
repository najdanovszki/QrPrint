package com.webtic.qrprint.ui.revenues

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TableLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.webtic.qrprint.R
import com.webtic.qrprint.models.QuantityDetails
import com.webtic.qrprint.models.SubDetailsTableItem
import com.webtic.qrprint.ui.BaseFragment
import com.webtic.qrprint.ui.components.QuantityInputForm
import com.webtic.qrprint.ui.qrcode.NavigationItem
import com.webtic.qrprint.ui.qrcode.decodeQuantity
import com.webtic.qrprint.util.AppConstants
import com.webtic.qrprint.util.AppConstants.DB_CHECKED_COLUMN_NAME
import com.webtic.qrprint.util.AppConstants.DB_NAME
import com.webtic.qrprint.util.ConnectionManager
import com.webtic.qrprint.util.PreferenceManager
import com.webtic.qrprint.util.QueryError
import com.webtic.qrprint.util.QuerySuccess
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_revenue_details.view.*
import java.sql.ResultSet
import javax.inject.Inject

@AndroidEntryPoint
class RevenueDetailsFragment : BaseFragment() {

    private lateinit var table: TableLayout
    private lateinit var subDetailsTable: TableLayout
    private lateinit var header: ViewGroup
    private val rows = mutableListOf<Pair<Cikk, View>>()
    private val subDetailsRows = mutableListOf<Pair<SubDetailsTableItem, View>>()
    private val args: RevenueDetailsFragmentArgs by navArgs()
    private lateinit var kbizszam: TextView
    private lateinit var szallVevo: TextView
    private lateinit var szallCim: TextView
    private lateinit var szallFizBrNtDn: TextView
    private var revenueDetailsItem = RevenueDetailsItem()
    private lateinit var quantityInputForm: QuantityInputForm
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_revenue_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        table = view.findViewById(R.id.revenueDetailsTable)
        subDetailsTable = view.findViewById(R.id.subDetailsTable)
        header = table.children.first() as ViewGroup
        initRowSort()

        kbizszam = view.findViewById(R.id.kbizszam)
        szallVevo = view.findViewById(R.id.szallVevo)
        szallCim = view.findViewById(R.id.szallCim)
        szallFizBrNtDn = view.findViewById(R.id.szallFizBrNtDn)

        view.backToMenuButton.setOnClickListener {
            findNavController().popBackStack(R.id.menuFragment, false)
        }
        quantityInputForm = view.findViewById(R.id.quantityInputForm)
        quantityInputForm.setSaveButtonClickListener {
            Log.e("QUANTITY", "ONSAVE")
            val quantityDetails: QuantityDetails = quantityInputForm.getQuantityDetails()
            val result = saveQuantityDetails(quantityDetails)
            if(result){
                Toast.makeText(context, "Successfully saved quantity details", Toast.LENGTH_LONG).show()
            }
            else{
                Toast.makeText(context, "Error while save quantity details", Toast.LENGTH_LONG).show()
            }

        }
    }

    override fun onStart() {
        super.onStart()
        loadData()
        updateViews()
    }

    private fun initRowSort() {
        header.children.forEach { children ->
            children.setOnClickListener { clickedView ->
                table.removeViews(1, table.childCount - 1)
                PreferenceManager.saveString( requireContext(),
                    AppConstants.REVENUE_DETAILS_SORT_KEY, when(clickedView.id) {
                        R.id.ssz -> "sorszam"
                        R.id.cikk -> "cikk"
                        R.id.kkod -> "kkod"
                        R.id.db -> "mennyiseg"
                        R.id.me -> "mennyisegiEgyseg"
                        R.id.megjegyzes -> "megjegyzes"
                        R.id.rk -> "raktarKeszlet"
                        R.id.ugyfelkod -> "ugyfelkod"
                        R.id.kkodMegoszlas -> "kkodMegoszlas"
                        else -> "sorszam"
                    })
                rows.sortedBy {
                    when (clickedView.id) {
                        R.id.ssz -> it.first.sorszam
                        R.id.cikk -> it.first.cikk
                        R.id.kkod -> it.first.kkod
                        R.id.db -> it.first.mennyiseg
                        R.id.me -> it.first.mennyisegiEgyseg
                        R.id.megjegyzes -> it.first.megjegyzes
                        R.id.rk -> it.first.raktarKeszlet
                        R.id.ugyfelkod -> it.first.ugyfelkod
                        R.id.kkodMegoszlas -> it.first.kkodMegoszlas
                        else -> it.first.sorszam
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
                        R.id.ssz -> it.first.sorszam
                        R.id.cikk -> it.first.cikk
                        R.id.kkod -> it.first.kkod
                        R.id.db -> it.first.mennyiseg
                        R.id.me -> it.first.mennyisegiEgyseg
                        R.id.megjegyzes -> it.first.megjegyzes
                        R.id.rk -> it.first.raktarKeszlet
                        R.id.ugyfelkod -> it.first.ugyfelkod
                        R.id.kkodMegoszlas -> it.first.kkodMegoszlas
                        else -> it.first.sorszam
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

    private fun loadData() {
        try {
            val mainDataSql =
                "select vc.UGYFNEV vevo, szc.UGYFNEV szallit, o.ORSZAG_BEV_KOD, c.pirkod, c.telepules, szc.tutca, kbizszam\n" +
                        " from ${DB_NAME}.dbo.KESZLETF, ${DB_NAME}.dbo.UGYFEL SZC, ${DB_NAME}.dbo.UGYFEL VC, ${DB_NAME}..TCIM c, ${DB_NAME}..ORSZAG o\n" +
                        " where SZC.UGYFELKOD=KESZLETF.ATVEVOKOD\n" +
                        " and VC.UGYFELKOD=KESZLETF.UGYFELKOD\n" +
                        " and c.CIMKOD=szc.TCIMKOD\n" +
                        " and c.ORSZAGKOD=O.ORSZAGKOD\n" +
                        " and szc.VEFLAG=1\n" +
                        " and KESZLETF.KBIKTSZAM='${args.kbiktszam}'"
            val otherDataSql =
                "select case when szmegnev1 IS null then '-' else szmegnev1 end szallmod, '-' fizmod , aa.KBRUTTOERT, aa.KNETTOERT, aa.DNEMKOD\n" +
                        "from \n" +
                        "${DB_NAME}.dbo.KESZLETF aa LEFT OUTER JOIN ${DB_NAME}.dbo.szallmod sz on sz.szallkod = aa.szallmod\n" +
                        "where aa.KBIKTSZAM='${args.kbiktszam}'"
            val itemListSql = "select aa.etk Cikk, aa.TETELSSZ, \n" +
                    " isnull(aa.GYARTAS,'-------') KKOD, \n" +
                    " aa.tetelmenny Mennyiség, \n" +
                    " MEROV1 Mennyiségi_egység, \n" +
                    " tetelssz,\n" +
                    " (select round(sum(case when mozgnem<200 then tetelmenny else tetelmenny*-1 end),2) from ${DB_NAME}.dbo.tetel t where t.etk=aa.etk and RAKTARKOD=1) as Raktár_készlet, \n" +
                    " Row_Number() Over (Order By TETELSSZ) sorsz\n" +
                    " ,aa.TETELMEGJ Megjegyzés\n" +
                    " ,aa.UGYFELKOD\n" +
                    " ,left(isnull(aa.GYARTAS,'-------'),1) MISC\n" +
                    " ,isnull(TETEL_marking.CHECKED, '0') as CHECKED\n "+
                    "from ${DB_NAME}.dbo.TETEL aa \n" +
                    " JOIN ${DB_NAME}.dbo.cikk ON cikk.etk = aa.etk \n" +
                    " LEFT JOIN ${DB_NAME}.dbo.TETEL_marking ON aa.TETELSSZ = TETEL_marking.id \n" +
                    "where cikk.etk=aa.etk \n" +
//                    "and sziktszam is null \n" +
                    "and cikk.cikkkatkod>2 \n" +
                    "and aa.KFIKTSZAM='${args.kbiktszam}'"
            var subDetailsSql = "SELECT\n" +
                    "RENDELF.RENDELSZAM, --Rendel. szám\n" +
                    "RENDELT.ETK , --Cikk\n" +
                    "gyartas, --KKOD\n" +
                    "format(RENDELT.RHATIDO,'yyyy.MM.dd') datum, --Határidő\n" +
                    "round(RENDELT.RENDMENNY - RENDELT.TRENDMENNY,3) TARTOZAS, --Tartozás\n" +
                    "round((select sum(rkeszlet.RMENNY) from rkeszlet where rkeszlet.etk = rendelt.ETK and rkeszlet.raktarkod = rendelt.RAKTAR and isnull(rendelt.gyartas,isnull(rkeszlet.gyartas,''))=isnull(rkeszlet.gyartas,'')),3) RAKTARON, --R.készl.\n" +
                    "RENDELF.HIVATKOZAS+' BELSŐ MEGJ.: '+ RENDELF.HIVATKOZAS3 MEGJ --Megjegyzés\n" +
                    "FROM\n" +
                    "RENDELT RENDELT,\n" +
                    "CIKK CIKK with (nolock) ,\n" +
                    "RENDELF RENDELF\n" +
                    "WHERE\n" +
                    "(RENDELT.ETK = CIKK.ETK)\n" +
                    "AND RENDELT.RIKTSZAM = RENDELF.RIKTSZAM\n" +
                    "AND RENDELT.RENDALL = 9\n" +
                    "AND (RENDELT.RENDMENNY-TRENDMENNY-KELLMENNY)>=0.0001 AND RENDELF.JELZO = 'V' AND RENDELT.JELZO = 'V'\n" +
                    "--and CIKKKATKOD > 2\n" +
                    "\n" +
                    "------------------------------------------------------\n" +
                    "--THIS PART HAS TO BE FILLED BY THE CURRENT MAIN TABLE PARAMETER /DO NOT KNOW IF YOU HAVE THE \"RIKTSZAM\" OR \"RENDELSZAM\" CHOSE THAT YOU HAVE/ AND NEED TO APPLY THE CUSTOMER CODE /\"ugyfelkod\"/\n" +
                    "and KESZLETF.ugyfelkod='${args.ugyfelkod}'\n" +
                    "------------------------------------------------------\n" +
                    "\n" +
                    "ORDER BY isnull(rendelt.RHATIDO,getdate()) asc,etk"
            when (val result = connectionManager.executeQuery(mainDataSql)) {
                is QuerySuccess -> fillMainData(result.resultSet)
                is QueryError -> Log.e("QueryError", "main " + result.exception.message.toString())
            }
            when (val result = connectionManager.executeQuery(otherDataSql)) {
                is QuerySuccess -> fillOtherData(result.resultSet)
                is QueryError -> Log.e("QueryError", "other " + result.exception.message.toString())
            }
            when (val result = connectionManager.executeQuery(subDetailsSql)) {
                is QuerySuccess -> fillSubDetails(result.resultSet)
                is QueryError -> Log.e("QueryError", "other " + result.exception.message.toString())
            }
            val columnName = DB_CHECKED_COLUMN_NAME
            val tableName = "TETEL"
            if(checkMarkingTable(tableName)){
                when (val result = connectionManager.executeQuery(itemListSql)) {
                    is QuerySuccess -> fillItemList(result.resultSet)
                    is QueryError -> Log.e(
                        "QueryError",
                        "itemList " + result.exception.message.toString()
                    )
                }
            }
            quantityInputForm.setQuantityDetails(getQuantityDetails(args.kbiktszam))
        } catch (e: Exception) {
            Log.e("FETCH_OR_DISPLAY_DATA", e.message.toString() + "\n" + e.stackTrace)
        }
    }


    private fun fillSubDetails(result: ResultSet){
        while(result.next()){
            val subDetailsItem = SubDetailsTableItem(
                szam = result.getString("RENDELSZAM"),
                cikk = result.getString("ETK"),
                kkod = result.getString("gyartas"),
                hatarido = result.getString("datum"),
                tartozas = result.getString("TARTOZAS"),
                keszl = result.getString("RAKTARON"),
                megjegyzes = result.getString("MEGJ")
            )
            revenueDetailsItem.subDetailsList.run {
//                Log.e("ADDSUB", "${subDetailsItem.szam}")
//                if (none { it.szam == subDetailsItem.szam }){
                Log.e("ADDSUB-02", "${subDetailsItem.szam}")
                add(subDetailsItem)
//                }

            }

        }
    }
    private fun fillMainData(result: ResultSet) {
        if (result.next()) {
            revenueDetailsItem.run {
                vevo = result.getString("vevo")
                szall = result.getString("szallit")
                orszag = result.getString("ORSZAG_BEV_KOD")
                pirkod = result.getString("pirkod")
                telepules = result.getString("telepules")
                utca = result.getString("tutca")
                kbizszam = result.getString("kbizszam")
            }
        }
    }

    private fun fillOtherData(result: ResultSet) {
        if (result.next()) {
            revenueDetailsItem.run {
                szallMod = result.getString("szallmod")
                fizMod = result.getString("fizmod")
                brutto = result.getString("KBRUTTOERT")
                netto = result.getString("KNETTOERT")
                devizanem = result.getString("DNEMKOD")
            }
        }
    }

    private fun fillItemList(result: ResultSet) {
        while (result.next()) {
            val item = Cikk(
                cikk = result.getString("Cikk"),
                kkod = result.getString("KKOD"),
                mennyiseg = result.getDouble("Mennyiség"),
                mennyisegiEgyseg = result.getString("Mennyiségi_egység"),
                megjegyzes = result.getString("Megjegyzés"),
                sorszam = result.getInt("sorsz"),
                ugyfelkod = result.getInt("UGYFELKOD"),
                misc = result.getString("MISC"),
                tetelsorszam = result.getInt("tetelssz"),
                raktarKeszlet = result.getDouble("Raktár_készlet"),
                checked = result.getBoolean(DB_CHECKED_COLUMN_NAME),
                itemNo = result.getString("TETELSSZ")
            )
            revenueDetailsItem.cikkek.run {
                if (none { it.sorszam == item.sorszam })
                    add(item)
            }
            try {
                val gyariszamSql =
                    "select gymegnev1 from ${DB_NAME}.dbo.gyujtes, ${DB_NAME}.dbo.gyujtok where tetelid =${item.tetelsorszam} and gyujtes.gykod=gyujtok.gykod"
                val kkodMegoszlasSql = "select round((SUM(TETELMENNY)/\n" +
                        "   (select SUM(TETELMENNY) from ${DB_NAME}.dbo.TETEL aa\n" +
                        "   where '${item.cikk}'=aa.etk \n" +
                        "   and UGYFELKOD=${item.ugyfelkod}\n" +
                        "   and TETTELJDAT between dateadd(year,-1,SYSDATETIME()) and SYSDATETIME()\n" +
                        "   and MOZGNEM='101'))*100,1) szazalek,\n" +
                        "  GYARTAS kkod\n" +
                        " from ${DB_NAME}.dbo.TETEL aa\n" +
                        " where '${item.cikk}'=aa.etk \n" +
                        " and UGYFELKOD=${item.ugyfelkod}\n" +
                        " and TETTELJDAT between dateadd(year,-1,SYSDATETIME()) and SYSDATETIME()\n" +
                        " and MOZGNEM='101'\n" +
                        " group by GYARTAS"
                when (val kkodResult = connectionManager.executeQuery(gyariszamSql)) {
                    is QuerySuccess -> {
                        kkodResult.resultSet.let {
                            if (it.next()) {
                                item.gyariszam = it.getString("gymegnev1")
                            }
                        }
                    }
                    is QueryError -> Log.e("QueryError", kkodResult.exception.message.toString())
                }
                when (val kkodResult = connectionManager.executeQuery(kkodMegoszlasSql)) {
                    is QuerySuccess -> {
                        kkodResult.resultSet.let {
                            if (it.next()) {
                                val kkod = it.getString("kkod")
                                val szazalek = it.getString("szazalek")
                                item.kkodMegoszlas = "$kkod - $szazalek"
                            }
                        }
                    }
                    is QueryError -> Log.e("QueryError", kkodResult.exception.message.toString())
                }
            } catch (e: Exception) {
                Log.e("FETCH_OR_DISPLAY_DATA", e.message.toString() + "\n" + e.stackTrace)
            }
        }
    }

    private fun updateViews() {
        rows.clear()
        val context = requireContext()
        kbizszam.text = revenueDetailsItem.kbizszam
        szallVevo.text = context.getString(
            R.string.tSzallVevo,
            revenueDetailsItem.szall,
            revenueDetailsItem.vevo
        )
        szallCim.text = context.getString(
            R.string.tSzallCim,
            revenueDetailsItem.orszag,
            revenueDetailsItem.pirkod,
            revenueDetailsItem.telepules,
            revenueDetailsItem.utca
        )
        szallFizBrNtDn.text = context.getString(
            R.string.tSzallFizBrNtDn,
            revenueDetailsItem.szallMod,
            revenueDetailsItem.fizMod,
            revenueDetailsItem.brutto,
            revenueDetailsItem.netto,
            revenueDetailsItem.devizanem
        )
        val listTableSortKey = PreferenceManager.getString(context,
            AppConstants.REVENUE_DETAILS_SORT_KEY
        )
        revenueDetailsItem.cikkek.sortedWith(compareBy { item ->
            when (listTableSortKey) {
                "sorszam" -> item.sorszam
                "cikk" -> item.cikk
                "kkod" -> item.kkod
                "mennyiseg" -> item.mennyiseg
                "mennyisegiEgyseg" -> item.mennyisegiEgyseg
                "megjegyzes" -> item.megjegyzes
                "raktarKeszlet" -> item.raktarKeszlet
                "ugyfelkod" -> item.ugyfelkod
                "kkodMegoszlas" -> item.kkodMegoszlas
                else -> item.sorszam
            }
        }).forEach { cikk ->
            val rowView =
                layoutInflater.inflate(R.layout.template_revenue_details_table_row, null, true)

            rows.add(cikk to rowView)
            val checkBoxRowChecked = rowView.findViewById<CheckBox>(R.id.checkBox)
            checkBoxRowChecked.visibility = VISIBLE
            checkBoxRowChecked.isChecked = cikk.checked
            checkBoxRowChecked.setOnCheckedChangeListener { buttonView, isChecked ->
                var willUpdateValue = 0
                if(isChecked) willUpdateValue = 1
                var checkRow = insertOrUpdateTable("TETEL_marking", DB_CHECKED_COLUMN_NAME, "${willUpdateValue}", "id = ${cikk.itemNo}")
            }
            rowView.findViewById<TextView>(R.id.ssz).text = cikk.sorszam.toString()
            rowView.findViewById<TextView>(R.id.cikk).run {
                text = cikk.cikk
                setOnClickListener {

                    findNavController().navigate(
                        RevenueDetailsFragmentDirections.actionRevenueDetailsFragmentToSearchFragment(
                            cikk.cikk
                        )
                    )
                }
            }
            rowView.findViewById<TextView>(R.id.kkod).run {
                text = cikk.kkod
                setOnClickListener {
                    var checkRow = insertOrUpdateTable("TETEL_marking", DB_CHECKED_COLUMN_NAME, "1", "id = ${cikk.itemNo}")
                    if(checkRow){
                        checkBoxRowChecked.isChecked = true
                        cikk.checked = true
                    }
                    findNavController().navigate(
                        RevenueDetailsFragmentDirections.actionRevenueDetailsFragmentToQrCodeFragment(
                            NavigationItem(
                                partNumber = cikk.cikk.toString(),
                                kkod = cikk.kkod.toString(),
                                storage = cikk.raktarKeszlet.toString(),
                                description = cikk.megjegyzes.toString(),
                                document = this@RevenueDetailsFragment.kbizszam.text.toString(),
                                quantity = decodeQuantity(
                                    cikk.mennyiseg.toString(),
                                    cikk.mennyisegiEgyseg.toString()
                                ),
                                notFromRevenues = false,
                                clientId = cikk.ugyfelkod
                            )
                        )
                    )
                }
            }
            rowView.findViewById<TextView>(R.id.db).text = cikk.mennyiseg.toString()
            rowView.findViewById<TextView>(R.id.me).text = cikk.mennyisegiEgyseg
            rowView.findViewById<TextView>(R.id.megjegyzes).text = cikk.megjegyzes
            rowView.findViewById<TextView>(R.id.rk).text = cikk.raktarKeszlet.toString()
            rowView.findViewById<TextView>(R.id.kkodMegoszlas).text = cikk.kkodMegoszlas
            table.addView(rowView)
        }
        revenueDetailsItem.subDetailsList.sortedBy { it.szam }.forEach {
                subDetailsTableItem ->

            Log.e("ADDROWVIEW", "${subDetailsTableItem.szam}")
            val rowView =
                layoutInflater.inflate(R.layout.template_sub_details_table_row, null, true)
            subDetailsRows.add(subDetailsTableItem to rowView)

            rowView.findViewById<TextView>(R.id.szam).text = subDetailsTableItem.szam
            rowView.findViewById<TextView>(R.id.cikk).text = subDetailsTableItem.cikk
            rowView.findViewById<TextView>(R.id.kkod).text = subDetailsTableItem.kkod
            rowView.findViewById<TextView>(R.id.hatarido).text = subDetailsTableItem.hatarido
            rowView.findViewById<TextView>(R.id.tartozas).text = subDetailsTableItem.tartozas
            rowView.findViewById<TextView>(R.id.keszl).text = subDetailsTableItem.keszl
            rowView.findViewById<TextView>(R.id.megjegyzes).text = subDetailsTableItem.megjegyzes
            subDetailsTable.addView(rowView)

        }
    }
}