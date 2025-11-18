package com.webtic.qrprint.ui.qrcode

import java.util.*

data class QrMetadataItem(
    val partNumber: String,
    val description: String,
    val kkod: String,
    val quantity: Int,
    val document: String,
    val lot: String,
    val firstPrinted: Date?,
    val reprinted: Date?,
)
