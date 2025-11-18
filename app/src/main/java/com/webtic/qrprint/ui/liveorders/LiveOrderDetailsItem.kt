package com.webtic.qrprint.ui.liveorders

import com.webtic.qrprint.models.SubDetailsTableItem
import java.sql.Date

data class LiveOrderDetailItem(
    var vevoUgyfelnev: String? = null,
    var szallUgyfelnev: String? = null,
    var orszagKod: String? = null,
    var pirkod: String? = null,
    var telepules: String? = null,
    var utca: String? = null,
    var rendelszam: String? = null,
    var zeta: String? = null,
    var szallMod: String? = null,
    var fizMod: String? = null,
    var brutto: String? = null,
    var netto: String? = null,
    var devizanem: String? = null,
    var status: String? = null,
    val cikkek: MutableList<Cikk> = mutableListOf(),
    val subDetailsList: MutableList<SubDetailsTableItem> = mutableListOf()
)

data class Cikk(
    val cikk: String? = null,
    val kkod: String? = null,
    val mennyiseg: Double? = null,
    val mennyisegiEgyseg: String? = null,
    val hatarido: Date? = null,
    val kiadhato: String? = null,
    val raktarKeszlet: String? = null,
    val sorszam: Int? = null,
    val megjegyzes: String? = null,
    val ugyfelkod: Int? = null,
    val misc: String? = null,
    var kkodMegoszlas: String? = null,
    var checked: Boolean = false,
    val itemNo: String = ""
)
