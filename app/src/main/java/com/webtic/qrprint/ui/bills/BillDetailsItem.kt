package com.webtic.qrprint.ui.bills

data class BillDetailItem(
    var vevoUgyfelnev: String? = null,
    var szallUgyfelnev: String? = null,
    var orszagKod: String? = null,
    var pirkod: String? = null,
    var telepules: String? = null,
    var utca: String? = null,
    var szamlaszam: String? = null,
    var zeta: String? = null,
    var szallMod: String? = null,
    var fizMod: String? = null,
    var brutto: String? = null,
    var netto: String? = null,
    var devizanem: String? = null,
    var megjegyzes: String? = null,
    val cikkek: MutableList<Cikk> = mutableListOf()
)

data class Cikk(
    var cikk: String? = null,
    var kkod: String? = null,
    var mennyiseg: Double? = null,
    var mennyisegiEgyseg: String? = null,
    var raktarKeszlet: String? = null,
    var sorszam: Int? = null,
    var megjegyzes: String? = null,
    var ugyfelkod: Int? = null,
    var kkodMegoszlas: String? = null,
    val misc: String? = null
)