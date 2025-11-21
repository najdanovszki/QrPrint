package com.webtic.qrprint.ui.liveorders

import java.sql.Date

data class LiveOrderItem(
    val ugyfelRovidNev: String,
    val szallitRovidNev: String,
    val riktszam: Int,
    val rendSzam: String,
    val rendelesDatuma: Date,
    val nettoErtek: Double,
    val bruttoSuly: Double,
    val szallitasiCimNeve: String,
    val hivatkozas: String,
    val hatarido: Date,
    val status: Int,
    var teljAzonos: String = "",
    var teljMas: String = "",
    var UGYFELKOD: String = ""
)
