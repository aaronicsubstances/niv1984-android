package com.aaronicsubstances.niv1984.data

import android.content.Context
import com.aaronicsubstances.niv1984.BuildConfig
import com.aaronicsubstances.niv1984.R
import com.aaronicsubstances.niv1984.models.HighlightRange
import com.aaronicsubstances.niv1984.models.UserHighlightData
import com.aaronicsubstances.niv1984.models.VerseBlockHighlightRange
import com.aaronicsubstances.niv1984.utils.AppUtils

class BookHighlighter(private val context: Context,
                      private val bookNumber: Int,
                      private val bibleVersion: String) {

    private val TAG = javaClass.name

    // use shades of yellow in both day and night modes
    private val highlightColor = AppUtils.colorResToString(R.color.highlightColor, context)

    private lateinit var highlightData: List<UserHighlightData>

    suspend fun load() {
        val db = AppDatabase.getDatabase(context)
        highlightData = db.userHighlightDataDao().getBookHighlightData(bibleVersion, bookNumber)
    }

    fun processBlockText(chapterNumber: Int, verseNumber: Int, verseBlockIndex: Int,
                         source: VerseHighlighter): String {
        source.beginProcessing()
        val blockHighlightData = highlightData.firstOrNull {
            it.chapterNumber == chapterNumber && it.verseNumber == verseNumber &&
                    it.verseBlockIndex == verseBlockIndex
        }
        val blockHighlights = if (blockHighlightData == null) {
            arrayOf()
        }
        else {
            AppUtils.deserializeFromJson(blockHighlightData.data, Array<HighlightRange>::class.java)
        }
        for (highlightRange in blockHighlights) {
            var insertedStartSpanIdx = -1
            try {
                insertedStartSpanIdx = source.updateMarkup(
                        highlightRange.startIndex,
                        "<span style='background-color: $highlightColor'>", false
                )
                source.updateMarkup(
                        highlightRange.endIndex,
                        "</span>", true
                )
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

    fun getHighlightModeRemovableMarkups(source: VerseHighlighter): List<VerseHighlighter.Markup>? {
        val removableMarkups = source.markupList.filter { it.removeDuringHighlighting }
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
                val existingHighlightRanges = if (existingBlockData == null) arrayOf() else
                    AppUtils.deserializeFromJson(existingBlockData.data, Array<HighlightRange>::class.java)
                val updated = if (removeHighlight) {
                    VerseHighlighter.removeHighlightRange(existingHighlightRanges, blockRange.range)
                }
                else {
                    VerseHighlighter.addHighlightRange(existingHighlightRanges, blockRange.range)
                }
                val newBlockData = UserHighlightData()
                newBlockData.let {
                    it.bibleVersion = bibleVersion
                    it.bookNumber = bookNumber
                    it.chapterNumber = chapterNumber
                    it.verseNumber = vNum
                    it.verseBlockIndex = blockRange.verseBlockIndex
                    it.data = AppUtils.serializeAsJson(updated)
                }
                entitiesToInsert.add(newBlockData)
            }
        }

        // clear cache just before saving.
        BookCache(context, bookNumber).purge(bibleVersion)

        db.userHighlightDataDao().updateHighlightData(entitiesToDelete, entitiesToInsert)
    }
}