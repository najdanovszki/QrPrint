package com.webtic.qrprint.ui.qrcode

import java.io.Serializable

data class NavigationItem(
    val partNumber: String,
    val kkod: String,
    val storage: String,
    val description: String,
    val document: String?,
    val quantity: Int,
    val notFromRevenues: Boolean,
    val clientId: Int?,
) : Serializable

fun decodeQuantity(quantity: String, multiplier: String): Int {
    val mul = multiplier.split(" ")
    return when (mul.first()) {
        "db" -> quantity.toDouble().toInt()
        "100db" -> (quantity.toDouble() * 100).toInt()
        "e" -> (quantity.toDouble() * 1000).toInt()
        else -> quantity.toDouble().toInt()
    }
}
