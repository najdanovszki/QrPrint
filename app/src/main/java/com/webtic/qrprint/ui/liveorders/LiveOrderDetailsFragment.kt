package com.webtic.qrprint.ui.liveorders

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.RadioGroup
import android.widget.TableLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.children
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.webtic.qrprint.R
import com.webtic.qrprint.models.SubDetailsTableItem
import com.webtic.qrprint.ui.BaseFragment
import com.webtic.qrprint.ui.qrcode.NavigationItem
import com.webtic.qrprint.ui.qrcode.decodeQuantity
import com.webtic.qrprint.util.AppConstants.DB_CHECKED_COLUMN_NAME
import com.webtic.qrprint.util.AppConstants.DB_NAME
import com.webtic.qrprint.util.AppConstants.LIVE_ORDER_DETAILS_SORT_KEY
import com.webtic.qrprint.util.PreferenceManager
import com.webtic.qrprint.util.QueryError
import com.webtic.qrprint.util.QuerySuccess
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_live_order_details.*
import kotlinx.android.synthetic.main.fragment_live_order_details.view.*
import java.sql.ResultSet

@AndroidEntryPoint
class LiveOrderDetailsFragment : BaseFragment() {

    private lateinit var table: TableLayout
    private lateinit var subDetailsTable: TableLayout
    private lateinit var header: ViewGroup
    private val rows = mutableListOf<Pair<Cikk, View>>()
    private val subDetailsRows = mutableListOf<Pair<SubDetailsTableItem, View>>()
    private val args: LiveOrderDetailsFragmentArgs by navArgs()
    private lateinit var rendelSzam: TextView
    private lateinit var szallVevo: TextView
    private lateinit var szallCim: TextView
    private lateinit var zeta: TextView
    private lateinit var szallFizBrNtDn: TextView
    private lateinit var status: TextView
    private var statusNumber = -1
    private var liveOrderDetailItem = LiveOrderDetailItem()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_live_order_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        table = view.findViewById(R.id.liveOrderDetailsTable)
        subDetailsTable = view.findViewById(R.id.subDetailsTable)
        header = table.children.first() as ViewGroup
        initRowSort()
        rendelSzam = view.findViewById(R.id.rendelSzam)
        szallVevo = view.findViewById(R.id.szallVevo)
        szallCim = view.findViewById(R.id.szallCim)
        zeta = view.findViewById(R.id.zeta)
        szallFizBrNtDn = view.findViewById(R.id.szallFizBrNtDn)
        status = view.findViewById(R.id.status)

        view.backToMenuButton.setOnClickListener {
            findNavController().popBackStack(R.id.menuFragment, false)
        }

        registerButtons()
        Log.e("LiveOrderDetails", "OnViewCreated")
    }

    override fun onStart() {
        super.onStart()
        loadData()
        updateViews()
        Log.e("LiveOrderDetails", "OnStart")
    }

    private fun registerButtons() {
        statusRadioGroup.setOnCheckedChangeListener { _: RadioGroup, view: Int ->
            statusNumber = when (view) {
                ujRendeles.id -> 1
                elokeszitheto.id -> 2
                osszeszedesAlatt.id -> 7
                osszeszedve.id -> 8
                else -> -1
            }
        }

        statusRogzitButton.setOnClickListener {
            val sql = "update ${DB_NAME}.dbo.RENDELF \n" +
                    "\tset status=$statusNumber \n" +
                    "\twhere RIKTSZAM=${args.riktszam}\n" +
                    "\tand jelzo='V'"
            when (val result = connectionManager.executeQuery(sql)) {
                is QuerySuccess -> Toast.makeText(
                    requireContext(),
                    "Sikeresen rögzítve",
                    Toast.LENGTH_LONG
                ).show()
                is QueryError -> Log.e("QueryError", "main " + result.exception.message.toString())
            }
        }
    }

    private fun initRowSort() {
        header.children.forEach { children ->
            children.setOnClickListener { clickedView ->
                table.removeViews(1, table.childCount - 1)
                PreferenceManager.saveString( requireContext(), LIVE_ORDER_DETAILS_SORT_KEY, when(clickedView.id) {
                    R.id.ssz -> "sorszam"
                    R.id.cikk -> "cikk"
                    R.id.kkod -> "kkod"
                    R.id.db -> "mennyiseg"
                    R.id.me -> "mennyisegiEgyseg"
                    R.id.hatarido -> "hatarido"
                    R.id.megjegyzes -> "megjegyzes"
                    R.id.kiadhat -> "kiadhato"
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
                        R.id.hatarido -> it.first.hatarido
                        R.id.megjegyzes -> it.first.megjegyzes
                        R.id.kiadhat -> it.first.kiadhato
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
                        R.id.hatarido -> it.first.hatarido
                        R.id.megjegyzes -> it.first.megjegyzes
                        R.id.kiadhat -> it.first.kiadhato
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
                "select vc.UGYFNEV vevo, szc.UGYFNEV szallit, o.ORSZAG_BEV_KOD, c.pirkod, c.telepules, szc.tutca, rendelszam,s.SZEMELYNEV ZETA_s_rögzítő\n" +
                        " from ${DB_NAME}.dbo.RENDELF, ${DB_NAME}.dbo.UGYFEL SZC, ${DB_NAME}.dbo.UGYFEL VC, ${DB_NAME}..TCIM c, ${DB_NAME}..ORSZAG o, ${DB_NAME}.dbo.szemely s\n" +
                        " where SZC.UGYFELKOD=RENDELF.SZUGYFKOD\n" +
                        "  and VC.UGYFELKOD=RENDELF.UGYFELKOD\n" +
                        "  and c.CIMKOD=szc.TCIMKOD\n" +
                        "  and c.ORSZAGKOD=O.ORSZAGKOD\n" +
                        "  and szc.VEFLAG=1\n" +
                        "  and s.SZEMELYKOD= RENDELF.SZEMELYKOD\n" +
                        "  and RENDELF.riktszam=${args.riktszam}"
            val otherDataSql =
                "select szmegnev1, Fizmodnev1, aa.bertek, aa.nertek, aa.devizanem, aa.status\n" +
                        "from\n" +
                        "${DB_NAME}.dbo.RENDELF aa,\n" +
                        "${DB_NAME}.dbo.fizmod f,\n" +
                        "${DB_NAME}.dbo.szallmod sz\n" +
                        "where f.fizmodkod=aa.fmod\n" +
                        "and sz.szallkod=aa.hivatkozas2\n" +
                        "and aa.riktszam=${args.riktszam}"
            val itemListSql = "SELECT RENDELT.ETK AS Cikk, RENDELT.TETELSSZ, " +
                    "ISNULL(GYARTAS, '-------') AS KKOD, " +
                    "(CASE WHEN rendall=9 OR rendall=10 THEN rendelt.rendmenny-trendmenny ELSE NULL END)/ISNULL(1,1) AS Mennyiség, " +
                    "CIKK.MEROV1 AS Mennyiségi_egység, " +
                    "RHATIDO AS Határidő, " +
                    "(CASE WHEN rendelt.rendmenny-trendmenny-kellmenny > 0 AND rendall=9 AND RENDELT.jelzo='V' THEN rendelt.rendmenny-trendmenny-kellmenny ELSE NULL END)/ISNULL(1,1) AS Kiadható, " +
                    "(SELECT ROUND(SUM(CASE WHEN mozgnem<200 THEN tetelmenny ELSE tetelmenny*-1 END), 2) FROM ${DB_NAME}.dbo.tetel t WHERE t.etk=RENDELT.etk AND RAKTARKOD=1) AS Raktár_készlet, " +
                    "Row_Number() OVER (ORDER BY RENDELT.TETELSSZ) AS sorsz, " +
                    "RENDELT.rendmemo AS Megjegyzés, " +
                    "RENDELf.ugyfelkod, " +
                    "LEFT(ISNULL(GYARTAS, '-------'), 1) AS MISC " +
                    " ,isnull(RENDELT_marking.CHECKED, '0') as CHECKED "+
                    "FROM ${DB_NAME}.dbo.RENDELT RENDELT " +
                    "JOIN ${DB_NAME}.dbo.CIKK CIKK ON RENDELT.ETK = CIKK.ETK " +
                    "JOIN ${DB_NAME}.dbo.BIZALL ON RENDELT.RENDALL = BIZALLKOD " +
                    "JOIN ${DB_NAME}.dbo.rendelf rendelf ON RENDELT.RIKTSZAM = RENDELf.RIKTSZAM " +
                    "LEFT JOIN ${DB_NAME}.dbo.RENDELT_marking ON RENDELT.TETELSSZ = RENDELT_marking.id " +
                    "WHERE RENDELT.RIKTSZAM = ${args.riktszam} " +
                    "AND BIZALLNEV1 NOT IN ('Lezárt', 'Ajánlat', 'Lezárva', 'Teljesült') " +
                    "ORDER BY sorsz"



            when (val result = connectionManager.executeQuery(mainDataSql)) {
                is QuerySuccess -> fillMainData(result.resultSet)
                is QueryError -> Log.e("QueryError", "main " + result.exception.message.toString())
            }
            when (val result = connectionManager.executeQuery(otherDataSql)) {
                is QuerySuccess -> fillOtherData(result.resultSet)
                is QueryError -> Log.e("QueryError", "other " + result.exception.message.toString())
            }



            val columnName = DB_CHECKED_COLUMN_NAME
            val tableName = "RENDELT"
            if(checkMarkingTable(tableName)){
                when (val result = connectionManager.executeQuery(itemListSql)) {
                    is QuerySuccess -> fillItemList(result.resultSet)
                    is QueryError -> Log.e(
                        "QueryError",
                        "itemList " + result.exception.message.toString()
                    )
                }
            }
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
                    "AND RENDELT.RIKTSZAM <> ${args.riktszam}\n" +
                    "and RENDELF.RENDELSZAM <>'${liveOrderDetailItem.rendelszam}'\n" +
                    "and RENDELF.ugyfelkod='${args.ugyfelkod}'\n" +
                    "------------------------------------------------------\n" +
                    "\n" +
                    "ORDER BY isnull(rendelt.RHATIDO,getdate()) asc,etk"

            Log.e("SUBSQL", subDetailsSql)
            when (val result = connectionManager.executeQuery(subDetailsSql)) {
                is QuerySuccess -> fillSubDetails(result.resultSet)
                is QueryError -> Log.e("QueryError", "other " + result.exception.message.toString())
            }

        } catch (e: Exception) {
            Log.e("FETCH_OR_DISPLAY_DATA", e.message.toString() + "\n" + e.stackTrace)
        }
    }

    private fun fillMainData(result: ResultSet) {
        if (result.next()) {
            liveOrderDetailItem.run {
                vevoUgyfelnev = result.getString("vevo")
                szallUgyfelnev = result.getString("szallit")
                orszagKod = result.getString("ORSZAG_BEV_KOD")
                pirkod = result.getString("pirkod")
                telepules = result.getString("telepules")
                utca = result.getString("tutca")
                rendelszam = result.getString("rendelszam")
                zeta = result.getString("ZETA_s_rögzítő")
            }
        }
    }

    private fun fillOtherData(result: ResultSet) {
        if (result.next()) {
            liveOrderDetailItem.run {
                szallMod = result.getString("szmegnev1")
                fizMod = result.getString("Fizmodnev1")
                brutto = result.getString("bertek")
                netto = result.getString("nertek")
                devizanem = result.getString("devizanem")
                status = result.getString("status")
            }
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
            liveOrderDetailItem.subDetailsList.run {
//                Log.e("ADDSUB", "${subDetailsItem.szam}")
//                if (none { it.szam == subDetailsItem.szam }){
                    Log.e("ADDSUB-02", "${subDetailsItem.szam}")
                    add(subDetailsItem)
//                }

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
                raktarKeszlet = result.getString("Raktár_készlet"),
                sorszam = result.getInt("sorsz"),
                ugyfelkod = result.getInt("ugyfelkod"),
                misc = result.getString("MISC"),
                kiadhato = result.getString("Kiadható"),
                hatarido = result.getDate("Határidő"),
                checked = result.getBoolean(DB_CHECKED_COLUMN_NAME),
                itemNo = result.getString("TETELSSZ")

            )
            liveOrderDetailItem.cikkek.run {
                if (none { it.sorszam == item.sorszam })
                    add(item)
            }
            try {
                val kkodMegoszlasSql = "select round((SUM(TETELMENNY)/\n" +
                        "  (select SUM(TETELMENNY) from ${DB_NAME}.dbo.TETEL aa\n" +
                        "  where '${item.cikk}'=aa.etk\n" +
                        "  and UGYFELKOD=${item.ugyfelkod}\n" +
                        "  and TETTELJDAT between dateadd(year,-1,SYSDATETIME()) and SYSDATETIME()\n" +
                        "  and MOZGNEM='201'))*100,1) szazalek,\n" +
                        " isnull(GYARTAS,'-------') kkod\n" +
                        "from ${DB_NAME}.dbo.TETEL aa\n" +
                        "where '${item.cikk}'=aa.etk\n" +
                        " and UGYFELKOD= ${item.ugyfelkod}\n" +
                        " and TETTELJDAT between dateadd(year,-1,SYSDATETIME()) and SYSDATETIME()\n" +
                        " and MOZGNEM='201'\n" +
                        "group by isnull(GYARTAS,'-------')"
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
        rendelSzam.text = liveOrderDetailItem.rendelszam
        szallVevo.text = context.getString(
            R.string.tSzallVevo,
            liveOrderDetailItem.szallUgyfelnev,
            liveOrderDetailItem.vevoUgyfelnev
        )
        szallCim.text = context.getString(
            R.string.tSzallCim,
            liveOrderDetailItem.orszagKod,
            liveOrderDetailItem.pirkod,
            liveOrderDetailItem.telepules,
            liveOrderDetailItem.utca
        )
        zeta.text = context.getString(
            R.string.tZeta,
            liveOrderDetailItem.zeta
        )
        szallFizBrNtDn.text = context.getString(
            R.string.tSzallFizBrNtDn,
            liveOrderDetailItem.szallMod,
            liveOrderDetailItem.fizMod,
            liveOrderDetailItem.brutto,
            liveOrderDetailItem.netto,
            liveOrderDetailItem.devizanem
        )
        status.text = context.getString(R.string.tStatus, liveOrderDetailItem.status)
        val liveOrderDetailsSortKey = PreferenceManager.getString(context, LIVE_ORDER_DETAILS_SORT_KEY)
        liveOrderDetailItem.cikkek.sortedWith(compareBy { item ->
            when (liveOrderDetailsSortKey) {
                "sorszam" -> item.sorszam
                "cikk" -> item.cikk
                "kkod" -> item.kkod
                "mennyiseg" -> item.mennyiseg
                "mennyisegiEgyseg" -> item.mennyisegiEgyseg
                "hatarido" -> item.hatarido
                "kiadhato" -> item.kiadhato
                "raktarKeszlet" -> item.raktarKeszlet
                "megjegyzes" -> item.megjegyzes
                "ugyfelkod" -> item.ugyfelkod
                "misc" -> item.misc
                "kkodMegoszlas" -> item.kkodMegoszlas
                else -> item.sorszam
            }
        }).forEach { cikk ->
            val rowView =
                layoutInflater.inflate(R.layout.template_live_order_details_table_row, null, true)

            rows.add(cikk to rowView)

            rowView.findViewById<TextView>(R.id.ssz).text = cikk.sorszam.toString()
            val checkBoxRowChecked = rowView.findViewById<CheckBox>(R.id.checkBox)
            checkBoxRowChecked.visibility = VISIBLE
            checkBoxRowChecked.isChecked = cikk.checked
            checkBoxRowChecked.setOnCheckedChangeListener { buttonView, isChecked ->
                var willUpdateValue = 0
                if(isChecked) willUpdateValue = 1
                var checkRow = insertOrUpdateTable("RENDELT_marking", DB_CHECKED_COLUMN_NAME, "${willUpdateValue}", "id = ${cikk.itemNo}")
            }
            rowView.findViewById<TextView>(R.id.cikk).run {
                text = cikk.cikk
                setOnClickListener {
                    findNavController().navigate(
                        LiveOrderDetailsFragmentDirections.actionLiveOrderDetailsFragmentToSearchFragment(
                            cikk.cikk
                        )
                    )
                }
            }
            rowView.findViewById<TextView>(R.id.kkod).run {
                text = cikk.kkod
                setOnClickListener {

                    var checkRow = insertOrUpdateTable("RENDELT_marking", DB_CHECKED_COLUMN_NAME, "1", "id = ${cikk.itemNo}")
                    if(checkRow){
                        checkBoxRowChecked.isChecked = true
                        cikk.checked = true
                    }
                    findNavController().navigate(
                        LiveOrderDetailsFragmentDirections.actionLiveOrderDetailsFragmentToQrCodeFragment(
                            NavigationItem(
                                partNumber = cikk.cikk.toString(),
                                kkod = cikk.kkod.toString(),
                                storage = cikk.raktarKeszlet.toString(),
                                description = cikk.megjegyzes.toString(),
                                document = this@LiveOrderDetailsFragment.rendelSzam.text.toString(),
                                quantity = decodeQuantity(
                                    cikk.mennyiseg.toString(),
                                    cikk.mennyisegiEgyseg.toString()
                                ),
                                notFromRevenues = true,
                                clientId = cikk.ugyfelkod
                            )
                        )
                    )
                }
            }
            rowView.findViewById<TextView>(R.id.db).text = cikk.mennyiseg.toString()
            rowView.findViewById<TextView>(R.id.me).text = cikk.mennyisegiEgyseg
            rowView.findViewById<TextView>(R.id.megjegyzes).text = cikk.megjegyzes
            rowView.findViewById<TextView>(R.id.rk).text = cikk.raktarKeszlet
            rowView.findViewById<TextView>(R.id.kkodMegoszlas).text = cikk.kkodMegoszlas
            rowView.findViewById<TextView>(R.id.kiadhat).text = cikk.kiadhato
            rowView.findViewById<TextView>(R.id.hatarido).text = cikk.hatarido.toString()

            table.addView(rowView)
        }
        liveOrderDetailItem.subDetailsList.sortedBy { it.szam }.forEach {
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