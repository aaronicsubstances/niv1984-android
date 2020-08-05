package com.aaronicsubstances.niv1984.ui.book_reading

import android.content.Context
import android.graphics.Rect
import android.text.*
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.aaronicsubstances.niv1984.R
import com.aaronicsubstances.niv1984.models.HighlightRange
import com.aaronicsubstances.niv1984.models.VerseBlockHighlightRange
import com.aaronicsubstances.niv1984.utils.AppUtils
import org.xml.sax.XMLReader

class HtmlViewManager(private val context: Context): Html.TagHandler {
    private val TAG = javaClass.name
    val vPrefix = "verse_"
    val bPrefix = "block_"

    private val versePosMap = mutableMapOf<String, Pair<Int, Int>>()
    val verseBlockRanges = mutableListOf<VerseBlockHighlightRange>()
    var lastVerseNumberSeen = 0

    private var htmlLayout: Layout? = null
    private var scrollHeightRange: Int = -1
    private val lineInfoList = mutableListOf<TextLineInfo>()

    override fun handleTag(
        opening: Boolean,
        tag: String,
        output: Editable,
        xmlReader: XMLReader
    ) {
        if (tag == "hr") {
            val hrColor = ContextCompat.getColor(context, R.color.dividerColor)
            handleHRTag(opening, output, hrColor)
            return
        }

        if (tag.startsWith(bPrefix)) {
            val verseBlockIndex = Integer.parseInt(tag.substring(bPrefix.length))
            if (opening) {
                verseBlockRanges.add(VerseBlockHighlightRange(lastVerseNumberSeen,
                        verseBlockIndex, HighlightRange(output.length, 0)))
            }
            else {
                val lastBlockEntry = verseBlockRanges[verseBlockRanges.size - 1]
                AppUtils.assert(lastBlockEntry.verseNumber == lastVerseNumberSeen)
                AppUtils.assert(lastBlockEntry.verseBlockIndex == verseBlockIndex)
                lastBlockEntry.range.endIndex = output.length
                AppUtils.assert(lastBlockEntry.range.startIndex < lastBlockEntry.range.endIndex) {
                    "Unexpected block range indices: $lastBlockEntry"
                }
            }
        }

        if (!tag.startsWith(vPrefix)) return

        if (opening) {
            versePosMap[tag] = Pair(output.length, 0)
            lastVerseNumberSeen = Integer.parseInt(tag.substring(vPrefix.length))
        }
        else {
            val beginValue = versePosMap.getValue(tag)
            versePosMap[tag] = Pair(beginValue.first, output.length)
        }
    }

    private fun handleHRTag(opening: Boolean, output: Editable, color: Int) {
        if (!opening) return
        val start = output.length
        // The space is necessary, the block requires some content:
        output.append("\u00a0") // &nbsp;
        output.setSpan(
            CustomHRSpan(color, 5.0f, 2.0f),
            start, output.length, 0
        )
    }

    fun reset() {
        // reset only text view related data, and leave
        // scroll unchanged.
        versePosMap.clear()
        verseBlockRanges.clear()
        htmlLayout = null
        lineInfoList.clear()
        lastVerseNumberSeen = 0
    }

    fun getVerseNumber(contentTf: TextView, scrollYPos: Int): Int {
        val yOffset = contentTf.top
        for (txtLineInfo in lineInfoList) {
            if (scrollYPos - yOffset >= txtLineInfo.topPos &&
                    scrollYPos - yOffset <= txtLineInfo.bottomPos) {
                return getVerseNumber(txtLineInfo)
            }
        }
        return 0
    }

    private fun getVerseNumber(txtLineInfo: TextLineInfo): Int {
        for (e in versePosMap) {
            if (txtLineInfo.lineStart < e.value.first || txtLineInfo.lineStart >= e.value.second) {
                continue
            }
            if (txtLineInfo.lineEnd < e.value.first || txtLineInfo.lineEnd >= e.value.second) {
                continue
            }
            return Integer.parseInt(e.key.substring(vPrefix.length))
        }
        return 0
    }

    fun goToVerse(vNum: Int, sp: ScrollView, contentTf: TextView) {
        if (vNum < 1) {
            sp.scrollTo(0, 0)
            return
        }
        val vKey = "$vPrefix$vNum"
        if (!versePosMap.contains(vKey)) {
            AppUtils.showShortToast(context, "$vKey not found in map")
            return
        }
        val vLoc = versePosMap.getValue(vKey)
        //android.util.Log.d(TAG, "$vKey maps to $vLoc")

        var vStart = vLoc.first
        var vEnd = vLoc.second

        //Selection.setSelection(contentTf.text as Spannable, vStart, vEnd)
        //contentTf.performLongClick()

        if (lineInfoList.isEmpty()) {
            AppUtils.showShortToast(context, "line info list is empty")
            return
        }

        var topLineIndex = -1
        var bottomLineIndex = -1
        for (i in 0 until lineInfoList.size) {
            val txtLineInfo = lineInfoList[i]
            if (topLineIndex == -1 && vStart >= txtLineInfo.lineStart && vStart < txtLineInfo.lineEnd) {
                topLineIndex = i
            }
            if (bottomLineIndex == -1 && vEnd >= txtLineInfo.lineStart && vEnd < txtLineInfo.lineEnd) {
                bottomLineIndex = i
            }
            if (topLineIndex != -1 && bottomLineIndex != -1) {
                break
            }
        }

        if (topLineIndex == -1) {
            AppUtils.showShortToast(context, "$vKey -> top line for $vStart not found in list")
            return
        }
        if (bottomLineIndex == -1) {
            AppUtils.showShortToast(context, "$vKey -> bottom line for $vEnd not found in list")
            return
        }

        val vTopLineInfo = lineInfoList[topLineIndex]
        /*val vBottomLineInfo = lineInfoList[bottomLineIndex]
        android.util.Log.d(TAG, "$vKey maps to $vTopLineInfo, $vBottomLineInfo")*/

        /*if (scrollHeightRange == -1) {
            AppUtils.showShortToast(context, "scrollHeightRange is not set")
            return
        }

        val scrY = sp.scrollY*/
        val yOffset = contentTf.top
        /*android.util.Log.d(TAG, "using scrollHeightRange=$scrollHeightRange, scrollY=$scrY" +
                ", contentTop=$yOffset")*/

        /*if (vTopLineInfo.topPos + yOffset >= scrY && vTopLineInfo.topPos + yOffset <= scrY + scrollHeightRange &&
            vBottomLineInfo.bottomPos + yOffset >= scrY && vBottomLineInfo.bottomPos + yOffset <= scrY + scrollHeightRange) {
            android.util.Log.d(TAG, "no need to scroll: already visible")
            return
        }

        val yPos = vBottomLineInfo.baselinePos + yOffset - scrollHeightRange*/
        val yPos = vTopLineInfo.topPos + yOffset

        //android.util.Log.d(TAG, "$vKey maps to yPos=$yPos")
        sp.scrollTo(0, yPos)
    }

    fun onScrollViewSizeChanged(v: ScrollView, w: Int, h: Int) {
        val newScrollHeightRange = h - v.paddingTop - v.paddingBottom
        if (newScrollHeightRange == scrollHeightRange) {
            //android.util.Log.w(TAG, "onScrollViewDrawn with same scroll height range")
            return
        }

        //android.util.Log.w(TAG, "onScrollViewDrawn with different scroll height range")

        scrollHeightRange = newScrollHeightRange
    }

    fun onTextViewDrawn(v: TextView) {
        if (v.layout == htmlLayout) {
            //android.util.Log.w(TAG, "onTextViewDrawn with same layout")
            return
        }

        //android.util.Log.w(TAG, "onTextViewDrawn with different layout")

        htmlLayout = v.layout

        lineInfoList.clear()
        val rcv = Rect()
        for (i in 0 until v.lineCount) {
            val baselinePos = v.getLineBounds(i, rcv)
            val topPos = rcv.top
            val bottomPos = rcv.bottom
            val lineStart = htmlLayout!!.getLineStart(i)
            val lineEnd = htmlLayout!!.getLineEnd(i)

            lineInfoList.add(
                TextLineInfo(
                    lineStart, lineEnd, topPos,
                    bottomPos, baselinePos
                )
            )
        }
    }
}

data class TextLineInfo(var lineStart: Int, var lineEnd: Int, var topPos: Int,
                        var bottomPos: Int, var baselinePos: Int)