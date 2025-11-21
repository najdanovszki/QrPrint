package com.webtic.qrprint.ui.tasks

import java.util.*

data class TaskItem(
    val statusz: String,
    val ugyfelRovidNev: String,
    val szallRovidNev: String,
    val cikk: String,
    val kkod: String,
    val rendelSzam: String,
    val hi: Date,
    val tartozas: Double,
    val kiadhato: Double,
    val keszlet: Double,
    val szallMod: String,
    val hivatkozas: String,
    val status: Int,
    val tetelssz: Int,
    val ugyfelkod: Int,
    val kkodEleje: String,
    val riktszam: String,
)