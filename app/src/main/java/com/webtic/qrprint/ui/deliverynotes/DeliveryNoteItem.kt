package com.webtic.qrprint.ui.deliverynotes

import java.sql.Date

data class DeliveryNoteItem(
    val ugyfelRovidNev: String,
    val szallitRovidNev: String,
    val kbiktszam: Int,
    val szlevSzam: String,
    val kelte: Date,
    val nettoErtek: Double,
    val bruttoSuly: Double,
    val ugyfnev: String,
    val eszamla: Boolean,
    val UGYFELKOD: String,
)
