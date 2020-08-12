package com.aaronicsubstances.niv1984.data

import android.content.Context
import com.aaronicsubstances.niv1984.BuildConfig
import com.aaronicsubstances.niv1984.R
import com.aaronicsubstances.niv1984.models.UserHighlightData
import com.aaronicsubstances.niv1984.models.VerseBlockHighlightRange
import com.aaronicsubstances.niv1984.utils.AppUtils

class BookHighlighter(private val context: Context,
                      private val bookNumber: Int,
                      private val bibleVersion: String) {

    private val TAG = javaClass.name

    companion object {
        const val MARKUP_ID_HIGHLIGHT = "highlight"
    }

    // use shades of yellow in both day and night modes
    private val highlightColor = AppUtils.colorResToString(R.color.highlightColor, context)

    private lateinit var highlightData: List<UserHighlightData>
    private var latestChapterHighlights = listOf<UserHighlightData>()

    suspend fun load(chapterNumbersToIgnore: List<Int>) {
        val db = AppDatabase.getDatabase(context)
        highlightData = db.userHighlightDataDao().getBookHighlightData(bibleVersion, bookNumber,
                chapterNumbersToIgnore)
    }

    fun loadChapterHighlights(chapterNumber: Int): Boolean {
        latestChapterHighlights = highlightData.filter {
            it.chapterNumber == chapterNumber
        }
        return latestChapterHighlights.isNotEmpty()
    }

    fun processBlockText(verseNumber: Int, verseBlockIndex: Int,
                         source: VerseHighlighter): String {
        source.beginProcessing()
        val blockHighlightData = latestChapterHighlights.firstOrNull {
            it.verseNumber == verseNumber && it.verseBlockIndex == verseBlockIndex
        }
        val blockHighlights = if (blockHighlightData == null) listOf() else {
            AppBinarySerializer.deserializeHighlightRanges(blockHighlightData.data)
        }
        for (highlightRange in blockHighlights) {
            var insertedStartSpanIdx = -1
            try {
                insertedStartSpanIdx = source.updateMarkup(
                        highlightRange.startIndex,
                        "<span style='background-color: $highlightColor'>", false
                )
                // ensure alpha channel addition.
                val startSpanId = "$MARKUP_ID_HIGHLIGHT+FF${highlightColor.substring(1)}"
                source.markupList[insertedStartSpanIdx].id = startSpanId
                val endSpanIdx = source.updateMarkup(
                        highlightRange.endIndex,
                        "</span>", true
                )
                source.markupList[endSpanIdx].id = startSpanId.replaceFirst('+', '-')
            }
            catch (ex: Exception) {
                if (BuildConfig.DEBUG) {
                    throw ex
                }

                // in production usage it won't be desirable for a single troublesome
                // highlight range to block the display of an entire book!
                // ignore error and therefore highlighting if start span could not be applied.
                if (insertedStartSpanIdx != -1) {
                    // on the other hand if start span was applied, then add end span
                    // if end index is equivalent to end of source raw text.
                    // else remove added start span.
                    if (highlightRange.endIndex >= source.rawText.length) {
                        source.updateMarkup(
                                source.rawText.length,
                                "</span>", true
                        )
                    }
                    else {
                        source.markupList.removeAt(insertedStartSpanIdx)
                    }
                }
            }
        }
        source.finalizeProcessing()
        return source.rawText.toString()
    }

    fun getHighlightModeEditableMarkups(source: VerseHighlighter): List<VerseHighlighter.Markup>? {
        val removableMarkups = source.markupList.filter {
            it.removeDuringHighlighting || it.id?.startsWith(MARKUP_ID_HIGHLIGHT) == true
        }
        return if (removableMarkups.isEmpty()) null else removableMarkups
    }

    suspend fun save(chapterNumber: Int,
                     changes: List<VerseBlockHighlightRange>,
                     removeHighlight: Boolean) {
        val db = AppDatabase.getDatabase(context)
        val uniqueVerseNumbers = changes.map { it.verseNumber }.distinct()
        val entitiesToDelete = mutableListOf<UserHighlightData>()
        val entitiesToInsert = mutableListOf<UserHighlightData>()
        for (vNum in uniqueVerseNumbers) {
            val newBlockRanges = changes.filter { it.verseNumber == vNum }
            val existingBlockRanges = db.userHighlightDataDao().getChapterHighlightData(
                    bibleVersion, bookNumber,
                    chapterNumber, vNum, newBlockRanges.map{ it.verseBlockIndex })
            entitiesToDelete.addAll(existingBlockRanges)
            for (blockRange in newBlockRanges) {
                val existingBlockData = existingBlockRanges.firstOrNull {
                    it.verseBlockIndex == blockRange.verseBlockIndex
                }
                val existingHighlightRanges = if (existingBlockData == null) listOf() else
                    AppBinarySerializer.deserializeHighlightRanges(existingBlockData.data)
                val updated = if (removeHighlight) {
                    VerseHighlighter.removeHighlightRange(existingHighlightRanges, blockRange.range)
                }
                else {
                    VerseHighlighter.addHighlightRange(existingHighlightRanges, blockRange.range)
                }
                val serializedUpdate = AppBinarySerializer.serializeHighlightRanges(updated)
                val newBlockData = UserHighlightData(0,
                    bibleVersion,
                    bookNumber,
                    chapterNumber,
                    vNum,
                    blockRange.verseBlockIndex,
                    serializedUpdate
                )
                entitiesToInsert.add(newBlockData)
            }
        }

        // clear cache just before saving.
        BookCache(context, bookNumber).purge(bibleVersion, chapterNumber)

        db.userHighlightDataDao().updateHighlightData(entitiesToDelete, entitiesToInsert)
    }
}