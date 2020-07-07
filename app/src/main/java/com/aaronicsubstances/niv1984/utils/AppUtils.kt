package com.aaronicsubstances.niv1984.utils

import android.text.Html
import android.text.Spanned

object AppUtils {
    fun parseHtml(txt: String): Spanned {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            Html.fromHtml(txt, Html.FROM_HTML_MODE_LEGACY, null, null)
        }
        else {
            Html.fromHtml(txt, null, null)
        }
    }
}