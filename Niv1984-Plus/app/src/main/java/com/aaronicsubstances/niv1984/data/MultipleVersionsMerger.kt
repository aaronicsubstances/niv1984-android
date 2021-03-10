package com.aaronicsubstances.niv1984.data

import com.aaronicsubstances.niv1984.models.BookDisplayItem
import com.aaronicsubstances.niv1984.models.BookDisplayItemContent
import com.aaronicsubstances.niv1984.models.BookDisplayItemViewType
import com.aaronicsubstances.niv1984.utils.AppConstants
import com.aaronicsubstances.niv1984.utils.AppUtils
import com.aaronicsubstances.niv1984.utils.AsanteTwiNABibleVersion2020

class MultipleVersionsMerger(
    private val bookNumber: Int,
    private val bibleVersions: List<String>,
    private val displayMultipleSideBySide: Boolean) {

    fun mergeVersionsInSingleColumn(
        chapterNumber: Int,
        combinedDisplayItems: MutableList<BookDisplayItem>,
        allDisplayItems: List<List<BookDisplayItem>>,
        allBeginChapterIndices:List<Int>,
        allEndChapterIndices: List<Int>
    ) {
        // skip all but verses, dividers and footnotes

        val allPts = mutableListOf<Int>()
        val allDividers = mutableListOf<Int>()

        allDisplayItems.indices.forEach {
            val displayItems = allDisplayItems[it]
            val cIdx = allBeginChapterIndices[it]
            val cEndIdx = allEndChapterIndices[it]
            val locInfo = locateDividersForMerge(cIdx, cEndIdx, displayItems)
            val pt = locInfo[0]
            val dividerIdx = locInfo[1]
            allPts.add(pt)
            allDividers.add(dividerIdx)
        }

        // select title of first bible version to represent combination.
        combinedDisplayItems.add(allDisplayItems[0][allPts[0] - 1])

        var vNum = 1
        while (true) {
            // copy over items with given verse number for each version

            val allVerseRanges = mutableListOf<IntArray>()
            allDisplayItems.indices.forEach {
                val displayItems = allDisplayItems[it]
                val pt = allPts[it]
                val dividerIdx = allDividers[it]
                val verseRange = getVerseRange(vNum, pt, dividerIdx, displayItems)
                if (verseRange == null) {
                    // no need to continue loop
                    return@forEach
                }
                allVerseRanges.add(verseRange)
            }

            if (allVerseRanges.size < allDisplayItems.size) {
                break
            }

            // add intervening divider elements.
            if (vNum > 1) {
                combinedDisplayItems.add(
                    BookDisplayItem(BookDisplayItemViewType.DECOR_DIVIDER,
                        chapterNumber,vNum,
                        BookDisplayItemContent(0, "")
                    )
                )
            }

            allVerseRanges.indices.forEach {
                val displayItems = allDisplayItems[it]
                val verseRange = allVerseRanges[it]
                val subList1 = displayItems.subList(verseRange[0], verseRange[1])
                combinedDisplayItems.addAll(subList1)
                allPts[it] = verseRange[1]
            }

            vNum++
        }

        // skip over any item before divider unless item is a verse
        allDisplayItems.indices.forEach {
            val displayItems = allDisplayItems[it]
            val dividerIdx = allDividers[it]
            var pt = allPts[it]
            while (pt < dividerIdx) {
                val item = displayItems[pt]
                if (item.viewType == BookDisplayItemViewType.VERSE) {
                    break
                }
                pt++
            }
            allPts[it] = pt
        }

        if (permitAsymmetricVerseCounts(chapterNumber)) {
            while (true) {
                val allVerseRanges = mutableListOf<IntArray?>()
                allDisplayItems.indices.forEach {
                    val displayItems = allDisplayItems[it]
                    val pt = allPts[it]
                    val dividerIdx = allDividers[it]
                    val verseRange = getVerseRange(vNum, pt, dividerIdx, displayItems)
                    allVerseRanges.add(verseRange)
                }
                if (allVerseRanges.filterNotNull().isEmpty()) {
                    break
                }
                if (vNum > 1) {
                    combinedDisplayItems.add(
                        BookDisplayItem(
                            BookDisplayItemViewType.DECOR_DIVIDER,
                            chapterNumber, vNum,
                            BookDisplayItemContent(0, "")
                        )
                    )
                }
                allDisplayItems.indices.forEach {
                    val displayItems = allDisplayItems[it]
                    val verseRange = allVerseRanges[it]
                    if (verseRange != null) {
                        allPts[it] = verseRange[1]
                        val subList = displayItems.subList(verseRange[0], verseRange[1])
                        combinedDisplayItems.addAll(subList)
                    }
                }
                vNum++
            }
        }

        allDisplayItems.indices.forEach {
            val displayItems = allDisplayItems[it]
            val dividerIdx = allDividers[it]
            val pt = allPts[it]
            AppUtils.assert(pt == dividerIdx) {
                "bibleVersions[$it]:$chapterNumber.$vNum: $pt != $dividerIdx"
            }

            // add footnotes as one, for each bible version.
            val cEndIdx = allEndChapterIndices[it]
            combinedDisplayItems.addAll(displayItems.subList(dividerIdx, cEndIdx))
        }
    }

    fun mergeVersionsInTwoColumns(
        chapterNumber: Int,
        combinedDisplayItems: MutableList<BookDisplayItem>,
        displayItems1: List<BookDisplayItem>,
        cIdx1: Int, cEndIdx1: Int,
        displayItems2: List<BookDisplayItem>,
        cIdx2: Int, cEndIdx2: Int
    ) {
        // skip all but verses, dividers and footnotes

        var locInfo = locateDividersForMerge(cIdx1, cEndIdx1, displayItems1)
        var pt1 = locInfo[0]
        val dividerIdx1 = locInfo[1]

        locInfo = locateDividersForMerge(cIdx2, cEndIdx2, displayItems2)
        var pt2 = locInfo[0]
        val dividerIdx2 = locInfo[1]

        // set title for combination.
        if (displayMultipleSideBySide) {
            val firstPart = displayItems1[pt1 - 1]
            val secondPart = displayItems2[pt2 - 1]
            combinedDisplayItems.add(
                BookDisplayItem(
                    BookDisplayItemViewType.TITLE,
                chapterNumber, 0, BookLoader.DUMMY_CONTENT,
                firstPartialContent = listOf(firstPart.fullContent),
                secondPartialContent = listOf(secondPart.fullContent))
            )
        }
        else {
            // select title of first bible version to represent combination.
            combinedDisplayItems.add(displayItems1[pt1 - 1])
        }

        var vNum = 1
        while (true) {
            // copy over items with given verse number for each version

            val verseRange1 = getVerseRange(vNum, pt1, dividerIdx1, displayItems1)
            if (verseRange1 == null) {
                break
            }

            val verseRange2 = getVerseRange(vNum, pt2, dividerIdx2, displayItems2)
            if (verseRange2 == null) {
                break
            }

            val subList1 = displayItems1.subList(verseRange1[0], verseRange1[1])
            pt1 = verseRange1[1]

            val subList2 = displayItems2.subList(verseRange2[0], verseRange2[1])
            pt2 = verseRange2[1]

            if (displayMultipleSideBySide) {
                combinedDisplayItems.add(
                    BookDisplayItem(BookDisplayItemViewType.VERSE,
                    chapterNumber, vNum, BookLoader.DUMMY_CONTENT,
                    firstPartialContent = subList1.map { it.fullContent },
                    secondPartialContent = subList2.map { it.fullContent })
                )
            }
            else {
                combinedDisplayItems.addAll(subList1)
                combinedDisplayItems.addAll(subList2)
            }

            vNum++
        }

        // skip over any item before divider unless item is a verse
        while (pt1 < dividerIdx1) {
            val item = displayItems1[pt1]
            if (item.viewType == BookDisplayItemViewType.VERSE) {
                break
            }
            pt1++
        }
        while (pt2 < dividerIdx2) {
            val item = displayItems2[pt2]
            if (item.viewType == BookDisplayItemViewType.VERSE) {
                break
            }
            pt2++
        }

        if (permitAsymmetricVerseCounts(chapterNumber)) {
            while (true) {
                val verseRange1 = getVerseRange(vNum, pt1, dividerIdx1, displayItems1)
                val verseRange2 = getVerseRange(vNum, pt2, dividerIdx2, displayItems2)
                if (verseRange1 == null && verseRange2 == null) {
                    break
                }
                if (verseRange1 != null) {
                    val subList1 = displayItems1.subList(verseRange1[0], verseRange1[1])
                    pt1 = verseRange1[1]

                    if (displayMultipleSideBySide) {
                        combinedDisplayItems.add(
                            BookDisplayItem(
                                BookDisplayItemViewType.VERSE,
                                chapterNumber, vNum, BookLoader.DUMMY_CONTENT,
                                firstPartialContent = subList1.map { it.fullContent },
                                secondPartialContent = subList1.map {
                                    BookDisplayItemContent(
                                        1,
                                        ""
                                    )
                                })
                        )
                    } else {
                        combinedDisplayItems.addAll(subList1)
                    }
                }
                if (verseRange2 != null) {
                    val subList2 = displayItems2.subList(verseRange2[0], verseRange2[1])
                    pt2 = verseRange2[1]

                    if (displayMultipleSideBySide) {
                        combinedDisplayItems.add(
                            BookDisplayItem(
                                BookDisplayItemViewType.VERSE,
                                chapterNumber, vNum, BookLoader.DUMMY_CONTENT,
                                firstPartialContent = subList2.map {
                                    BookDisplayItemContent(
                                        0,
                                        ""
                                    )
                                },
                                secondPartialContent = subList2.map { it.fullContent })
                        )
                    } else {
                        combinedDisplayItems.addAll(subList2)
                    }
                }

                vNum++
            }
        }
        AppUtils.assert(pt1 == dividerIdx1) {
            "$chapterNumber.$vNum: $pt1 != $dividerIdx1"
        }
        AppUtils.assert(pt2 == dividerIdx2) {
            "$chapterNumber.$vNum: $pt2 != $dividerIdx2"
        }

        // add footnotes as one, for each bible version.
        combinedDisplayItems.addAll(displayItems1.subList(dividerIdx1, cEndIdx1))
        combinedDisplayItems.addAll(displayItems2.subList(dividerIdx2, cEndIdx2))
    }

    private fun permitAsymmetricVerseCounts(chapterNumber: Int): Boolean {
        if (bibleVersions.any { AppConstants.bibleVersions.getValue(it).isAsanteTwiBibleVersion() }) {
            // Revelation 12
            if (bookNumber == 66 && chapterNumber == 12) {
                return true
            }
            // 3 John
            if (bookNumber == 64) {
                return true
            }
        }
        if (bibleVersions.contains(AsanteTwiNABibleVersion2020.code)) {
            // currently Old Testament, Mark and Revelation are not included.
            // Old Testament has tables in it (e.g. Ezra), Mark has combined
            // verses somewhere
            if (bookNumber < 40 || bookNumber == 41 || bookNumber == 66) {
                return true;
            }
        }
        return false
    }

    private fun locateDividersForMerge(
        cIdx: Int, cEndIdx: Int,
        displayItems: List<BookDisplayItem>
    ): IntArray {
        var dividerIdx = cEndIdx - 1
        AppUtils.assert(displayItems[dividerIdx].viewType == BookDisplayItemViewType.DIVIDER) {
            "${displayItems[dividerIdx].viewType} != DIVIDER"
        }
        dividerIdx--
        while (true) {
            if (displayItems[dividerIdx].viewType == BookDisplayItemViewType.DIVIDER) {
                break
            }
            dividerIdx--
        }

        // deal with verses next.

        var pt = cIdx
        AppUtils.assert(displayItems[pt].viewType == BookDisplayItemViewType.TITLE) {
            "${displayItems[pt].viewType} != TITLE"
        }

        return intArrayOf(pt + 1, dividerIdx)
    }

    private fun getVerseRange(
        vNum: Int,
        initialPt: Int,
        dividerIdx: Int,
        displayItems: List<BookDisplayItem>
    ): IntArray? {
        var pt = initialPt
        while (pt < dividerIdx) {
            val item = displayItems[pt]
            if (item.viewType == BookDisplayItemViewType.VERSE &&
                item.verseNumber == vNum) {
                break
            }
            pt++
        }
        if (pt >= dividerIdx) {
            return null
        }
        var retResult = intArrayOf(pt, pt)
        while (pt < dividerIdx) {
            val item = displayItems[pt]
            if (item.verseNumber != vNum) {
                break
            }
            pt++
        }
        retResult[1] = pt
        return retResult
    }
}