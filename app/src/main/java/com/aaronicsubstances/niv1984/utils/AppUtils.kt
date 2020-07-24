package com.aaronicsubstances.niv1984.utils

import android.content.Context
import android.content.res.Resources
import android.text.Html
import android.text.Spanned
import android.util.TypedValue
import android.widget.Toast
import androidx.annotation.DimenRes

object AppUtils {
    fun parseHtml(txt: String): Spanned {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            Html.fromHtml(txt, Html.FROM_HTML_MODE_COMPACT, null, null)
        }
        else {
            Html.fromHtml(txt, null, null)
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

    fun showShortToast(context: Context?, s: String) {
        Toast.makeText(context, s, Toast.LENGTH_SHORT).show()
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
}