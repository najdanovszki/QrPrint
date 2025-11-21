package com.webtic.qrprint.ui.revenues

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
import com.webtic.qrprint.ui.BaseFragment
import com.webtic.qrprint.util.AppConstants.DB_NAME
import com.webtic.qrprint.util.AppConstants.addLineBreak
import com.webtic.qrprint.util.ConnectionManager
import com.webtic.qrprint.util.QueryError
import com.webtic.qrprint.util.QuerySuccess
import dagger.hilt.android.AndroidEntryPoint
import java.sql.Date
import java.sql.ResultSet
import javax.inject.Inject

@AndroidEntryPoint
class RevenuesFragment : BaseFragment() {

    private lateinit var table: TableLayout
    private lateinit var header: ViewGroup
    private val rows = mutableListOf<Pair<RevenueItem, View>>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_revenues, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        table = view.findViewById(R.id.revenuesTable)
        header = table.children.first() as ViewGroup
        initRowSort()
    }

    override fun onStart() {
        super.onStart()
        try {
            when (val result = connectionManager.executeQuery(REVENUES_SQL)) {
                is QuerySuccess -> updateTable(result.resultSet)
                is QueryError -> Log.e("QueryError", "main " + result.exception.message.toString())
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
                        R.id.szlevszam -> it.first.szlevSzam
                        R.id.kelte -> it.first.kelte
                        R.id.nettoErtek -> it.first.nettoErtek
                        else -> it.first.kbiktszam
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
                        R.id.szlevszam -> it.first.szlevSzam
                        R.id.kelte -> it.first.kelte
                        R.id.nettoErtek -> it.first.nettoErtek
                        else -> it.first.kbiktszam
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
            val row = RevenueItem(
                ugyfelRovidNev = result.getString("Ügyfél_rövid_név") ?: "",
                szallitRovidNev = result.getString("Száll_rövid_név") ?: "",
                kbiktszam = result.getInt("KBIKTSZAM"),
                szlevSzam = result.getString("Szlev_szám") ?: "",
                kelte = result.getDate("Kelte") ?: Date(0L),
                nettoErtek = result.getDouble("Nettó_érték"),
                bruttoSuly = result.getDouble("Br_suly"),
                ugyfnev = result.getString("UGYFNEV") ?: "",
                UGYFELKOD = result.getString("UGYFELKOD")?: "",
            )

            val rowView =
                layoutInflater.inflate(R.layout.template_revenue_table_row, null, true)
            rows.add(Pair(row, rowView))

            rowView.findViewById<TextView>(R.id.ugyfelRovidNev).text = row.ugyfelRovidNev
            rowView.findViewById<TextView>(R.id.szallitRovidNev).text = addLineBreak(row.szallitRovidNev, 20)
            rowView.findViewById<TextView>(R.id.szlevszam).run {
                text = row.szlevSzam
                setOnClickListener {
                    findNavController().navigate(
                        RevenuesFragmentDirections.actionRevenuesFragmentToRevenueDetailsFragment(
                            row.kbiktszam.toString(),
                            row.UGYFELKOD.toString()
                        )
                    )
                }
            }
            rowView.findViewById<TextView>(R.id.kelte).text =
                if (row.kelte.time != 0L) row.kelte.toString() else ""
            rowView.findViewById<TextView>(R.id.nettoErtek).text = row.nettoErtek.toString()
            rowView.findViewById<TextView>(R.id.bruttoSuly).text = row.bruttoSuly.toString()

            table.addView(rowView)
        }
    }

    private companion object {
        var REVENUES_SQL = "select vc.UGYFNEV Ügyfél_rövid_név, szc.UGYFELKOD, \n" +
                " vc.UGYFTNEV Száll_rövid_név, \n" +
                " KBIKTSZAM, \n" +
                " KBIZSZAM Szlev_szám, \n" +
                " KBKELTE Kelte, \n" +
                " round(KNETTOERT,2) Nettó_érték,\n" +
                " round( (SELECT sum(tetelmenny*convert(float,replace(replace(jellemzo,',','.'),' ',''))) Súly \n" +
                " FROM ${DB_NAME}.dbo.jellemzok, \n" +
                "  ${DB_NAME}.dbo.tetel \n" +
                " WHERE tetel.KFIKTSZAM =KESZLETF.KBIKTSZAM \n" +
                "  and jellemzok.etk = tetel.etk \n" +
                "  and jellemzok.jelmegnev1 in ('Bruttó kg','Bruttó súly') \n" +
                "  and not jellemzok.jellemzo is null \n" +
                "  and isnumeric(jellemzo)=1),2) Br_suly,\n" +
                " szc.UGYFNEV\n" +
                "from ${DB_NAME}.dbo.KESZLETF,\n" +
                " ${DB_NAME}.dbo.UGYFEL vc,\n" +
                " ${DB_NAME}.dbo.UGYFEL szc\n" +
                "where vc.UGYFELKOD=KESZLETF.UGYFELKOD \n" +
                " and szc.UGYFELKOD=KESZLETF.atvevokod\n" +
                " and mozgnem=101 \n" +
//                " and exists(select 1 from ${DB_NAME}.dbo.tetel where kfiktszam=kbiktszam and sziktszam is null)" +
                " and KBKELTE>=dateadd(d,-10,getdate()) \n" +
                "ORDER BY KBIKTSZAM DESC"
    }
}