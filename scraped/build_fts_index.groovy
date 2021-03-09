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
def appDb = "../Niv1984-Plus/app/src/main/assets/search_data.db"
//appDb = "test_search_data.db"
Sql.withInstance("jdbc:sqlite:$scriptDir/$appDb".replaceAll("\\\\", '/'), '', '', 'org.sqlite.JDBC') { sql ->
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
    
    def multiset = [:]
    assert getStringOutOfMultiset(multiset) == "0 words, 0 instances"
    multiset["the"] = 1
    assert getStringOutOfMultiset(multiset) == "1 words, 1 instances\r\nthe:1:100.00%"
    multiset["the"]++
    assert getStringOutOfMultiset(multiset) == "1 words, 2 instances\r\nthe:2:100.00%"
    multiset["she"] = 1 
    assert getStringOutOfMultiset(multiset) == "2 words, 3 instances\r\nthe:2:66.67%\r\nshe:1:33.33%"
    multiset["she"] = 2 
    assert getStringOutOfMultiset(multiset) == "2 words, 4 instances\r\nshe:2:50.00%\r\nthe:2:50.00%"
    multiset["she"]++ 
    assert getStringOutOfMultiset(multiset) == "2 words, 5 instances\r\nshe:3:60.00%\r\nthe:2:40.00%"
    multiset["yhe"] = 1 
    assert getStringOutOfMultiset(multiset) == "3 words, 6 instances\r\nshe:3:50.00%\r\nthe:2:33.33%\r\nyhe:1:16.67%"
    multiset["yhe"]++ 
    assert getStringOutOfMultiset(multiset) == "3 words, 7 instances\r\nshe:3:42.86%\r\nthe:2:28.57%\r\nyhe:2:28.57%"
    multiset["yhe"]++ 
    assert getStringOutOfMultiset(multiset) == "3 words, 8 instances\r\nshe:3:37.50%\r\nyhe:3:37.50%\r\nthe:2:25.00%"
    multiset["yhe"]++ 
    assert getStringOutOfMultiset(multiset) == "3 words, 9 instances\r\nyhe:4:44.44%\r\nshe:3:33.33%\r\nthe:2:22.22%"
    
    assert splitUserQuery("") == []
    assert splitUserQuery("  -: () ") == []
    assert splitUserQuery("a") == [ 'a' ]
    assert splitUserQuery(" a  - b") == [ 'a', 'b' ]
    assert splitUserQuery(" a 1  - cb") == [ 'a', '1', 'cb' ]
    
    multiset.clear()
    addToMultiset(multiset, "")
    assert multiset == [:]
    addToMultiset(multiset, " a b")
    assert multiset == [a:1, b:1]
    addToMultiset(multiset, " a b c")
    assert multiset == [a:2, b:2, c:1]
}

def populateDatabase(db) {
    def startTime = new Date().time
    def bibleVersions = [ "asw2015", "niv1984", "kjv1769" ]
    for (bibleVersion in bibleVersions) {
        println "Indexing $bibleVersion..."
        def bvStartTime = new Date().time
        for (bookNumber in 1..66) {
            println "Indexing $bibleVersion book $bookNumber..."
            indexVerses(db, bibleVersion, bookNumber)
            //break
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
    //def multiset = [:]
    db.withTransaction { // solved hanging problem with db.withBatch
        db.withBatch('INSERT INTO bible_index_record (bible_version, book_number, chapter_number, verse_number, content) VALUES (?, ?, ?, ?, ?)') { ps ->
        
            def parser = new Builder()
            def assetPath = String.format("%s/%02d.xml", bibleVersion, bookNumber)
            def doc = parser.build(new File("$scriptDir/../Niv1984-Plus", "app/src/main/assets/$assetPath"))        
            def root = doc.rootElement
            assert root.localName == "book"
            for (chapter in root.getChildElements("chapter")) {
                def chapterNumber = chapter.getAttributeValue("num")
                //println "Indexing $bibleVersion book $bookNumber chapter $chapterNumber..."
                for (verse in chapter.getChildElements("verse")) {
                    def vNum = verse.getAttributeValue("num")
                    def allVerseText = grabRelevantText(verse)
                    //addToMultiset(multiset, allVerseText)
                    def sqlParams = [bibleVersion, bookNumber, chapterNumber, vNum, allVerseText]
                    ps.addBatch(sqlParams)
                }
                for (note in chapter.getChildElements("note")) {
                    def kind = note.getAttributeValue("kind")
                    if (kind ==~ /(?i)CROSS_REFERENCES/){
                        continue
                    }
                    def allNoteText = grabRelevantText(note)
                    //addToMultiset(multiset, allNoteText)
                    def sqlParams = [bibleVersion, bookNumber, chapterNumber, 0, allNoteText]
                    ps.addBatch(sqlParams)
                }
            }
        }
    }
    //new File("$bibleVersion-${bookNumber}.txt").bytes = getStringOutOfMultiset(multiset).getBytes("utf-8")
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

def addToMultiset(multiset, s) {
    // split by letters.
    def terms = splitUserQuery(s)
    for (t in terms) {
        if (!multiset.containsKey(t)) {
            multiset[t] = 0
        }
        multiset[t]++
    }
}

def splitUserQuery(rawUserQuery) {
    // Replace all chars which are neither letters nor digits with space.
    def processed = new StringBuilder()
    for (i in 0..<rawUserQuery.size()) {
        def c = rawUserQuery[i] as char
        if (Character.isLetterOrDigit(c)) {
            processed.append(c)
        } else {
            processed.append(" ")
        }
    }
    def terms = processed.toString().split(" ", -1).findAll { !it.isEmpty() }
    assert terms instanceof List
    return terms
}

def getStringOutOfMultiset(multiset) {
    def totalCount = multiset.values().sum()
    if (!totalCount) totalCount = 0
    def text = "${multiset.size()} words, $totalCount instances\r\n"
    text += multiset.entrySet().sort(false){ a,b -> 
        // sort reverse by frequency.
        def cmp1 = Integer.compare(a.value, b.value)
        if (cmp1 != 0) return -cmp1
        // then ascending by word itself.
        return a.key.toLowerCase().compareTo(b.key.toLowerCase())
    }
    .collect { 
        "${it.key}:${it.value}:" + String.format("%.2f%%", it.value*100.0/(totalCount?totalCount:1)) 
    }
    .join("\r\n")
    return text.trim()
}