package com.webtic.qrprint.ui.bills

import java.sql.Date

data class BillItem(
    val ugyfelRovidNev: String,
    val szallitRovidNev: String,
    val sziktszam: Int,
    val szamlaszam: String,
    val kelte: Date,
    val netto: Double,
    val suly: Double,
)
