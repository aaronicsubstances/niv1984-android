@GrabConfig(systemClassLoader=true)
@Grab(group='xom', module='xom', version='1.3.5')
@Grab(group='org.xerial', module='sqlite-jdbc', version='3.31.1')
@GrabExclude('xml-apis:xml-apis')

import nu.xom.*

import groovy.sql.Sql

/*
std query:
exact term 
|
inexact term
|
phrase
|
near query

stuff to escape (treat all as ASCII chars other than alphanumeric)
-AND, OR, NOT, NEAR - use lower case to escape
-deal with parentheses
-unary NOT '-'
-phrase indicator '"'
-near query forward slash '/'
-inexact term '^' and '*'
-field indicator ':'
*/

/*
To avoid ambiguity of loosest precedence in standard or enhanced query syntax
- no use of NOT
- always use phrase query for AND operations.
- specify OR explicity all the time.
*/

chapterCounts = [
    50, 40, 27, 36, 34, 24, 21, 4, 31, 24, 22, 25, 29, 36, 10, 13, 10, 42, 150,
    31, 12, 8, 66, 52, 5, 48, 12, 14, 3, 9, 1, 4, 7, 3, 3, 3, 2, 14, 4,
    28, 16, 24, 21, 28, 16, 16, 13, 6, 6, 4, 4, 5, 3, 6, 4, 3, 1, 13,
    5, 5, 3, 5, 1, 1, 1, 22
]

testAssumptions()

scriptDir = new File(getClass().protectionDomain.codeSource.location.path).parentFile
        
Sql.withInstance("jdbc:sqlite:$scriptDir/../Niv1984-Plus/app/src/main/assets/search_data.db".replaceAll("\\\\", '/'), '', '', 'org.sqlite.JDBC') { sql ->
    // use 'sql' instance ...
    //args = ["SELECT snippet(bible_index_record) FROM bible_index_record WHERE content MATCH 'Nyankop\u0186n' LIMIT 9"] // verifies that capital twi Oh
                                                                                                                    // can still find small twi oh in index
    if (args) {
        def sqlQuery = args.join(" ")
        println "SQL query: $sqlQuery"
        def results = sql.rows(sqlQuery)
        results.each {
            println it
        }
        println "${results.size()} row(s) found"
        return
    }
    // use backticks to quote all identifiers for Android Room annotation processor to validate successfully
    sql.execute("""CREATE VIRTUAL TABLE bible_index_record USING FTS4(
        `bible_version` TEXT NOT NULL, 
        `book_number` INT NOT NULL, 
        `chapter_number` INT NOT NULL, 
        `verse_number` INT NOT NULL,
        `content` TEXT NOT NULL,
        notindexed=`bible_version`, notindexed=`book_number`,
        notindexed=`chapter_number`, notindexed=`verse_number`,
        tokenize=unicode61 `remove_diacritics=0`)
    """)
    
    populateDatabase(sql)
}

def testAssumptions() {
    assert "\u025B\u0190\u0254\u0186".toLowerCase() == "\u025B\u025B\u0254\u0254"
    assert "\u025B\u0190\u0254\u0186".toUpperCase() == "\u0190\u0190\u0186\u0186"
    assert normalizeContent("\u2018\u2019\u201B\u201C\u201D\u201E") == "'''\"\"\""
}

def populateDatabase(db) {
    def startTime = new Date().time
    def bibleVersions = [ "asante2012", "niv1984", "kjv1769" ]
    for (bibleVersion in bibleVersions) {
        println "Indexing $bibleVersion..."
        def bvStartTime = new Date().time
        for (bookNumber in 1..66) {
            println "Indexing $bibleVersion book $bookNumber..."
            indexVerses(db, bibleVersion, bookNumber)
        }
        final bibleVersionTimeTaken = (new Date().time - bvStartTime) / 1000.0
        println("Done indexing bible version ${bibleVersion} in ${bibleVersionTimeTaken} secs")
    }
    println "Optimizing..."
    db.execute("INSERT INTO bible_index_record(bible_index_record) VALUES('optimize')")
    final timeTaken = (new Date().time - startTime) / 1000.0
    println("Database population took ${timeTaken} secs")
}

def indexVerses(db, bibleVersion, bookNumber) {
    db.withTransaction { // solved hanging problem with db.withBatch
        db.withBatch('INSERT INTO bible_index_record (bible_version, book_number, chapter_number, verse_number, content) VALUES (?, ?, ?, ?, ?)') { ps ->
        
            def parser = new Builder()
            def assetPath = String.format("%s/%02d.xml", bibleVersion, bookNumber)
            def doc = parser.build(new File(scriptDir, "app/src/main/assets/$assetPath"))        
            def root = doc.rootElement
            assert root.localName == "book"
            for (chapter in root.getChildElements("chapter")) {
                def chapterNumber = chapter.getAttributeValue("num")
                //println "Indexing $bibleVersion book $bookNumber chapter $chapterNumber..."
                for (verse in chapter.getChildElements("verse")) {
                    def vNum = verse.getAttributeValue("num")
                    def allVerseText = grabRelevantText(verse)
                    def sqlParams = [bibleVersion, bookNumber, chapterNumber, vNum, allVerseText]
                    ps.addBatch(sqlParams)
                }
                for (note in chapter.getChildElements("note")) {
                    def kind = note.getAttributeValue("kind")
                    if (kind ==~ /(?i)CROSS_REFERENCES/){
                        continue
                    }
                    def allNoteText = grabRelevantText(note)
                    def sqlParams = [bibleVersion, bookNumber, chapterNumber, 0, allNoteText]
                    ps.addBatch(sqlParams)
                }
            }
        }
    }
}

def grabRelevantText(el) {
    // first remove irrelevant elements
    removeIrrelevantElements(el)
    return normalizeContent(el.value)
}

def removeIrrelevantElements(el) {
    def toRemove = []
    for (c in el.getChildElements()) {
        if (c.localName == "note_ref") {
            toRemove << c
        }
        else {
            def kind = c.getAttributeValue("kind")
            if (kind ==~ /(?i)REF_VERSE_START/) {
                toRemove << c
            }
        }
    }
    toRemove.each {
        el.removeChild(it)
    }
    for (c in el.getChildElements()) {
        removeIrrelevantElements(c)
    }
}

def normalizeContent(c) {
    def normalized = c

    // remove unnecessary whitespace to reduce size of index.
    normalized = normalized.trim().replaceAll("\\s+", " ")

    // replace twi non-English alphabets with english ones.
    normalized = normalized.tr("\u2018\u2019\u201B\u201C\u201D\u201E", "'''\"\"\"")
    
    return normalized
}