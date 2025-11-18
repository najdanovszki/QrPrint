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
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.webtic.qrprint.R
import com.webtic.qrprint.ui.BaseFragment
import com.webtic.qrprint.ui.qrcode.NavigationItem
import com.webtic.qrprint.ui.qrcode.decodeQuantity
import com.webtic.qrprint.util.AppConstants.DB_NAME
import com.webtic.qrprint.util.ConnectionManager
import com.webtic.qrprint.util.QueryError
import com.webtic.qrprint.util.QuerySuccess
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_bill_details.view.*
import java.sql.ResultSet
import javax.inject.Inject

@AndroidEntryPoint
class BillDetailsFragment : BaseFragment() {

    private lateinit var table: TableLayout
    private lateinit var header: ViewGroup
    private val rows = mutableListOf<Pair<Cikk, View>>()
    private val args: BillDetailsFragmentArgs by navArgs()
    private lateinit var szamla: TextView
    private lateinit var szallVevo: TextView
    private lateinit var szallCim: TextView
    private lateinit var zeta: TextView
    private lateinit var szallFizBrNtDn: TextView
    private lateinit var megjegyzes: TextView
    private var billDetailItem = BillDetailItem()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_bill_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        table = view.findViewById(R.id.billDetailsTable)
        header = table.children.first() as ViewGroup
        initRowSort()

        szamla = view.findViewById(R.id.szamla)
        szallVevo = view.findViewById(R.id.szallVevo)
        szallCim = view.findViewById(R.id.szallCim)
        zeta = view.findViewById(R.id.zeta)
        szallFizBrNtDn = view.findViewById(R.id.szallFizBrNtDn)
        megjegyzes = view.findViewById(R.id.megjegyzesSzamla)

        view.backToMenuButton.setOnClickListener {
            findNavController().popBackStack(R.id.menuFragment, false)
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
                rows.sortedBy {
                    when (clickedView.id) {
                        R.id.ssz -> it.first.sorszam
                        R.id.cikk -> it.first.cikk
                        R.id.kkod -> it.first.kkod
                        R.id.db -> it.first.mennyiseg
                        R.id.me -> it.first.mennyisegiEgyseg
                        R.id.megjegyzes -> it.first.megjegyzes
                        R.id.rk -> it.first.raktarKeszlet
                        R.id.kkodMegoszlas -> it.first.kkodMegoszlas
                        else -> it.first.sorszam
                    } as Comparable<Any>
                }.forEach { pair ->
                    /*
                        The view has been removed from the parent view a few rows before
                        However it would crash if one of the removals is deleted
                        So both of them are welcome to stay
                        This is a recurring phenomena in each simple and details fragment,
                        where the rows are sorted
                     */
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
            val mainDataSql = "select vc.UGYFNEV Vevo, \n" +
                    " szc.UGYFNEV Szallito, \n" +
                    " o.ORSZAG_BEV_KOD Orszag, \n" +
                    " c.pirkod Pirkod, \n" +
                    " c.telepules Telepules, \n" +
                    " szc.tutca Utca, \n" +
                    " szlaszam,\n" +
                    " s.SZEMELYNEV ZETA_s_rögzítő \n" +
                    " from ${DB_NAME}.dbo.SZAMLA, ${DB_NAME}.dbo.UGYFEL SZC, ${DB_NAME}.dbo.UGYFEL VC, ${DB_NAME}..TCIM c, ${DB_NAME}..ORSZAG o,${DB_NAME}.dbo.szemely s\n" +
                    " where SZC.UGYFELKOD=SZAMLA.ATVEVOKOD \n" +
                    "  and VC.UGYFELKOD=SZAMLA.UGYFELKOD\n" +
                    "  and c.CIMKOD=szc.TCIMKOD\n" +
                    "  and c.ORSZAGKOD=O.ORSZAGKOD\n" +
                    "  and szc.VEFLAG=1\n" +
                    "  and s.SZEMELYKOD=UGYINTEZO\n" +
                    "  and SZAMLA.sziktszam=${args.sziktszam}"

            val otherDataSql =
                "select szmegnev1, Fizmodnev1, aa.szbruttert, aa.sznettoert, aa.devizanem \n" +
                        "from \n" +
                        "\t${DB_NAME}.dbo.szamla aa, \n" +
                        "\t${DB_NAME}.dbo.fizmod f,\n" +
                        "\t${DB_NAME}.dbo.szallmod sz\n" +
                        "where f.fizmodkod=aa.fizmod\n" +
                        "\tand sz.szallkod=aa.szallmod \n" +
                        "\tand aa.sziktszam=${args.sziktszam}"

            val itemListSql = "select aa.etk Cikk, \n" +
                    " isnull(aa.GYARTAS,'-------') KKOD, \n" +
                    " aa.tetelmenny Mennyiség, \n" +
                    " MEROV1 Mennyiségi_egység,\n" +
                    " '' gymegnev1,  \n" +
                    " (select round(sum(case when mozgnem<200 then tetelmenny else tetelmenny*-1 end),2) from ${DB_NAME}.dbo.tetel t where t.etk=aa.etk and RAKTARKOD=1) as Raktár_készlet,\n" +
                    " Row_Number() Over (Order By TETELSSZ) Sorszám,\n" +
                    " aa.tetelmegj Megjegyzés,\n" +
                    " aa.UGYFELKOD,\n" +
                    " left(isnull(aa.GYARTAS,'-------'),1) MISC\n" +
                    "from ${DB_NAME}.dbo.TETEL aa, \n" +
                    " ${DB_NAME}.dbo.cikk \n" +
                    "where cikk.etk=aa.etk\n" +
                    " and aa.etk!='000'\n" +
                    " and aa.sziktszam=${args.sziktszam}\n" +
                    "ORDER BY Sorszám"

            val megjegyzesSql = "select szmegjegyz \n" +
                    "from \n" +
                    "${DB_NAME}.dbo.szamla aa\n" +
                    "where aa.sziktszam=${args.sziktszam}"

            when (val result = connectionManager.executeQuery(mainDataSql)) {
                is QuerySuccess -> fillMainData(result.resultSet)
                is QueryError -> Log.e("QueryError", "main " + result.exception.message.toString())
            }
            when (val result = connectionManager.executeQuery(otherDataSql)) {
                is QuerySuccess -> fillOtherData(result.resultSet)
                is QueryError -> Log.e("QueryError", "other " + result.exception.message.toString())
            }
            when (val result = connectionManager.executeQuery(itemListSql)) {
                is QuerySuccess -> fillItemList(result.resultSet)
                is QueryError -> Log.e(
                    "QueryError",
                    "itemList " + result.exception.message.toString()
                )
            }
            when (val result = connectionManager.executeQuery(megjegyzesSql)) {
                is QuerySuccess ->
                    if (result.resultSet.next())
                        billDetailItem.megjegyzes = result.resultSet.getString("szmegjegyz")
                is QueryError -> Log.e("QueryError", "desc " + result.exception.message.toString())
            }
        } catch (e: Exception) {
            Log.e("FETCH_OR_DISPLAY_DATA", e.message.toString() + "\n" + e.stackTrace)
        }
    }

    private fun fillMainData(result: ResultSet) {
        if (result.next())
            billDetailItem.run {
                vevoUgyfelnev = result.getString("Vevo")
                szallUgyfelnev = result.getString("Szallito")
                orszagKod = result.getString("Orszag")
                pirkod = result.getString("Pirkod")
                telepules = result.getString("Telepules")
                utca = result.getString("Utca")
                szamlaszam = result.getString("szlaszam")
                zeta = result.getString("ZETA_s_rögzítő")
            }
    }

    private fun fillOtherData(result: ResultSet) {
        if (result.next())
            billDetailItem.run {
                szallMod = result.getString("szmegnev1")
                fizMod = result.getString("Fizmodnev1")
                brutto = result.getString("szbruttert")
                netto = result.getString("sznettoert")
                devizanem = result.getString("devizanem")
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
                sorszam = result.getInt("Sorszám"),
                ugyfelkod = result.getInt("UGYFELKOD"),
                misc = result.getString("MISC"),
            )
            billDetailItem.cikkek.run {
                if (none { it.sorszam == item.sorszam })
                    add(item)
            }
            try {
                val kkodMegoszlasSql = "select round((SUM(TETELMENNY)/\n" +
                        "(select SUM(TETELMENNY) from ${DB_NAME}.dbo.TETEL aa\n" +
                        "where '${item.cikk}'=aa.etk \n" +
                        "and UGYFELKOD=${item.ugyfelkod}\n" +
                        "and TETTELJDAT between dateadd(year,-1,SYSDATETIME()) and SYSDATETIME()\n" +
                        "and MOZGNEM='201'))*100,1) szazalek,\n" +
                        "isnull(GYARTAS,'-------') kkod\n" +
                        "from ${DB_NAME}.dbo.TETEL aa\n" +
                        "where '${item.cikk}'=aa.etk \n" +
                        "and UGYFELKOD=${item.ugyfelkod}\n" +
                        "and TETTELJDAT between dateadd(year,-1,SYSDATETIME()) and SYSDATETIME()\n" +
                        "and MOZGNEM='201'\n" +
                        "group by isnull(GYARTAS,'-------')"
                when (val kkodResult = connectionManager.executeQuery(kkodMegoszlasSql)) {
                    is QuerySuccess -> {
                        kkodResult.resultSet.let {
                            if (it.next()) {
                                val kkod = it.getString("kkod")
                                val szazalek = it.getString("szazalek")
                                item.kkodMegoszlas = "$kkod - $szazalek%"
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
        val context = requireContext()
        szamla.text = billDetailItem.szamlaszam
        szallVevo.text = context.getString(
            R.string.tSzallVevo,
            billDetailItem.szallUgyfelnev,
            billDetailItem.vevoUgyfelnev
        )
        szallCim.text = context.getString(
            R.string.tSzallCim,
            billDetailItem.orszagKod,
            billDetailItem.pirkod,
            billDetailItem.telepules,
            billDetailItem.utca
        )
        zeta.text = context.getString(
            R.string.tZeta,
            billDetailItem.zeta
        )
        szallFizBrNtDn.text = context.getString(
            R.string.tSzallFizBrNtDn,
            billDetailItem.szallMod,
            billDetailItem.fizMod,
            billDetailItem.brutto,
            billDetailItem.netto,
            billDetailItem.devizanem
        )
        megjegyzes.text = context.getString(
            R.string.tMegjegyzes,
            billDetailItem.megjegyzes
        )
        billDetailItem.cikkek.sortedBy { it.sorszam }.forEach { cikk ->
            val rowView =
                layoutInflater.inflate(R.layout.template_bill_details_table_row, null, true)

            rows.add(cikk to rowView)
            rowView.findViewById<TextView>(R.id.ssz).text = cikk.sorszam.toString()
            rowView.findViewById<TextView>(R.id.cikk).run {
                text = cikk.cikk
                setOnClickListener {
                    findNavController().navigate(
                        BillDetailsFragmentDirections.actionBillDetailsToSearchFragment(
                            cikk.cikk
                        )
                    )
                }
            }
            rowView.findViewById<TextView>(R.id.kkod).run {
                text = cikk.kkod
                setOnClickListener {
                    val quantity = decodeQuantity(
                        cikk.mennyiseg.toString(),
                        cikk.mennyisegiEgyseg.toString()
                    )
                    findNavController().navigate(
                        BillDetailsFragmentDirections.actionBillDetailsToQrCodeFragment(
                            NavigationItem(
                                partNumber = cikk.cikk.toString(),
                                kkod = cikk.kkod.toString(),
                                storage = cikk.raktarKeszlet.toString(),
                                description = cikk.megjegyzes.toString(),
                                document = this@BillDetailsFragment.szamla.text.toString(),
                                quantity = quantity,
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
            rowView.findViewById<TextView>(R.id.kiadhat).text = ""
            rowView.findViewById<TextView>(R.id.hiSn).text = ""

            table.addView(rowView)
        }
    }
}