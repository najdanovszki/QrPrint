package com.webtic.qrprint.ui.liveorders

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
import com.webtic.qrprint.util.AppConstants
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
class LiveOrdersFragment : BaseFragment() {

    private lateinit var table: TableLayout
    private lateinit var header: ViewGroup
    private val rows = mutableListOf<Pair<LiveOrderItem, View>>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_live_orders, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        table = view.findViewById(R.id.liveOrdersTable)
        header = table.children.first() as ViewGroup
        initRowSort()
    }

    override fun onStart() {
        super.onStart()
        try {
            when (val result = connectionManager.executeQuery(LIVE_ORDERS_SQL)) {
                is QuerySuccess -> processData(result.resultSet)
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
                        R.id.rendelSzam -> it.first.rendSzam
                        R.id.nettoErtek -> it.first.nettoErtek
                        R.id.bruttoSuly -> it.first.bruttoSuly
                        R.id.hivatkozas -> it.first.hivatkozas
                        R.id.hatarido -> it.first.hatarido
                        // TODO lassú
                        // R.id.teljKkodAzonos -> it.first.teljAzonos
                        // R.id.teljKkodMas -> it.first.teljMas
                        else -> it.first.riktszam
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
                        R.id.rendelSzam -> it.first.rendSzam
                        R.id.nettoErtek -> it.first.nettoErtek
                        R.id.bruttoSuly -> it.first.bruttoSuly
                        R.id.hivatkozas -> it.first.hivatkozas
                        R.id.hatarido -> it.first.hatarido
                        // TODO lassú
//                        R.id.teljKkodAzonos -> it.first.teljAzonos
//                        R.id.teljKkodMas -> it.first.teljMas
                        else -> it.first.riktszam
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

    private fun processData(result: ResultSet) {
        val tempRows = mutableListOf<LiveOrderItem>()
        while (result.next()) {
            val row = LiveOrderItem(
                ugyfelRovidNev = result.getString("Ügyfél_rövid_név") ?: "",
                szallitRovidNev = result.getString("Szállít_rövid_név") ?: "",
                riktszam = result.getInt("RIKTSZAM"),
                rendSzam = result.getString("Rend_szám") ?: "",
                rendelesDatuma = result.getDate("Rendelés_dátuma") ?: Date(0L),
                nettoErtek = result.getDouble("Nettó_érték"),
                bruttoSuly = result.getDouble("Bruttó_súly"),
                szallitasiCimNeve = result.getString("Szállítási_cím_neve") ?: "",
                hivatkozas = result.getString("HIVATKOZAS") ?: "",
                hatarido = result.getDate("Határidő") ?: Date(0L),
                status = result.getInt("STATUS"),
                UGYFELKOD = result.getString("UGYFELKOD")
            )
            tempRows.add(row)
        }
        // TODO lassú
        /*tempRows.forEach { liveOrderItem ->
            // lassú
            val query = "select\n" +
                    " cast(\n" +
                    "   case when ((sum(KKOD_TEJESITHETO)/sorok)*100)>100 then 100.0 else (sum(KKOD_TEJESITHETO)/sorok)*100 end\n" +
                    "  as decimal(5,1)) TELLJ_KKOD_AZONOS,\n" +
                    " cast(\n" +
                    "   case when ((sum(case when mas_kkod>0 then 1.0 else 0.0 end)/sorok)*100)>100 then 100.0 else (sum(case when mas_kkod>0 then 1.0 else 0.0 end)/sorok)*100 end\n" +
                    "  as decimal(5,1)) TELLJ_KKOD_MAS\n" +
                    "from \n" +
                    "(\n" +
                    " select etk,\n" +
                    "  SUM(KKOD_TEJESITHETO) KKOD_TEJESITHETO,\n" +
                    "  SUM(MAS_KKOD_TEJESITHETO) MAS_KKOD_TEJESITHETO,\n" +
                    "  SUM(MAS_KKOD_TEJESITHETO)/COUNT(*) mas_kkod,\n" +
                    "  COUNT(*) cikkbol_hany_sor_van,\n" +
                    "  COUNT(*) over () sorok\n" +
                    " from \n" +
                    " (\n" +
                    "\n" +
                    " select a.ETK,\n" +
                    "  case when CIKKKATKOD<>1 then\n" +
                    "   case when isnull(a.GYARTAS,'-------')=isnull(b.GYARTAS,'-------') then \n" +
                    "    case when a.TELESITENDO<=kkod_db then 1.0\n" +
                    "    else 0.0 end\n" +
                    "   else 0.0 end \n" +
                    "  else 1.0 end KKOD_TEJESITHETO,\n" +
                    "  case when CIKKKATKOD<>1 then      \n" +
                    "   case when isnull(a.GYARTAS,'-------')=isnull(b.GYARTAS,'-------') then \n" +
                    "    case when a.TELESITENDO<=kkod_db then 1.0\n" +
                    "    else 0.0 end\n" +
                    "   else\n" +
                    "    case when a.TELESITENDO<=kkod_db then 1.0\n" +
                    "    else 0.0 end\n" +
                    "   end \n" +
                    "  else 1.0 end MAS_KKOD_TEJESITHETO\n" +
                    " from\n" +
                    "  (\n" +
                    "   SELECT RENDELT.ETK, \n" +
                    "     GYARTAS, \n" +
                    "     (case when rendall=9 or rendall=10 then rendelt.rendmenny-trendmenny else null end)/isnull(1,1) TELESITENDO,\n" +
                    "     cikkkatkod\n" +
                    "   FROM ${DB_NAME}.dbo.RENDELT RENDELT , \n" +
                    "    ${DB_NAME}.dbo.BIZALL,\n" +
                    "    ${DB_NAME}.dbo.cikk       \n" +
                    "   WHERE RENDELT.RIKTSZAM =${liveOrderItem.riktszam}\n" +
                    "    and RENDALL=BIZALLKOD \n" +
                    "    and BIZALLNEV1 not in ('Lezárt','Ajánlat','Lezárva','Teljesült')--ORDER BY TETELSSZ\n" +
                    "    and cikk.etk=RENDELT.ETK\n" +
                    "  ) a \n" +
                    "  LEFT OUTER JOIN\n" +
                    "  (\n" +
                    "   select etk,\n" +
                    "    gyartas,\n" +
                    "    SUM( db) kkod_db\n" +
                    "   from     \n" +
                    "    (select etk,gyartas, round(sum(case when mozgnem<200 then tetelmenny else tetelmenny*-1 end),2)  db\n" +
                    "    from ${DB_NAME}.dbo.tetel kt\n" +
                    "    where RAKTARKOD=1\n" +
                    "    group by kt.etk,gyartas) a\n" +
                    "   where db<>0 group by etk, gyartas\n" +
                    "  ) b \n" +
                    "  on a.ETK=b.etk\n" +
                    " ) x\n" +
                    " group by etk\n" +
                    ") y \n" +
                    "group by sorok " +
                    "order by "
            when (val completionResult = connectionManager.executeQuery(query)) {
                is QuerySuccess -> if (completionResult.resultSet.next()) {
                    liveOrderItem.teljAzonos =
                        completionResult.resultSet.getString("TELLJ_KKOD_AZONOS") + "%"
                    liveOrderItem.teljMas =
                        completionResult.resultSet.getString("TELLJ_KKOD_MAS") + "%"
                }
            }
        }*/
        updateTable(tempRows)
    }

    private fun updateTable(list: List<LiveOrderItem>) {
        rows.clear()
        list.forEach { liveOrderItem ->
            val rowView = layoutInflater.inflate(R.layout.template_live_order_table_row, null, true)
            rows.add(Pair(liveOrderItem, rowView))

            rowView.setBackgroundColor(
                when (liveOrderItem.status) {
                    1 -> 0xFFFFFFFF.toInt()
                    2 -> 0xFF66B3FF.toInt()
                    4 -> 0xFFFFFFFF.toInt()
                    7 -> 0xFFFFFF00.toInt()
                    8 -> 0xFF00FF00.toInt()
                    else -> 0xFFFF0000.toInt()
                }
            )

            rowView.findViewById<TextView>(R.id.ugyfelRovidNev).text = liveOrderItem.ugyfelRovidNev
            rowView.findViewById<TextView>(R.id.szallitRovidNev).text =
                addLineBreak(liveOrderItem.szallitRovidNev, 20)
            rowView.findViewById<TextView>(R.id.rendelSzam).run {
                text = liveOrderItem.rendSzam
                setOnClickListener {
                    findNavController().navigate(
                        LiveOrdersFragmentDirections.actionLiveOrdersFragmentToLiveOrderDetailsFragment(
                            liveOrderItem.riktszam.toString(),
                            liveOrderItem.UGYFELKOD.toString()
                        )
                    )
                }
            }
            rowView.findViewById<TextView>(R.id.nettoErtek).text =
                liveOrderItem.nettoErtek.toString()
            rowView.findViewById<TextView>(R.id.bruttoSuly).text =
                liveOrderItem.bruttoSuly.toString()
            rowView.findViewById<TextView>(R.id.hivatkozas).text = AppConstants.addLineBreak(
                liveOrderItem.hivatkozas,
                16
            )
            rowView.findViewById<TextView>(R.id.hatarido).text =
                if (liveOrderItem.hatarido.time != 0L) liveOrderItem.hatarido.toString()
                    .split(" ").first() else ""
            // TODO lassú
            //rowView.findViewById<TextView>(R.id.teljKkodAzonos).text = liveOrderItem.teljAzonos
            //rowView.findViewById<TextView>(R.id.teljKkodMas).text = liveOrderItem.teljMas

            table.addView(rowView)
        }
    }

    private companion object {
        var LIVE_ORDERS_SQL = "select  vc.UGYFNEV Ügyfél_rövid_név,vc.UGYFELKOD, \n" +
                "         vc.UGYFTNEV Szállít_rövid_név, \n" +
                " RENDELF.RIKTSZAM, \n" +
                " RENDELSZAM Rend_szám, \n" +
                " RENDDATUM Rendelés_dátuma, \n" +
                " RENDELF.NERTEK Nettó_érték,\n" +
                " ( SELECT sum((RENDMENNY-trendmenny)*convert(float,replace(replace(jellemzo,',','.'),' ','')))  suly FROM ${DB_NAME}.dbo.jellemzok, ${DB_NAME}.dbo.RENDELT \n" +
                "           WHERE RENDELT.RIKTSZAM =RENDELF.RIKTSZAM and jellemzok.etk = RENDELT.etk and jellemzok.jelmegnev1 in ('Bruttó kg','Bruttó súly') \n" +
                "     and not jellemzok.jellemzo is null and isnumeric(jellemzo)=1) Bruttó_súly,\n" +
                " szc.UGYFNEV Szállítási_cím_neve,\n" +
                " HIVATKOZAS,\n" +
                " min(RHATIDO) Határidő,\n" +
                " STATUS\n" +
                "  from ${DB_NAME}.dbo.RENDELF,\n" +
                "       ${DB_NAME}.dbo.UGYFEL vc,\n" +
                "       ${DB_NAME}.dbo.UGYFEL szc,\n" +
                "       ${DB_NAME}.dbo.RENDELT\n" +
                "  where vc.UGYFELKOD=RENDELF.UGYFELKOD\n" +
                "  and szc.UGYFELKOD=RENDELF.SZUGYFKOD\n" +
                "  and RENDELF.RIKTSZAM in (select distinct RIKTSZAM from ${DB_NAME}.dbo.RENDELT, ${DB_NAME}.dbo.BIZALL where  RENDALL=BIZALLKOD and BIZALLNEV1 not in ('Lezárt','Ajánlat','Lezárva','Teljesült')) \n" +
                "  and RENDELF.JELZO='V' \n" +
                "  and RENDELT.riktszam=RENDELF.riktszam\n" +
                "  and (RENDELT.rendall=9 or RENDELT.rendall=10)\n" +
                "  group by vc.UGYFELKOD, vc.UGYFNEV,  vc.UGYFTNEV, RENDELF.RIKTSZAM, RENDELSZAM, RENDDATUM, RENDELF.NERTEK ,szc.UGYFNEV,HIVATKOZAS,STATUS " +
                "order by RENDELF.RIKTSZAM desc"
    }
}