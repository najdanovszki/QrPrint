package com.webtic.qrprint.ui.search

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.webtic.qrprint.R
import com.webtic.qrprint.ui.BaseFragment
import com.webtic.qrprint.util.AppConstants.DB_NAME
import com.webtic.qrprint.util.ConnectionManager
import com.webtic.qrprint.util.QueryError
import com.webtic.qrprint.util.QuerySuccess
import dagger.hilt.android.AndroidEntryPoint
import java.sql.ResultSet
import javax.inject.Inject


@AndroidEntryPoint
class SearchFragment : BaseFragment() {

    private lateinit var table: TableLayout
    private lateinit var searchText: EditText
    private lateinit var searchBtn: ImageButton
    private lateinit var header: ViewGroup
    private val rows = mutableListOf<Pair<SearchItem, View>>()

    private val args: SearchFragmentArgs by navArgs()

    private val executeQuery: () -> Unit = {
        val sql = "select etk Cikk, gyartas KKOD, db Mennyiség, cikknev1 Leiras\n" +
                "from \n" +
                "(select kt.etk,gyartas, round(sum(case when mozgnem<200 then tetelmenny else tetelmenny*-1 end),2)  db, cikknev1\n" +
                "from ${DB_NAME}.dbo.tetel kt,\n" +
                "${DB_NAME}.dbo.cikk\n" +
                "where replace(kt.ETK,'-','') like '%${searchText.text}%' and cikk.etk=kt.etk and RAKTARKOD=1\n" +
                "group by kt.etk,gyartas, cikknev1) a\n" +
                "where db<>0 order by etk, gyartas"
        try {
            when (val result = connectionManager.executeQuery(sql)) {
                is QuerySuccess -> updateTable(result.resultSet)
                is QueryError -> Toast.makeText(
                    requireContext(),
                    result.exception.message,
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {
            Log.e("FETCH_OR_DISPLAY_DATA", e.message.toString())
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        table = view.findViewById(R.id.searchTable)
        searchText = view.findViewById(R.id.searchText)
        searchBtn = view.findViewById(R.id.searchBtn)
        header = table.children.first() as ViewGroup
        initRowSort()
        searchText.setOnEditorActionListener { _, actionId, _ ->
            when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    executeQuery()
                    hideKeyboard()
                    true
                }
                else -> false
            }
        }
        searchBtn.setOnClickListener {
            hideKeyboard()
            executeQuery()
        }
        args.searchParam?.let {
            searchText.setText(it)
            executeQuery()
        }
    }

    private fun initRowSort() {
        header.children.forEach { children ->
            children.setOnClickListener { clickedView ->
                table.removeViews(1, table.childCount - 1)
                rows.sortedBy {
                    when (clickedView.id) {
                        R.id.cikkszam -> it.first.cikkszam
                        R.id.keszletkod -> it.first.keszletkod
                        R.id.mennyiseg -> it.first.mennyiseg
                        R.id.leiras -> it.first.leiras
                        else -> it.first.cikkszam
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
                        R.id.cikkszam -> it.first.cikkszam
                        R.id.keszletkod -> it.first.keszletkod
                        R.id.mennyiseg -> it.first.mennyiseg
                        R.id.leiras -> it.first.leiras
                        else -> it.first.cikkszam
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
        table.removeViews(1, table.childCount - 1)
        while (result.next()) {
            val row = SearchItem(
                cikkszam = result.getString("Cikk") ?: "",
                keszletkod = result.getString("KKOD") ?: "",
                mennyiseg = result.getDouble("Mennyiség"),
                leiras = result.getString("Leiras") ?: "",
            )
            val rowView = layoutInflater.inflate(R.layout.template_search_table_row, null, true)
            rows.add(Pair(row, rowView))

            rowView.findViewById<TextView>(R.id.cikkszam).text = row.cikkszam
            rowView.findViewById<TextView>(R.id.keszletkod).text = row.keszletkod
            rowView.findViewById<TextView>(R.id.mennyiseg).text = String.format(row.mennyiseg.toString())
            rowView.findViewById<TextView>(R.id.leiras).text = row.leiras

            table.addView(rowView)
        }
    }

    private fun hideKeyboard() {
        val inputManager: InputMethodManager? =
            getSystemService(requireContext(), InputMethodManager::class.java)

        inputManager?.hideSoftInputFromWindow(
            requireActivity().currentFocus?.windowToken,
            InputMethodManager.HIDE_NOT_ALWAYS
        )
    }
}