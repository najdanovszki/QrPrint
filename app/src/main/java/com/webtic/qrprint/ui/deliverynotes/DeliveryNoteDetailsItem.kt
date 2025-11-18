package com.webtic.qrprint.ui.deliverynotes

import com.webtic.qrprint.models.QuantityDetails
import com.webtic.qrprint.models.SubDetailsTableItem

data class DeliveryNoteDetailsItem(
    var vevo: String? = null,
    var szall: String? = null,
    var orszag: String? = null,
    var pirkod: String? = null,
    var telepules: String? = null,
    var utca: String? = null,
    var kbizszam: String? = null,
    var zeta: String? = null,
    var szallMod: String? = null,
    var fizMod: String? = null,
    var brutto: String? = null,
    var netto: String? = null,
    var devizanem: String? = null,
    val cikkek: MutableList<Cikk> = mutableListOf(),
    val subDetailsList: MutableList<SubDetailsTableItem> = mutableListOf(),
    val quantityDetails: QuantityDetails? = null
)

data class Cikk(
    val cikk: String? = null,
    val kkod: String? = null,
    val mennyiseg: Double? = null,
    val mennyisegiEgyseg: String? = null,
    val tetelsorszam: String? = null,
    val sorszam: Int? = null,
    val megjegyzes: String? = null,
    val ugyfelkod: Int? = null,
    val misc: String? = null,
    val raktarKeszlet: String? = null,
    var gyariszam: String? = null,
    var kkodMegoszlas: String? = null,
    var checked: Boolean = false,
    val itemNo: String = ""
)
