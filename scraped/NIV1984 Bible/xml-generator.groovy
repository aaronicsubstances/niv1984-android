@Grab(group='org.jsoup', module='jsoup', version='1.13.1')
@Grab(group='xom', module='xom', version='1.3.5')

import groovy.io.FileType

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;

class XmlGenerator {
    static final TAG_BOOK = "book"
    static final TAG_CHAPTER = "chapter"
    static final TAG_VERSE = "verse"
    static final TAG_CHAPTER_FRAGMENT = "fragment"
    static final TAG_ELEMENT_CONTENT = "content"
    static final TAG_WJ = "wj"
    static final ATTR_KIND = "kind"
    static final ATTR_NUMBER = "num"

    static final TAG_NOTE = "note"
    static final TAG_NOTE_REF = "note_ref"        

    enum ChapterFragmentKind {
        NONE, HEADING
    }

    enum NoteKind {
        DEFAULT, CROSS_REFERENCES
    }

    enum NoteContentKind {
        NONE, EM, STRONG_EM,
        REF_VERSE_START, REF_VERSE
    }

    enum FancyContentKind {
        NONE, EM, STRONG_EM, SELAH, PICTOGRAM
    }

    static final BIBLE_BOOK_CHAPTER_COUNT = [
        50, 40, 27, 36, 34, 24, 21, 4, 31, 24, 22, 25, 29, 36, 10, 13, 10, 42, 150,
        31, 12, 8, 66, 52, 5, 48, 12, 14, 3, 9, 1, 4, 7, 3, 3, 3, 2, 14, 4,
        28, 16, 24, 21, 28, 16, 16, 13, 6, 6, 4, 4, 5, 3, 6, 4, 3, 1, 13,
        5, 5, 3, 5, 1, 1, 1, 22]

	public static void main(String[] args) {
        new File(".").list().findAll {
            new File(it).isDirectory()
        }.sort(false).eachWithIndex { n, i ->
            def destFile = new File(String.format("%02d.xml", i+1))
            println "Generating ${destFile}..."
            def root = new nu.xom.Element(TAG_BOOK)
            def chapterCount =  BIBLE_BOOK_CHAPTER_COUNT[i]
            1.upto(chapterCount) { cn ->
                def chapterEl = processChapter(n, cn)
                root.appendChild(chapterEl)
            }
            def doc = new nu.xom.Document(root)
            serializeXml(destFile, doc)
        }
	}
    
    static void serializeXml(destFile, doc) {
        destFile.withOutputStream {
            def serializer = new nu.xom.Serializer(it, "UTF-8")
            serializer.setIndent(4)
            serializer.write(doc)
        }      
    }
    
    static processChapter(bookDir, cn) {
        def root = new nu.xom.Element(TAG_CHAPTER)
        def numAttr = new nu.xom.Attribute(ATTR_NUMBER, String.valueOf(cn))
        root.addAttribute(numAttr)
        
        def cf = new File(bookDir, String.format("%03d.html", cn)) 
        println "parsing $cf..."
        def doc = Jsoup.parse(cf, null);
        def title = doc.select("b")get(0).text()
        def chapterElems = doc.select("dl")
        def chapterElem
        if (title.contains('70')) { // Psalm 70
            assert chapterElems.size() == 2
            chapterElem = chapterElems.get(1)
        }
        else {
            assert chapterElems.size() == 1
            chapterElem = chapterElems.get(0)
        }
        
        // normal scenario is:
        // - footnotes present.
        // - dt is verse number, dd is verse text.
        // - no verse jumps or duplicates.
        // - verse can only have as child element a footnote reference.
        //
        
        def footnoteMap = buildFootnoteMap(doc.select("ol")[0])
        
        def verseElems = chapterElem.children()
        def chapterTitleProcessed = false;
        def skipVerseCount = 0
        def jer39v1TitleDd = null
        for (int i = 0; i < verseElems.size(); i+=2) {
            def dt = verseElems[i]
            assert dt.tagName() == 'dt'
            def dd = verseElems[i + 1]
            assert dd.tagName() == 'dd'
            if (!chapterTitleProcessed) {
                chapterTitleProcessed = true
                if (title.contains('119')) {
                
                    processPsalm119Title(root, dd, footnoteMap, title)
                    
                    skipVerseCount++
                    continue
                }
                else if (title.contains('Psalm') && !title.endsWith(" 1")) {
                    if (dd.html().substring(title.size()).trim()) {
                        processTitleText(root, dd, footnoteMap, title)
                    }
                    skipVerseCount++
                    continue
                }
                else if (title.contains('Jeremiah') && title.endsWith(" 39")) {
                    // move this title text to be part of verse 1.
                    jer39v1TitleDd = dd
                    skipVerseCount++
                    continue
                }
            }
            def expectedVerseNum = i.intdiv(2) + 1 - skipVerseCount
            def omittedCount = dealWithOmittedVerses(title, expectedVerseNum)
            if (omittedCount > 0) {
                1.upto(omittedCount) {
                    def expVerseEl = new nu.xom.Element(TAG_VERSE)
                    root.appendChild(expVerseEl)
                    expVerseEl.addAttribute(new nu.xom.Attribute(ATTR_NUMBER, "${expectedVerseNum}"))
                    expVerseEl.appendChild(createOutContentEl("", null))
                    expectedVerseNum++
                    skipVerseCount--
                }
            }
            assert dt.text() == "$expectedVerseNum"
            if (title.contains('119') && expectedVerseNum % 8 == 0 && expectedVerseNum < 170) {
                processPsalm119LastStanzaVerse(root, dd, footnoteMap, expectedVerseNum)
            }
            else if (title.contains('Jeremiah') && title.endsWith(" 39") && expectedVerseNum == 1) {
                processJeremiah39Verse1(root, jer39v1TitleDd, dd, footnoteMap)
            }
            else {
                processVerseText(root, dd, footnoteMap, expectedVerseNum)
            }
        }
        
        footnoteMap.values().each {
            root.appendChild(it)
        }
        return root
    }
    
    static psa119Titles = [
        'Aleph' : 1488, 
        'Beth' : 1489,
        'Gimel': 1490,
        'Daleth': 1491,
        'He': 1492,
        'Waw': 1493,
        'Zayin': 1494,
        'Heth': 1495,
        'Teth': 1496,
        'Yodh': 1497,
        'Kaph': 1499,
        'Lamedh': 1500,
        'Mem': 1502,
        'Nun': 1504,
        'Samekh': 1505, 
        'Ayin': 1506,
        'Pe': 1508,
        'Tsadhe': 1510, 
        'Qoph': 1511,
        'Resh': 1512,
        'Sin and Shin': 1513, 
        'Taw': 1514
    ]
    
    static final LTR_MARKER = "\u200e"
    
    /*
        deal with omitted verses:
        Matthew 17:21, 18:11, 23:14, 
        Mark 7:16, 9:44, 46, 11:26, 15:28
        Luke 17:36, 23:17
        John 5:4
        Acts 8:37, 15:34, 24:7, 28:29
        Romans 16:24
    */
    static dealWithOmittedVerses(title, vNum) {
        if (title.contains("Matthew") && 
                ((title.endsWith(" 17") && vNum == 21) ||
                (title.endsWith(" 18") && vNum == 11) ||
                (title.endsWith(" 23") && vNum == 14))) {
            return 1
        }
        if (title.contains("Mark") && 
                ((title.endsWith(" 7") && vNum == 16) ||
                (title.endsWith(" 9") && (vNum == 44 || vNum == 46)) ||
                (title.endsWith(" 11") && vNum == 26) ||
                (title.endsWith(" 15") && vNum == 28))) {
            return 1
        }
        if (title.contains("Luke") && 
                ((title.endsWith(" 17") && vNum == 36) ||
                (title.endsWith(" 23") && vNum == 17))){
            return 1
        }
        // present in scraped html
        /*if (title.contains("John") && 
                title.endsWith(" 5") && vNum == 4) {
            return 1
        }*/
        if (title.contains("Acts") && 
                ((title.endsWith(" 8") && vNum == 37) ||
                (title.endsWith(" 15") && vNum == 34) ||
                (title.endsWith(" 24") && vNum == 7) ||
                (title.endsWith(" 28") && vNum == 29))) {
            return 1
        }
        if (title.contains("Romans") && 
                title.endsWith(" 16") && vNum == 24) {
            return 1
        }
        return 0
    }
    
    static processPsalm119Title(root, dd, footnoteMap, title) {
        def first = true;
        def alephSeen = false;
        def fragEl = new nu.xom.Element(TAG_CHAPTER_FRAGMENT)
        root.appendChild(fragEl)
        final heading1 = "Aleph"
        for (ddChild in dd.childNodes()) {
            assert !alephSeen
            if (ddChild instanceof TextNode) {
                String remainder = getText(ddChild)
                if (first) {
                    remainder = remainder.substring(title.size())
                }
                int alephIndex = remainder.indexOf(heading1)
                if (alephIndex != -1) {
                    alephSeen = true;
                    remainder = remainder.substring(0, alephIndex)
                }
                fragEl.appendChild(createOutContentEl(remainder, FancyContentKind.EM))
            }
            else {
                processNoteRef(fragEl, ddChild, footnoteMap)
            }
            first = false
        }
        
        assert alephSeen
        
        def headingEl = new nu.xom.Element(TAG_CHAPTER_FRAGMENT)
        root.appendChild(headingEl)
        headingEl.addAttribute(new nu.xom.Attribute(ATTR_KIND, "${ChapterFragmentKind.HEADING}"))
        headingEl.appendChild(createOutContentEl(String.format("${LTR_MARKER}%c", psa119Titles[heading1]), FancyContentKind.PICTOGRAM))
        headingEl.appendChild(createOutContentEl(" $heading1", null))
    }
    
    static processPsalm119LastStanzaVerse(root, dd, footnoteMap, vNum) {
        def verseEl = new nu.xom.Element(TAG_VERSE)
        root.appendChild(verseEl)
        verseEl.addAttribute(new nu.xom.Attribute(ATTR_NUMBER, "$vNum"))
        
        def alephSeen = false;
        def heading1 = psa119Titles.values().sort()[vNum.intdiv(8)]
        heading1 = psa119Titles.keySet().find { psa119Titles[it] == heading1 }
        assert heading1
        for (ddChild in dd.childNodes()) {
            if (ddChild instanceof TextNode) {
                String remainder = getText(ddChild)
                int alephIndex = remainder.indexOf(heading1)
                if (alephIndex != -1) {
                    alephSeen = true;
                    remainder = remainder.substring(0, alephIndex)
                }
                verseEl.appendChild(createOutContentEl(remainder, null))
            }
            else {
                processNoteRef(verseEl, ddChild, footnoteMap)
            }
        }
        
        assert alephSeen
        
        def headingEl = new nu.xom.Element(TAG_CHAPTER_FRAGMENT)
        root.appendChild(headingEl)
        headingEl.addAttribute(new nu.xom.Attribute(ATTR_KIND, "${ChapterFragmentKind.HEADING}"))
        headingEl.appendChild(createOutContentEl(String.format("${LTR_MARKER}%c", psa119Titles[heading1]), FancyContentKind.PICTOGRAM))
        headingEl.appendChild(createOutContentEl(" $heading1", null))
    }
    
    static processTitleText(root, dd, footnoteMap, removeFromFirst) {
        def first = true;
        
        def fragEl = new nu.xom.Element(TAG_CHAPTER_FRAGMENT)
        root.appendChild(fragEl)
        
        for (ddChild in dd.childNodes()) {
            if (ddChild instanceof TextNode) {
                String remainder = getText(ddChild)
                if (first && removeFromFirst) {
                    remainder = remainder.substring(removeFromFirst.size())
                }
                fragEl.appendChild(createOutContentEl(remainder, FancyContentKind.EM))
            }
            else {
                processNoteRef(fragEl, ddChild, footnoteMap)
            }
            first = false
        }
    }
    
    static processVerseText(root, dd, footnoteMap, vNum) {     
        def verseEl = new nu.xom.Element(TAG_VERSE)
        root.appendChild(verseEl)
        verseEl.addAttribute(new nu.xom.Attribute(ATTR_NUMBER, "$vNum"))
        for (ddChild in dd.childNodes()) {
            if (ddChild instanceof TextNode) {
                verseEl.appendChild(createOutContentEl(ddChild, null))
            }
            else {
                processNoteRef(verseEl, ddChild, footnoteMap)
            }
        }
    }
    
    static processJeremiah39Verse1(root, jer39v1TitleDd, verseDd, footnoteMap) {
        def verseEl = new nu.xom.Element(TAG_VERSE)
        root.appendChild(verseEl)
        verseEl.addAttribute(new nu.xom.Attribute(ATTR_NUMBER, "1"))
        // insert space between title and verse.
        [jer39v1TitleDd, null, verseDd].each { dd-> 
            if (dd == null) {
                verseEl.appendChild(createOutContentEl(" ", null))
                return
            }
            for (ddChild in dd.childNodes()) {
                if (ddChild instanceof TextNode) {
                    verseEl.appendChild(createOutContentEl(ddChild, null))
                }
                else {
                    processNoteRef(verseEl, ddChild, footnoteMap)
                }
            }
        }
    }
    
    static processNoteRef(root, ddChild, footnoteMap) {
        assert ddChild.tagName() == 'sup'
        def aChild = ddChild.children()[0]
        def fKey = aChild.attr('href')
        assert footnoteMap.containsKey(fKey)
        def footnote = footnoteMap[fKey]
        
        def noteRefEl = new nu.xom.Element(TAG_NOTE_REF)
        root.appendChild(noteRefEl)
        noteRefEl.appendChild(footnote.getAttributeValue(ATTR_NUMBER))
    }
    
    static buildFootnoteMap(olWrapper) {
        def map = new LinkedHashMap() // preserve order
        def liIndex = -1
        for (li in olWrapper.children()) {
            liIndex++
            assert li.children().size() == 1
            def aChild = li.children()[0]
            def mapKey = '#' + aChild.attr('name')
            boolean first = true
            def noteEl = new nu.xom.Element(TAG_NOTE)
            noteEl.addAttribute(new nu.xom.Attribute(ATTR_NUMBER, "${liIndex + 1}"))
            for (n in aChild.childNodes()) {
                assert n instanceof TextNode || n.tagName() == 'i'
                def t = getText(n)
                def m
                if (!first || n instanceof TextNode) {
                    m = t =~ /\[(\d+([\-,]\d+)?)\]/
                }
                else {
                    m = t =~ /\d+([\-,]\d+)?/
                }
                if (!first) {
                    int start = 0
                    while (m.find(start)) {
                        noteEl.appendChild(createOutContentEl(t.substring(start, m.start()), (n instanceof Element) ? NoteContentKind.EM : null))
                        noteEl.appendChild(createOutContentEl(m.group(1), NoteContentKind.REF_VERSE))
                        start = m.end()
                    }
                    if (start < t.size()) {
                        noteEl.appendChild(createOutContentEl(t.substring(start), (n instanceof Element) ? NoteContentKind.EM : null))
                    }
                }
                else {
                    if (m) {
                        // normalize.
                        if (n instanceof Element) {
                            assert m.group() == t
                            noteEl.appendChild(createOutContentEl(t, NoteContentKind.REF_VERSE_START))
                        }
                        else {
                            assert m.start() == 0;
                            noteEl.appendChild(createOutContentEl(m.group(1), NoteContentKind.REF_VERSE_START))
                            noteEl.appendChild(createOutContentEl(t.substring(m.group().size()), null))
                        }
                    }
                    else {
                        assert n instanceof TextNode
                        if (t.startsWith("[")) {
                            noteEl.appendChild(createOutContentEl(t.substring(1), null))
                        }
                        else {
                            noteEl.appendChild(createOutContentEl(t, null))
                        }
                    }
                }
                first = false
            }
            assert noteEl.childCount
            map[mapKey] = noteEl
        }
        return map
    }
    
    static assertArrayEquals(a, b) {
        int minLen = Math.min(a.size(), b.size())
        for (int i = 0; i < minLen; i++) {
            assert a[i] == b[i]
        }
        assert a.size() == b.size()
    }
    
    static getText(elem) {
        if (elem instanceof String || elem instanceof GString) {
            return elem
        }
        else if (elem instanceof TextNode) {
            return elem.wholeText
        }
        else {
            return elem.wholeText()
        }
    }
    
    static createOutContentEl(elem, type = null) {
        def el = new nu.xom.Element(TAG_ELEMENT_CONTENT)
        if (type) {
            el.addAttribute(new nu.xom.Attribute(ATTR_KIND, "$type"))
        }
        def elemText = getText(elem)
        // remove Ctrl-Z - ASCII code 26
        elemText = elemText.replaceFirst("\u001a", "")
        // replace -- with long dash?
        //elemText = elemText.replace("--", "\u2014")
        el.appendChild(elemText)
        return el
    }
}