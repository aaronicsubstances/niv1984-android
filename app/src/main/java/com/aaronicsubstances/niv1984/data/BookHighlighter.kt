package com.aaronicsubstances.niv1984.data

import android.content.Context
import com.aaronicsubstances.niv1984.R
import com.aaronicsubstances.niv1984.models.HighlightRange
import com.aaronicsubstances.niv1984.models.UserHighlightData
import com.aaronicsubstances.niv1984.utils.AppUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BookHighlighter(private val context: Context,
                      private val bookNumber: Int,
                      private val bibleVersion: String) {

    // use shades of yellow in both day and night modes
    private val highlightColor = AppUtils.colorResToString(R.color.highlightColor, context)

    private lateinit var highlightData: List<UserHighlightData>

    suspend fun save(chapterNumber: Int,
                     changes: List<IntArray>,
                     removeHighlight: Boolean) {
        withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            val uniqueVerseNumbers = changes.map { it[0] }.distinct()
            val entitiesToDelete = mutableListOf<UserHighlightData>()
            val entitiesToInsert = mutableListOf<UserHighlightData>()
            for (vNum in uniqueVerseNumbers) {
                val newBlockRanges = changes.filter { it[0] == vNum }
                val existingBlockRanges = db.userHighlightDataDao().fetchHighlightData(
                    bibleVersion, bookNumber,
                    chapterNumber, vNum, newBlockRanges.map{ it[1] })
                entitiesToDelete.addAll(existingBlockRanges)
                for (blockRange in newBlockRanges) {
                    val existingBlockData = existingBlockRanges.firstOrNull { it.verseBlockIndex == blockRange[1] }
                    val existingHighlightRanges = if (existingBlockData == null) arrayOf() else {
                        AppUtils.deserializeFromJson(existingBlockData.data, Array<HighlightRange>::class.java)
                    }
                    val updated = if (removeHighlight) {
                        VerseHighlighter.removeHighlightRange(existingHighlightRanges,
                            HighlightRange(blockRange[2], blockRange[3]))
                    }
                    else {
                        VerseHighlighter.addHighlightRange(existingHighlightRanges,
                            HighlightRange(blockRange[2], blockRange[3]))
                    }
                    val newBlockData = UserHighlightData()
                    newBlockData.apply {
                        this.bibleVersion = bibleVersion
                        this.bookNumber = bookNumber
                        this.chapterNumber = chapterNumber
                        this.verseNumber = vNum
                        this.verseBlockIndex = blockRange[1]
                        this.data = AppUtils.serializeAsJson(updated)
                    }
                    entitiesToInsert.add(newBlockData)
                }
            }
            db.userHighlightDataDao().updateHighlightData(entitiesToDelete,
                entitiesToInsert)
        }
    }

    suspend fun load() {
        highlightData = withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            db.userHighlightDataDao().getHighlightData(bibleVersion, bookNumber)
        }
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
        try {
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
        }
        catch (ex: Exception) {
            val exLoc = "$chapterNumber.${verseNumber}[$verseBlockIndex]"
            AppUtils.showLongToast(context, "Unable to apply highlights to $exLoc: ${ex.message}")
            var i = source.markupList.size -1
            while (i >= 0) {
                if (source.markupList[i].addedDuringUpdate) {
                    source.markupList.removeAt(i)
                }
                i--
            }
        }
        source.finalizeProcessing()
        return source.rawText.toString()
    }

    fun getHighlightModeRemovableMarkups(source: VerseHighlighter): List<VerseHighlighter.Markup>? {
        val removableMarkups = source.markupList.filter { it.id != null }
        return if (removableMarkups.isEmpty()) null else removableMarkups
    }
}