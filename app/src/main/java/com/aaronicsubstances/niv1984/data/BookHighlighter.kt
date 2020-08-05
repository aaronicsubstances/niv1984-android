package com.aaronicsubstances.niv1984.data

import android.content.Context
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
        //try {
            for (highlightRange in blockHighlights) {
                source.updateMarkup(
                    highlightRange.startIndex,
                    "<span style='background-color: $highlightColor'>", false
                )
                source.updateMarkup(
                    highlightRange.endIndex,
                    "</span>", true
                )
            }
        /*}
        catch (ex: Exception) {
            val exLoc = "$chapterNumber.${verseNumber}[$verseBlockIndex]"
            android.util.Log.e(TAG, "Unable to apply highlights to $exLoc: ${ex.message}", ex)
            var i = source.markupList.size -1
            while (i >= 0) {
                if (source.markupList[i].addedDuringUpdate) {
                    source.markupList.removeAt(i)
                }
                i--
            }
        }*/
        source.finalizeProcessing()
        return source.rawText.toString()
    }

    fun getHighlightModeRemovableMarkups(source: VerseHighlighter): List<VerseHighlighter.Markup>? {
        val removableMarkups = source.markupList.filter { it.id != null }
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
        db.userHighlightDataDao().updateHighlightData(entitiesToDelete, entitiesToInsert)
    }
}