package com.webtic.qrprint.ui.tasks

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
import com.webtic.qrprint.util.ConnectionManager
import com.webtic.qrprint.util.QueryError
import com.webtic.qrprint.util.QuerySuccess
import dagger.hilt.android.AndroidEntryPoint
import java.sql.ResultSet
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class TasksFragment : BaseFragment() {

    private lateinit var table: TableLayout
    private lateinit var header: ViewGroup
    private val rows = mutableListOf<Pair<TaskItem, View>>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_tasks, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        table = view.findViewById(R.id.tasksTable)
        header = table.children.first() as ViewGroup
        initRowSort()
        try {
            when (val result = connectionManager.executeQuery(TASK_SQL)) {
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
                        R.id.statusz -> it.first.statusz
                        R.id.ugyfelRovidNev -> it.first.ugyfelRovidNev
                        R.id.szallRovidNev -> it.first.szallRovidNev
                        R.id.cikk -> it.first.cikk
                        R.id.kkod -> it.first.kkod
                        R.id.rendelSzam -> it.first.rendelSzam
                        R.id.hi -> it.first.hi
                        R.id.tartozas -> it.first.tartozas
                        R.id.kiadhato -> it.first.kiadhato
                        R.id.keszlet -> it.first.keszlet
                        R.id.szallMod -> it.first.szallMod
                        R.id.hivatkozas -> it.first.hivatkozas
                        else -> it.first.ugyfelRovidNev
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
                        R.id.statusz -> it.first.statusz
                        R.id.ugyfelRovidNev -> it.first.ugyfelRovidNev
                        R.id.szallRovidNev -> it.first.szallRovidNev
                        R.id.cikk -> it.first.cikk
                        R.id.kkod -> it.first.kkod
                        R.id.rendelSzam -> it.first.rendelSzam
                        R.id.hi -> it.first.hi
                        R.id.tartozas -> it.first.tartozas
                        R.id.kiadhato -> it.first.kiadhato
                        R.id.keszlet -> it.first.keszlet
                        R.id.szallMod -> it.first.szallMod
                        R.id.hivatkozas -> it.first.hivatkozas
                        else -> it.first.ugyfelRovidNev
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
            val row = TaskItem(
                statusz = result.getString("Statusz") ?: "",
                ugyfelRovidNev = result.getString("Ügyfél_rövid_név") ?: "",
                szallRovidNev = result.getString("Száll_rövid_név") ?: "",
                cikk = result.getString("Cikk") ?: "",
                kkod = result.getString("KKOD") ?: "",
                rendelSzam = result.getString("Rendel_szam") ?: "",
                hi = result.getDate("Hi") ?: Date(0L),
                tartozas = result.getDouble("Tartozas"),
                kiadhato = result.getDouble("Kiadható"),
                keszlet = result.getDouble("Keszlet"),
                szallMod = result.getString("Szall_mód") ?: "",
                hivatkozas = result.getString("Hivatkozás") ?: "",
                status = result.getInt("STATUS"),
                tetelssz = result.getInt("TETELSSZ"),
                ugyfelkod = result.getInt("ugyfelkod"),
                kkodEleje = result.getString("KKOD_eleje") ?: "",
                riktszam = result.getString("RIKTSZAM") ?: "",
            )
            val rowView = layoutInflater.inflate(R.layout.template_task_table_row, null, true)
            rows.add(Pair(row, rowView))

            when (row.status) {
                1 -> rowView.setBackgroundColor(0xFFFFFFFF.toInt())
                2 -> rowView.setBackgroundColor(0xFF66B3FF.toInt())
                4 -> rowView.setBackgroundColor(0xFFFFFFFF.toInt())
                7 -> rowView.setBackgroundColor(0xFFFFFF00.toInt())
                8 -> rowView.setBackgroundColor(0xFF00FF00.toInt())
                else -> rowView.setBackgroundColor(0xFFFF0000.toInt())
            }

            rowView.findViewById<TextView>(R.id.statusz).text = row.statusz
            rowView.findViewById<TextView>(R.id.ugyfelRovidNev).text = row.ugyfelRovidNev
            rowView.findViewById<TextView>(R.id.szallRovidNev).text = row.szallRovidNev
            rowView.findViewById<TextView>(R.id.cikk).run {
                text = row.cikk
                setOnClickListener {
                    findNavController().navigate(
                        TasksFragmentDirections.actionTasksFragmentToSearchFragment(
                            row.cikk
                        )
                    )
                }
            }
            rowView.findViewById<TextView>(R.id.kkod).text = row.kkod
            rowView.findViewById<TextView>(R.id.rendelSzam).run {
                text = row.rendelSzam
                setOnClickListener {
                    findNavController().navigate(
                        TasksFragmentDirections.actionTasksFragmentToLiveOrderDetailsFragment(
                            row.riktszam,
                            "${row.ugyfelkod}"
                        )
                    )
                }
            }
            rowView.findViewById<TextView>(R.id.hi).text =
                if (row.hi.time != 0L) row.hi.toString().split(" ").first() else ""
            rowView.findViewById<TextView>(R.id.tartozas).text = row.tartozas.toString()
            rowView.findViewById<TextView>(R.id.kiadhato).text = row.kiadhato.toString()
            rowView.findViewById<TextView>(R.id.keszlet).text = row.keszlet.toString()
            rowView.findViewById<TextView>(R.id.szallMod).text = row.szallMod
            rowView.findViewById<TextView>(R.id.hivatkozas).text = row.hivatkozas

            table.addView(rowView)
        }
    }

    private companion object {
        var TASK_SQL = "SELECT\n" +
                "case when round(RENDELT.RENDMENNY-RENDELT.TRENDMENNY,3)<= round(RENDELT.RENDMENNY-RENDELT.TRENDMENNY-RENDELT.KELLMENNY,4) then '1-Kiadható'\n" +
                "when (round(RENDELT.RENDMENNY-RENDELT.TRENDMENNY,3)> round(RENDELT.RENDMENNY-RENDELT.TRENDMENNY-RENDELT.KELLMENNY,4))\n" +
                "      and (round(RENDELT.RENDMENNY-RENDELT.TRENDMENNY-RENDELT.KELLMENNY,4)!=0) then '2-Részben'\n" +
                "when round(RENDELT.RENDMENNY-RENDELT.TRENDMENNY-RENDELT.KELLMENNY,4)=0 then '3-Nincs'\n" +
                "when (select case when (sum(rkeszlet.RMENNY) IS null) then 0 else sum(rkeszlet.RMENNY) end from ${DB_NAME}..rkeszlet where rkeszlet.etk = rendelt.ETK and rkeszlet.raktarkod = rendelt.RAKTAR) =0 then '3-Nincs'\n" +
                "else '4-Keresd a rendszergazdát!' end as Statusz, \n" +
                "  UGYFEL.UGYFNEV Ügyfél_rövid_név,\n" +
                "  ugyfel2.ugyfnev Száll_rövid_név,\n" +
                "  RENDELT.ETK Cikk,\n" +
                "  isnull(GYARTAS,'-------') KKOD,\n" +
                "  RENDELF.RENDELSZAM Rendel_szam,\n" +
                "  RENDELT.RHATIDO Hi,\n" +
                "  round(RENDELT.RENDMENNY-RENDELT.TRENDMENNY,3) Tartozas ,\n" +
                "  round(RENDELT.RENDMENNY-RENDELT.TRENDMENNY-RENDELT.KELLMENNY,4) Kiadható,\n" +
                "  (select sum(rkeszlet.RMENNY) from ${DB_NAME}..rkeszlet where rkeszlet.etk = rendelt.ETK and rkeszlet.raktarkod = rendelt.RAKTAR) Keszlet,\n" +
                "  (SZMEGNEV1) Szall_mód,\n" +
                "  RENDELF.HIVATKOZAS Hivatkozás,\n" +
                "  STATUS,\n" +
                "  RENDELT.TETELSSZ,\n" +
                "  UGYFEL.ugyfelkod,\n" +
                "  left(isnull(GYARTAS,'-------'),1) KKOD_eleje,\n" +
                "  RENDELF.RIKTSZAM\n" +
                "FROM\n" +
                "  ${DB_NAME}..RENDELT RENDELT with (nolock) ,\n" +
                "  ${DB_NAME}..RENDELF RENDELF with (nolock) ,\n" +
                "  ${DB_NAME}..UGYFEL UGYFEL,\n" +
                "  ${DB_NAME}..UGYFEL UGYFEL2,\n" +
                "  ${DB_NAME}..szallmod\n" +
                "WHERE\n" +
                "      RENDELT.RIKTSZAM = RENDELF.RIKTSZAM\n" +
                "  AND RENDELF.JELZO = 'V'\n" +
                "  AND RENDELT.JELZO = 'V'\n" +
                "  AND RENDELF.UGYFELKOD = UGYFEL.UGYFELKOD\n" +
                "  AND RENDELT.RENDALL = 9\n" +
                "  and ugyfel2.ugyfelkod=rendelf.SZUGYFKOD\n" +
                "  and szallkod=hivatkozas2\n" +
                "ORDER BY UGYFEL.UGYFNEV, statusz, RENDELT.RHATIDO desc;"
    }
}