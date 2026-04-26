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

    // Parametrizable label font sizes (dp multiplied by 3f for px conversion)
    const val LABEL_PART_NO_FONT_DP: Float = 20f  // Part number / customer code in label header
    const val LABEL_INFO_FONT_DP: Float = 11f     // QTY, DESC, FP, RP and other info on label

    // Currently logged-in DB user (set at login, used for logging)
    var loggedInUser: String = ""
    const val LOG_TABLE_NAME: String = "QrPrint_log"

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