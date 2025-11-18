package com.webtic.qrprint.ui.revenues

import java.sql.Date

data class RevenueItem(
    val ugyfelRovidNev: String,
    val szallitRovidNev: String,
    val kbiktszam: Int,
    val szlevSzam: String,
    val kelte: Date,
    val nettoErtek: Double,
    val bruttoSuly: Double,
    val ugyfnev: String,
    val UGYFELKOD: String
)
