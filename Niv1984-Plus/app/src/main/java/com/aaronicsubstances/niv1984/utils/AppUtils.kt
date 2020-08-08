package com.aaronicsubstances.niv1984.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.text.Html
import android.text.Spanned
import android.util.TypedValue
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.core.content.ContextCompat
import com.google.gson.GsonBuilder

object AppUtils {
    const val VIEW_TYPE_LOADING = -1
    const val DEFAULT_CHARSET = "utf-8"

    private val JSON_SERIALIZER = GsonBuilder().create()

    fun serializeAsJson(obj: Any): String {
        return JSON_SERIALIZER.toJson(obj)
    }

    fun <T> deserializeFromJson(s: String, cls: Class<T>): T {
        return JSON_SERIALIZER.fromJson(s, cls)
    }

    fun parseHtml(txt: String, tagHandler: Html.TagHandler? = null): Spanned {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            Html.fromHtml(txt, Html.FROM_HTML_MODE_COMPACT, null, tagHandler)
        }
        else {
            Html.fromHtml(txt, null, tagHandler)
        }
    }

    fun pxToDp(px: Int): Float {
        return px / Resources.getSystem().displayMetrics.density
    }

    fun dpToPx(dp: Int): Float {
        return dp * Resources.getSystem().displayMetrics.density
    }

    fun spToPx(sp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, Resources.getSystem().displayMetrics)
    }

    fun dimenResToPx(@DimenRes dimenRes: Int, context: Context): Int {
        return context.resources.getDimensionPixelOffset(dimenRes)
    }

    fun colorResToString(@ColorRes colorRes: Int, context: Context): String {
        val intColor = ContextCompat.getColor(context, colorRes)
        if (intColor ushr 24 == 0xFF) {
            return String.format("#%06X", intColor and 0xFFFFFF)
        }
        else {
            return String.format("#%08X", intColor.toLong() and 0xFFFFFFFF)
        }
    }

    fun isNightMode(context: Context): Boolean
            = (context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    fun showShortToast(context: Context?, s: String) {
        Toast.makeText(context, s, Toast.LENGTH_SHORT).show()
    }

    fun showLongToast(context: Context?, s: String) {
        Toast.makeText(context, s, Toast.LENGTH_LONG).show()
    }

    fun getAllBooks(topBooks: List<String>): ArrayList<String> {
        val allBooks = ArrayList(AppConstants.bibleVersions.keys)

        // Ensure top books appear on top.
        for (i in topBooks.indices) {
            // find ith top book in allBooks and swap it with
            // ith position in allBooks
            val idx = allBooks.indexOf(topBooks[i])
            assert(idx != -1) {
                "Could not find book ${topBooks[i]}"
            }
            val temp = allBooks[i]
            allBooks[i] = topBooks[i]
            allBooks[idx] = temp
        }

        return allBooks
    }

    fun assert(value: Boolean, lazyMessage: (()->Any)? = null){
        if (!value) {
            if (lazyMessage != null) {
                throw AssertionError(lazyMessage().toString())
            }
            throw AssertionError()
        }
    }
}