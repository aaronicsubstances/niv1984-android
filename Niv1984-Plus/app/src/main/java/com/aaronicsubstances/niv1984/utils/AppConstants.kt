package com.aaronicsubstances.niv1984.utils;

object AppConstants {
    const val BIBLE_BOOK_COUNT = 66
    val DEFAULT_BIBLE_VERSIONS = listOf(NivBibleVersion.code, KjvBibleVersion.code)

    val BIBLE_BOOK_CHAPTER_COUNT = listOf(
        50, 40, 27, 36, 34, 24, 21, 4, 31, 24, 22, 25, 29, 36, 10, 13, 10, 42, 150,
        31, 12, 8, 66, 52, 5, 48, 12, 14, 3, 9, 1, 4, 7, 3, 3, 3, 2, 14, 4,
        28, 16, 24, 21, 28, 16, 16, 13, 6, 6, 4, 4, 5, 3, 6, 4, 3, 1, 13,
        5, 5, 3, 5, 1, 1, 1, 22)

    val bibleVersions: Map<String, BibleVersion>
        get() {
            return mapOf(AsanteTwiBibleVersion2015.code to AsanteTwiBibleVersion2015,
                KjvBibleVersion.code to KjvBibleVersion,
                NivBibleVersion.code to NivBibleVersion)
        }
}

interface BibleVersion {
    fun getChapterTitle(bookNumber: Int, chapterNumber: Int): String
    fun isAsanteTwiBibleVersion(): Boolean

    val code: String
    val description: String
    val abbreviation: String
    val strFootnote: String
    val bookNames: List<String>
}

object AsanteTwiBibleVersion2015: BibleVersion {
    override fun getChapterTitle(bookNumber: Int, chapterNumber: Int): String {
        if (bookNumber == 19) {
            return "Nnwom $chapterNumber"
        }
        else {
            return "Ti $chapterNumber"
        }
    }

    override fun isAsanteTwiBibleVersion() = true

    override val code = "asw2015"
    override val description = "Twer\u025B Kronkron (Asante Twi Bible, 2015)"
    override val abbreviation = "ASW"
    override val strFootnote = "Footnote"
    override val bookNames = listOf(
        "Gyenesis", "Eksod\u0254s", "Lewitik\u0254s", "Numeri", "Deuteronomium",
        "Yosua", "Akannifo\u0254", "Rut",
        "(1 Sa) Samuel nwoma a edi kan",
        "(2 Sa) Samuel nwoma a \u025Bt\u0254 so mmienu",
        "(1 Ah) Ahemfo nwoma a edi kan",
        "(2 Ah) Ahemfo nwoma a \u025Bt\u0254 so mmienu",
        "(1 Be) Beresos\u025Bm nwoma a edi kan",
        "(2 Be) Beresos\u025Bm nwoma a \u025Bt\u0254 so mmienu", "Esra",
        "Nehemia", "Ester", "Hiob", "Nnwom", "Mmebus\u025Bm",
        "\u0186s\u025Bnkafo\u0254", "Nnwom mu dwom", "Yesaia", "Yeremia", "Kwadwom",
        "Hesekiel", "Daniel", "Hosea", "Yoel", "Amos", "Obadia", "Yona",
        "Mika", "Nahum", "Habakuk", "Sefania", "Hagai", "Sakaria", "Malaki",
        "Mateo As\u025Bmpa", "Marko As\u025Bmpa", "Luka As\u025Bmpa", "Yohane As\u025Bmpa",
        "Asomafo\u0254 no Nnwuma", "Romanfo\u0254 nwoma",
        "(1 Ko) Korintofo\u0254 nwoma a edi kan",
        "(2 Ko) Korintofo\u0254 nwoma a \u025Bt\u0254 so mmienu",
        "Galatifo\u0254 nwoma", "Efesofo\u0254 nwoma",
        "Filipifo\u0254 nwoma", "Kolosefo\u0254 nwoma",
        "(1 Te) Tesalonikafo\u0254 nwoma a edi kan",
        "(2 Te) Tesalonikafo\u0254 nwoma a \u025Bt\u0254 so mmienu",
        "(1 Ti) Timoteo nwoma a edi kan",
        "(2 Ti) Timoteo nwoma a \u025Bt\u0254 so mmienu",
        "Tito nwoma", "Filemon nwoma", "Hebrifo\u0254 nwoma", "Yakobo nwoma",
        "(1 Pe) Petro nwoma a edi kan",
        "(2 Pe) Petro nwoma a \u025Bt\u0254 so mmienu",
        "(1 Yo) Yohane nwoma a edi kan",
        "(2 Yo) Yohane nwoma a \u025Bt\u0254 so mmienu",
        "(3 Yo) Yohane nwoma a \u025Bt\u0254 so mmi\u025Bnsa",
        "Yuda", "Yohane Adiyis\u025Bm")
}

object KjvBibleVersion: BibleVersion {
    override fun getChapterTitle(bookNumber: Int, chapterNumber: Int): String {
        if (bookNumber == 19) {
            return "Psalm $chapterNumber"
        }
        else {
            return "Chapter $chapterNumber"
        }
    }

    override fun isAsanteTwiBibleVersion() = false

    override val code = "kjv1769"
    override val description = "King James Bible (1769)"
    override val abbreviation = "KJV"
    override val strFootnote = "Footnote"
    override val bookNames = listOf(
        "Genesis", "Exodus", "Leviticus", "Numbers", "Deuteronomy",
        "Joshua", "Judges", "Ruth", "1 Samuel", "2 Samuel",
        "1 Kings", "2 Kings", "1 Chronicles", "2 Chronicles", "Ezra",
        "Nehemiah", "Esther", "Job", "Psalms", "Proverbs",
        "Ecclesiastes", "Song of Solomon", "Isaiah", "Jeremiah", "Lamentations",
        "Ezekiel", "Daniel", "Hosea", "Joel", "Amos", "Obadiah", "Jonah",
        "Micah", "Nahum", "Habakkuk", "Zephaniah", "Haggai", "Zechariah", "Malachi",
        "Matthew", "Mark", "Luke", "John", "Acts", "Romans", "1 Corinthians",
        "2 Corinthians", "Galatians", "Ephesians", "Philippians", "Colossians",
        "1 Thessalonians", "2 Thessalonians", "1 Timothy", "2 Timothy",
        "Titus", "Philemon", "Hebrews", "James", "1 Peter", "2 Peter",
        "1 John", "2 John", "3 John", "Jude", "Revelation")
}

object NivBibleVersion: BibleVersion {
    override fun getChapterTitle(bookNumber: Int, chapterNumber: Int): String {
        if (bookNumber == 19) {
            return "Psalm $chapterNumber"
        }
        else {
            return "Chapter $chapterNumber"
        }
    }

    override fun isAsanteTwiBibleVersion() = false

    override val code = "niv1984"
    override val description = "New International Version (1984)"
    override val abbreviation = "NIV"
    override val strFootnote = "Footnote"
    override val bookNames = listOf(
        "Genesis", "Exodus", "Leviticus", "Numbers", "Deuteronomy",
        "Joshua", "Judges", "Ruth", "1 Samuel", "2 Samuel",
        "1 Kings", "2 Kings", "1 Chronicles", "2 Chronicles", "Ezra",
        "Nehemiah", "Esther", "Job", "Psalms", "Proverbs",
        "Ecclesiastes", "Song of Songs", "Isaiah", "Jeremiah", "Lamentations",
        "Ezekiel", "Daniel", "Hosea", "Joel", "Amos", "Obadiah", "Jonah",
        "Micah", "Nahum", "Habakkuk", "Zephaniah", "Haggai", "Zechariah", "Malachi",
        "Matthew", "Mark", "Luke", "John", "Acts", "Romans", "1 Corinthians",
        "2 Corinthians", "Galatians", "Ephesians", "Philippians", "Colossians",
        "1 Thessalonians", "2 Thessalonians", "1 Timothy", "2 Timothy",
        "Titus", "Philemon", "Hebrews", "James", "1 Peter", "2 Peter",
        "1 John", "2 John", "3 John", "Jude", "Revelation")
}
