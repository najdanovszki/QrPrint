package com.webtic.qrprint.util

object AppConstants {
    const val DB_SERVER:    String = "192.168.121.15"
    var DB_NAME:      String = "vyw"
    const val DB_USER_NAME: String = ""
    const val DB_USER_PASS: String = ""
    const val DB_QTY_TBL_NAME: String   = "ZETAEPRMENNY"
    const val DB_CHECKED_COLUMN_NAME: String = "CHECKED"

    const val LIVE_ORDER_DETAILS_SORT_KEY: String = "live_order_details_sort_key"
    const val DELIVERY_NOTE_DETAILS_SORT_KEY: String = "delivery_note_details_sort_key"
    const val REVENUE_DETAILS_SORT_KEY: String = "revenue_details_sort_key"

    fun addLineBreak(originalString: String, charCount: Int): String{
        val stringBuilder = StringBuilder()
        var count = 0

        for (char in originalString) {
            stringBuilder.append(char)
            count++

            if (count == charCount) {
                stringBuilder.append('\n')
                count = 0
            }
        }

        return stringBuilder.toString()
    }
}