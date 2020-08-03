package com.aaronicsubstances.niv1984.utils

import com.aaronicsubstances.niv1984.utils.BookParser.BlockQuote
import com.aaronicsubstances.niv1984.utils.BookParser.BlockQuoteKind
import com.aaronicsubstances.niv1984.utils.BookParser.Chapter
import com.aaronicsubstances.niv1984.utils.BookParser.ChapterFragment
import com.aaronicsubstances.niv1984.utils.BookParser.ChapterFragmentKind
import com.aaronicsubstances.niv1984.utils.BookParser.FancyContent
import com.aaronicsubstances.niv1984.utils.BookParser.FancyContentKind
import com.aaronicsubstances.niv1984.utils.BookParser.Note
import com.aaronicsubstances.niv1984.utils.BookParser.NoteContent
import com.aaronicsubstances.niv1984.utils.BookParser.NoteContentKind
import com.aaronicsubstances.niv1984.utils.BookParser.NoteKind
import com.aaronicsubstances.niv1984.utils.BookParser.NoteRef
import com.aaronicsubstances.niv1984.utils.BookParser.Verse
import com.aaronicsubstances.niv1984.utils.BookParser.WordsOfJesus

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test

import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookParserTest {
    @Test
    fun testWithEmpty() {
        runTest(::supplyEmptyTestParam)
    }

    @Test
    fun test1() {
        runTest(::supplyTestParam1)
    }

    @Test
    fun test2() {
        runTest(::supplyTestParam2)
    }

    @Test
    fun testKjvAndWordsOfJesus() {
        runTest(::supplyTestWithKjvAndWordsOfJesus)
    }

    @Test
    fun testEntityUsage() {
        runTest(::supplyTestWithEntityUsage)
    }

    @Test
    fun testMultipleChapters() {
        runTest(::supplyTestWithMultipleChapters)
    }

    @Test
    fun testEmptyContentAndBlockQuote() {
        runTest(::supplyTestWithEmptyContentAndBlockQuote)
    }

    private fun runTest(supplier: ((MutableList<Chapter>) -> String)) {
        val expectedResults = mutableListOf<Chapter>()
        val inputStream = supplier(expectedResults).byteInputStream()
        val instance = BookParser()
        val actualResults = instance.parse(inputStream)
        assertEquals(expectedResults, actualResults)
    }

    private fun supplyEmptyTestParam(book: MutableList<Chapter>): String {
        return """<book>
        </book>
        """
    }

    private fun supplyTestParam1(book: MutableList<Chapter>): String {
        val results = mutableListOf<Any>();
        book.add(Chapter(1, results))
        results.add(Verse(1,
            listOf(FancyContent(FancyContentKind.NONE,
                "In the beginning God created the heavens and the earth."))))
        results.add(Verse(2,
            listOf(FancyContent(FancyContentKind.NONE,
                "Now the earth was without form"),
                NoteRef(1),
                FancyContent(FancyContentKind.NONE,
                    " and void."))))
        results.add(Note(1, NoteKind.DEFAULT,
            listOf(NoteContent(NoteContentKind.REF_VERSE_START,
                "2"),
                NoteContent(NoteContentKind.NONE,"Or "),
                NoteContent(NoteContentKind.EM,
                    "possibly become"))))

        return """<book>
        <chapter num="1">
            <verse num="1">
                <content>In the beginning God created the heavens and the earth.</content>
            </verse>
            <verse num="2">
                <content>Now the earth was without form</content>
                <note_ref>1</note_ref>
                <content> and void.</content>
            </verse>
            <note num="1">
                <content kind="REF_VERSE_START">2</content>
                <content>Or </content>
                <content kind="em">possibly become</content>
            </note>
        </chapter>
        </book>"""
    }

    private fun supplyTestParam2(book: MutableList<Chapter>): String {
        val results = mutableListOf<Any>()
        book.add(Chapter(2, results))
        results.add(ChapterFragment(ChapterFragmentKind.HEADING,
            listOf(FancyContent(FancyContentKind.NONE,
                "Origins"))))
        results.add(Verse(1,
            listOf(FancyContent(FancyContentKind.NONE,
                "In the beginning God created the heavens and the earth."))))
        results.add(Verse(2,
            listOf(FancyContent(FancyContentKind.NONE,
                "Now the earth was without form"),
                NoteRef(1),
                FancyContent(FancyContentKind.NONE,
                    " and void."))))
        results.add(Note(0, NoteKind.CROSS_REFERENCES,
            listOf(NoteContent(NoteContentKind.REF_VERSE_START,
                "1"),
                NoteContent(NoteContentKind.NONE,"Heb 1:3"))))
        results.add(Note(1, NoteKind.DEFAULT,
            listOf(NoteContent(NoteContentKind.REF_VERSE_START,
                "2"),
                NoteContent(NoteContentKind.NONE,"Or "),
                NoteContent(NoteContentKind.EM,
                    "possibly become"))))

        return """<book>
        <chapter num="2">
            <fragment kind="heading">
                <content>Origins</content>
            </fragment>
            <verse num="1">
                <content>In the beginning God created the heavens and the earth.</content>
            </verse>
            <verse num="2">
                <content>Now the earth was without form</content>
                <note_ref>1</note_ref>
                <content> and void.</content>
            </verse>
            <note kind="CROSS_references">
                <content kind="REF_VERSE_START">1</content>
                <content>Heb 1:3</content>
            </note>
            <note num="1">
                <content kind="ref_verse_START">2</content>
                <content>Or </content>
                <content kind="em">possibly become</content>
            </note>
        </chapter></book>
        """
    }

    private fun supplyTestWithKjvAndWordsOfJesus(book: MutableList<Chapter>): String {
        val results = mutableListOf<Any>()
        book.add(Chapter(3, results))
        results.add(Verse(13,
            listOf(FancyContent(FancyContentKind.NONE,
                    "Jesus said to"),
                FancyContent(FancyContentKind.EM,
                    " the "),
                FancyContent(FancyContentKind.NONE,
                    "centurion: "),
                WordsOfJesus(listOf(FancyContent(FancyContentKind.NONE,
                    "Go your way. As you have believed, so be it done unto you."))))))

        results.add(Verse(14,
            listOf(FancyContent(FancyContentKind.NONE,
                "And his servant was healed in the selfsame hour."))))
        return """<book>
        <chapter num="3">
            <verse num="13">
                <content>Jesus said to</content>
                <content kind="em"> the </content>
                <content>centurion: </content>
                <wj>
                    <content>Go your way. As you have believed, so be it done unto you.</content>
                </wj>
            </verse>
            <verse num="14">
                <content>And his servant was healed in the selfsame hour.</content>
            </verse>
        </chapter></book>
        """
    }

    private fun supplyTestWithEntityUsage(book: MutableList<Chapter>): String {
        val results = mutableListOf<Any>()
        book.add(Chapter(4, results))
        run {
            val ei = '\u025B'
            val EI = '\u0190'
            val oh = '\u0254'
            val OH = '\u0186'
            results.add(
                ChapterFragment(
                    ChapterFragmentKind.NONE,
                    listOf(
                        FancyContent(FancyContentKind.SELAH, "test"),
                        FancyContent(FancyContentKind.STRONG_EM, "ing")
                    )
                )
            )
            results.add(
                Verse(
                    13,
                    listOf(
                        WordsOfJesus(
                            listOf(
                                FancyContent(
                                    FancyContentKind.NONE,
                                    "Na ${oh}fr${ei}${ei} ne nkoa mu du de mm${ei}nnaa"
                                ),
                                NoteRef(3),
                                FancyContent(
                                    FancyContentKind.NONE,
                                    "du maa w${oh}n; na ${oh}ka kyer${ei}${ei} w${oh}n s${ei}: "
                                ),
                                FancyContent(
                                    FancyContentKind.NONE,
                                    "M${oh}ny${ei} ho adwuma nk${oh}si s${ei} m${ei}ba."
                                )
                            )
                        )
                    )
                )
            )
            results.add(
                Note(
                    3, NoteKind.DEFAULT,
                    listOf(
                        NoteContent(NoteContentKind.REF_VERSE, "13"),
                        NoteContent(NoteContentKind.STRONG_EM, "MM${EI}NNAA"),
                        NoteContent(
                            NoteContentKind.NONE,
                            ": Na yei y${ei} ${oh}paani bosome mmi${ei}nsa akatua."
                        )
                    )
                )
            )
        }
        return run {
            val ei = "&#603;"
            val EI = "&#400;"
            val oh = "&#596;"
            val OH = "&#390;"
            """
            <book>
            <chapter num="4">
                <fragment>
                    <content kind="SELAH">test</content>
                    <content kind="strong_em">ing</content>
                </fragment>
                <verse num="13">
                    <wj>
                        <content>Na ${oh}fr${ei}${ei} ne nkoa mu du de mm${ei}nnaa</content>
                        <note_ref>3</note_ref>
                        <content>du maa w${oh}n; na ${oh}ka kyer${ei}${ei} w${oh}n s${ei}: </content>
                        <content>M${oh}ny${ei} ho adwuma nk${oh}si s${ei} m${ei}ba.</content>
                    </wj>
                </verse>
                <note num="3">
                    <content kind="REF_VERSE">13</content>
                    <content kind="strong_em">MM${EI}NNAA</content>
                    <content>: Na yei y${ei} ${oh}paani bosome mmi${ei}nsa akatua.</content>
                </note>
            </chapter>
            </book>
            """
        }
    }

    private fun supplyTestWithMultipleChapters(book: MutableList<Chapter>): String {
        book.add(Chapter(1, listOf(
            Verse(4, listOf(FancyContent(FancyContentKind.NONE, "yes")))
        )))
        book.add(Chapter(2, listOf(
            Verse(3, listOf(FancyContent(FancyContentKind.EM, "yes again")))
        )))
        return """<book>
            <chapter num="1">
                <verse num="4">
                    <content>yes</content>
                </verse>
            </chapter>
            <chapter num="2">
                <verse num="3">
                    <content kind="em">yes again</content>
                </verse>
            </chapter>
        </book>
        """
    }

    private fun supplyTestWithEmptyContentAndBlockQuote(book: MutableList<Chapter>): String {
        book.add(Chapter(17, listOf(
            Verse(21, listOf(FancyContent(FancyContentKind.NONE, "")))
        )))
        book.add(Chapter(25, listOf(
            Verse(20, listOf(
                BlockQuote(BlockQuoteKind.LEFT_INDENTED, listOf(
                    WordsOfJesus(listOf(FancyContent(FancyContentKind.NONE, "He"))),
                    FancyContent(FancyContentKind.NONE, " that is separated")))))
        )))
        return """<book>
            <chapter num="17">
                <verse num="21">
                    <content/>
                </verse>
            </chapter>
            <chapter num="25">
                <verse num="20">
                    <block_quote kind="left_indented">
                        <wj>
                            <content>He</content>
                        </wj>
                        <content> that is separated</content>
                    </block_quote>
                </verse>
            </chapter>
        </book>"""
    }
}