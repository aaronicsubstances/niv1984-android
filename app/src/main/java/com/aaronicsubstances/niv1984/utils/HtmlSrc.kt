package com.aaronicsubstances.niv1984.utils

import android.content.Context
import android.text.Editable
import android.text.Html
import android.text.Spanned
import androidx.core.content.ContextCompat
import com.aaronicsubstances.niv1984.R
import com.aaronicsubstances.niv1984.ui_helpers.CustomHRSpan
import org.xml.sax.XMLReader

class HtmlSrc: Html.TagHandler {
    val spanned: Spanned
    val versePosMap = mutableMapOf<String, Pair<Int, Int>>()
    private val context: Context

    constructor(context: Context, txt: String) {
        this.context = context
        spanned =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                Html.fromHtml(txt, Html.FROM_HTML_MODE_LEGACY, null, this)
            }
            else {
                Html.fromHtml(txt, null, this)
            }

    }
    override fun handleTag(
        opening: Boolean,
        tag: String?,
        output: Editable?,
        xmlReader: XMLReader?
    ) {
        if(tag.equals("hr")) {
            val hrColor = ContextCompat.getColor(context, R.color.hrColor)
            handleHRTag(opening, output!!, hrColor);
            return
        }

        if (!tag!!.startsWith(vPrefix)) return

        val vNum = "v" + tag.substring(vPrefix.length)
        if (opening) {
            versePosMap[vNum] = Pair(output!!.length, 0)
        }
        else {
            val beginValue = versePosMap.remove(vNum)
            versePosMap[vNum] = Pair(beginValue!!.first, output!!.length)
        }
    }

    private fun handleHRTag(opening: Boolean, output: Editable, color: Int) {
        if (!opening) return
        val start = output.length
        // The space is necessary, the block requires some content:
        output.append(" \n")
        output.setSpan(
            CustomHRSpan(color, 5.0f, 2.0f),
            start, output.length, 0
        )
    }

    companion object {
        private val vPrefix = "verse_"

        fun getBookText(context: Context, bibleVersionCode: String, bookIndex: Int): String {
            val chapterCount = AppConstants.BIBLE_BOOK_CHAPTER_COUNT[bookIndex]
            val out = StringBuilder()
            for (chapNum in 1..chapterCount) {
                loadChapterText(context, bibleVersionCode, bookIndex + 1,
                    chapNum, out)
            }
            return out.toString()
        }

        private fun loadChapterText(context: Context, bibleVersion: String,
                                   bookNum: Int, chapNum: Int, out: StringBuilder) {
            val assetPath = String.format("%s/%02d/%03d.tvn", bibleVersion, bookNum, chapNum)
            context.assets.open(assetPath).bufferedReader().use {
                out.append("<h2>").append("<font color='black'>Chapter ${chapNum}</font>")
                    .append("</h2>\n")
                var closeFancyContent: String? = null
                var verseNum = 0
                while (true) {
                    val line = it.readLine()
                    if (line == null) break
                    var colonIndex = line.indexOf(':')
                    val tag = line.substring(0, colonIndex)
                    var value = line.substring(colonIndex + 1)
                    when (tag) {
                        "v_start" -> {
                            verseNum = Integer.parseInt(value)
                            out.append("<$vPrefix$verseNum><p><font color='black'>")
                            out.append(value).append(". ")
                        }
                        "v_end" -> {
                            out.append("\n</font></p></$vPrefix$verseNum>\n")
                        }
                        "fancy_content" -> {
                            assert(value == "em") {
                                "Expected 'em' but found: $value"
                            }
                            closeFancyContent = "i"
                            out.append("<").append(closeFancyContent).append(">")
                        }
                        "content" -> {
                            out.append(value)
                            if (closeFancyContent != null) {
                                out.append("</").append(closeFancyContent).append(">")
                            }
                            closeFancyContent = null
                        }
                    }
                }
                out.append("<hr><br>")
            }
        }
    }
}