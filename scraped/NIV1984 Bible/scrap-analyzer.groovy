@Grab(group='org.jsoup', module='jsoup', version='1.13.1')

import groovy.io.FileType

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;

/**
 * label: leaf
 * content: leaf
 * fr: leaf
 * fk: leaf
 * ft: leaf
 *
 * {it, qs}: one content
 * note: one label, one kind, one body
 * wj: one or more of {content, note f}
 *
 * T:V newline format for leaf, T: only for nonleaf, and 4 special tags for verse start, verse end, end of wj and literal newline
 
 // encode special chars as numbered html entities, ie any char outside of 32 to 126 incl, plus & and #.
 */
class ScrapAnalyzer {
    static TAG_CONTENT = "content";
    static TAG_NOTE_BODY = "note_body";
    
    static TAG_FANCY_CONTENT = "fancy_content";
    static TAG_FANCY_NOTE_BODY = "fancy_note_body";
    
    static TAG_NOTE_START = "note_start";
    static TAG_NOTE_END = "note_end";
    static TAG_CHAP_NOTE_START = "cnote_start";
    static TAG_CHAP_NOTE_END = "cnote_end";
    static TAG_WJ_START = "wj_start";
    static TAG_WJ_END = "wj_end";
    static TAG_VERSE_START = "v_start";
    static TAG_VERSE_END = "v_end";
    
    static FANCY_CONTENT_TYPE_EM = "em"
    static FANCY_CONTENT_TYPE_SELAH = "selah"
    
    static FANCY_NOTE_BODY_TYPE_EM = "em"
    static FANCY_NOTE_BODY_TYPE_STRONG_EM = "strong_em"
    static FANCY_NOTE_BODY_TYPE_START_REF_VERSE = "ref_verse_start"
    static FANCY_NOTE_BODY_TYPE_OTHER_REF_VERSE = "ref_verse"
    
    static NOTE_KIND_HEADING = "heading"
    static NOTE_KIND_DIRECT_REF = "dref"
    static NOTE_KIND_OTHER = "other"
    
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

	public static void main(String[] args) {
        //verifyFiles()
        assert quoteValue('a') == 'a'
        assert quoteValue('1') == '1'
        assert quoteValue('16*') == '16*'
        assert quoteValue(' a') == ' a'
        assert quoteValue('\na') == '&#10;a'
        assert quoteValue('\ra#') == '&#13;a&#35;'
        assert quoteValue('\r\na&') == '&#13;&#10;a&#38;'
        transformFiles()
	}
    
    static verifyFiles() {
        new File(".").eachFileRecurse(FileType.FILES) {
            if (!it.name.endsWith(".html")) {
                return
            }
            println "parsing $it..."
            def doc = Jsoup.parse(it, null);
            def titleElems = doc.select("b");
            assert titleElems.size() == 1;
            def title = titleElems.get(0).text()
            def chapterElems = doc.select("dl")
            def chapterElem
            if (title.contains('70')) {
                assert chapterElems.size() == 2
                chapterElem = chapterElems.get(1)
            }
            else {
                assert chapterElems.size() == 1
                chapterElem = chapterElems.get(0)
            }
            def earliestDds = []
            for (dd in chapterElem.select("dd")) {
                if (earliestDds.size() < 2) {
                    earliestDds << dd
                }
                for (sup in dd.children()) {
                    assert sup.tagName() == 'sup'
                    def supChild = sup.children()
                    assert supChild.size() == 1
                    supChild = supChild[0]
                    assert supChild.attr('href').startsWith("#footnote_")
                    assert sup.html() == "[${supChild.outerHtml()}]"
                }
            }
            
            def dts = chapterElem.select("dt")
            assert dts.every { !it.children() && it.text() ==~ /\d+/ }
            def firstVerseDts = dts.findAll { it.text() == "1" }
            if (title.contains('Psalm') && !title.endsWith(" 1")) {
                assert firstVerseDts.size() == 2
                assert earliestDds[0].text().startsWith(title)
                // some such as ps 2 consist only of the title
            }
            else if (title.contains('Jeremiah') && title.endsWith(" 39")) {
                assert firstVerseDts.size() == 2
            }
            else {
                assert firstVerseDts.size() == 1
            }
            def ols = doc.select("ol");
            assert ols.size() == 2;
            assert !ols[1].children()
            for (li in ols[0].children()) {
                assert li.children().size() == 1
                def aChild = li.children()[0]
                assert aChild.tagName() == 'a'
                assert aChild.attr('name').startsWith("footnote_")
                if (aChild.children()) {
                    assert aChild.children().collect { it.tagName() }.unique() == ['i']
                }
                def aText = aChild.html()
                if (aText =~ /^\[\d+([\-,]\d+)?\]/ || aText.startsWith("[Title:")) {
                }
                else if (aText =~ /^<i>\d+([\-,]\d+)?<\/i>/) {
                    // first observed in Joel 1
                }
                else if (aText.startsWith("[This chapter is an acrostic poem")) {
                    // Lamentations
                }
                else if (aText.startsWith("The most reliable early manuscripts") || 
                        aText.startsWith("The earliest and most reliable manuscripts")){
                    // observed in Mark, John
                }
                else if (aText.startsWith("In many Hebrew manuscripts Psalms 42 and 43")) {
                }
                else {
                    assert aText.startsWith("Psalms 9 and 10") || aText.startsWith("This psalm")
                }
            }
        }
    }
    
    static transformFiles() {
        new File(".").eachFileRecurse(FileType.FILES) {
            if (!it.name.endsWith(".html")) {
                return
            }
            def outFile = new File(it.path.replaceFirst(/\.html$/, ".tvn"))
            outFile.withWriter { out ->
                println "parsing $it..."
                def doc = Jsoup.parse(it, null);
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
                for (int i = 0; i < verseElems.size(); i+=2) {
                    def dt = verseElems[i]
                    assert dt.tagName() == 'dt'
                    def dd = verseElems[i + 1]
                    assert dd.tagName() == 'dd'
                    if (!chapterTitleProcessed) {
                        chapterTitleProcessed = true
                        if (title.contains('119')) {
                        
                            processPsalm119Title(out, dd, footnoteMap, title)
                            
                            skipVerseCount++
                            continue
                        }
                        else if (title.contains('Psalm') && !title.endsWith(" 1")) {
                            if (dd.html().substring(title.size()).trim()) {
                                processTitleText(out, dd, footnoteMap, title)
                            }
                            skipVerseCount++
                            continue
                        }
                        else if (title.contains('Jeremiah') && title.endsWith(" 39")) {
                            // manually move this title text to be part of verse 1.
                            processTitleText(out, dd, footnoteMap, null)
                            
                            skipVerseCount++
                            continue
                        }
                    }
                    def expectedVerseNum = i.intdiv(2) + 1 - skipVerseCount
                    def omittedCount = dealWithOmittedVerses(title, expectedVerseNum)
                    if (omittedCount > 0) {
                        1.upto(omittedCount) {
                            writeTagValue(out, TAG_VERSE_START, "${expectedVerseNum}", false)
                            writeTagValue(out, TAG_CONTENT, "", true)
                            writeTag(out, TAG_VERSE_END)
                            expectedVerseNum++
                            skipVerseCount--
                        }
                    }
                    assert dt.text() == "$expectedVerseNum"
                    if (title.contains('119') && expectedVerseNum % 8 == 0 && expectedVerseNum < 170) {
                        processPsalm119LastStanzaVerse(out, dd, footnoteMap, expectedVerseNum)
                    }
                    else {
                        processVerseText(out, dd, footnoteMap, expectedVerseNum)
                    }
                }
                
                assert !footnoteMap.size()
            }
        }
    }
    
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
    
    static processPsalm119Title(out, dd, footnoteMap, title) {
        def first = true;
        def alephSeen = false;
        writeTag(out, TAG_CHAP_NOTE_START)
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
                writeTagValue(out, TAG_NOTE_BODY, remainder, true)
            }
            else {
                assert ddChild.tagName() == 'sup'
                def aChild = ddChild.children()[0]
                def fKey = aChild.attr('href')
                assert footnoteMap.containsKey(fKey)
                def footnote = footnoteMap.remove(fKey)
                out << footnote
            }
            first = false
        }
        writeTag(out, TAG_CHAP_NOTE_END)
        
        assert alephSeen
        writeTagValue(out, TAG_NOTE_START, NOTE_KIND_HEADING, false)
        writeTagValue(out, TAG_NOTE_BODY, "&#" + psa119Titles[heading1] +  "; $heading1", false)
        writeTag(out, TAG_NOTE_END)
    }
    
    static processPsalm119LastStanzaVerse(out, dd, footnoteMap, vNum) {
        def alephSeen = false;
        def heading1 = psa119Titles.values().sort()[vNum.intdiv(8)]
        heading1 = psa119Titles.keySet().find { psa119Titles[it] == heading1 }
        assert heading1
        writeTagValue(out, TAG_VERSE_START, "$vNum", false)
        for (ddChild in dd.childNodes()) {
            if (ddChild instanceof TextNode) {
                String remainder = getText(ddChild)
                int alephIndex = remainder.indexOf(heading1)
                if (alephIndex != -1) {
                    alephSeen = true;
                    remainder = remainder.substring(0, alephIndex)
                }
                writeTagValue(out, TAG_CONTENT, remainder, true)
            }
            else {
                assert ddChild.tagName() == 'sup'
                def aChild = ddChild.children()[0]
                def fKey = aChild.attr('href')
                assert footnoteMap.containsKey(fKey)
                def footnote = footnoteMap.remove(fKey)
                out << footnote
            }
        }
        writeTag(out, TAG_VERSE_END)
        
        assert alephSeen
        writeTagValue(out, TAG_NOTE_START, NOTE_KIND_HEADING, false)
        writeTagValue(out, TAG_NOTE_BODY, "&#" + psa119Titles[heading1] +  "; $heading1", false)
        writeTag(out, TAG_NOTE_END)
    }
    
    static processTitleText(out, dd, footnoteMap, removeFromFirst) {
        def first = true;
        
        writeTag(out, TAG_CHAP_NOTE_START)
        for (ddChild in dd.childNodes()) {
            if (ddChild instanceof TextNode) {
                String remainder = getText(ddChild)
                if (first && removeFromFirst) {
                    remainder = remainder.substring(removeFromFirst.size())
                }
                writeTagValue(out, TAG_NOTE_BODY, remainder, true)
            }
            else {
                assert ddChild.tagName() == 'sup'
                def aChild = ddChild.children()[0]
                def fKey = aChild.attr('href')
                assert footnoteMap.containsKey(fKey)
                def footnote = footnoteMap.remove(fKey)
                out << footnote
            }
            first = false
        }
        writeTag(out, TAG_CHAP_NOTE_END)
    }
    
    static processVerseText(out, dd, footnoteMap, vNum) {
        writeTagValue(out, TAG_VERSE_START, "$vNum", false)
        for (ddChild in dd.childNodes()) {
            if (ddChild instanceof TextNode) {
                writeTagValue(out, TAG_CONTENT, getText(ddChild), true)
            }
            else {
                assert ddChild.tagName() == 'sup'
                def aChild = ddChild.children()[0]
                def fKey = aChild.attr('href')
                assert footnoteMap.containsKey(fKey)
                def footnote = footnoteMap.remove(fKey)
                out << footnote
            }
        }
        writeTag(out, TAG_VERSE_END)
    }
    
    static buildFootnoteMap(olWrapper) {
        def map = [:]
        for (li in olWrapper.children()) {
            assert li.children().size() == 1
            def aChild = li.children()[0]
            def mapKey = '#' + aChild.attr('name')
            boolean first = true
            def out = new StringBuilder()
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
                        if (n instanceof Element) {
                            writeTagValue(out, TAG_FANCY_NOTE_BODY, FANCY_NOTE_BODY_TYPE_EM, false)
                        }
                        writeTagValue(out, TAG_NOTE_BODY, t.substring(start, m.start()), true)
                        writeTagValue(out, TAG_FANCY_NOTE_BODY, FANCY_NOTE_BODY_TYPE_OTHER_REF_VERSE, false)
                        writeTagValue(out, TAG_NOTE_BODY, m.group(1), true)
                        start = m.end()
                    }
                    if (start < t.size()) {
                        if (n instanceof Element) {
                            writeTagValue(out, TAG_FANCY_NOTE_BODY, FANCY_NOTE_BODY_TYPE_EM, false)
                        }
                        writeTagValue(out, TAG_NOTE_BODY, t.substring(start), true)
                    }
                }
                else {
                    if (m) {
                        // normalize.
                        writeTagValue(out, TAG_NOTE_START, NOTE_KIND_DIRECT_REF, false)
                        if (n instanceof Element) {
                            assert m.group() == t
                            // only write it out if it consists of multiple verses
                            if (m.group(1)) {
                                writeTagValue(out, TAG_FANCY_NOTE_BODY, FANCY_NOTE_BODY_TYPE_START_REF_VERSE, false)
                                writeTagValue(out, TAG_NOTE_BODY, t, true)
                            }
                        }
                        else {
                            assert m.start() == 0;
                            // only write it out if it consists of multiple verses
                            if (m.group(2)) {
                                writeTagValue(out, TAG_FANCY_NOTE_BODY, FANCY_NOTE_BODY_TYPE_START_REF_VERSE, false)
                                writeTagValue(out, TAG_NOTE_BODY, m.group(1), true)
                            }
                            writeTagValue(out, TAG_NOTE_BODY, t.substring(m.group().size()), true)
                        }
                    }
                    else {
                        assert n instanceof TextNode
                        writeTagValue(out, TAG_NOTE_START, NOTE_KIND_DIRECT_REF, false)
                        if (t.startsWith("[")) {                            
                            writeTagValue(out, TAG_NOTE_BODY, t.substring(1), true)
                        }
                        else {             
                            writeTagValue(out, TAG_NOTE_BODY, t, true)
                        }
                    }
                }
                first = false
            }
            assert out.size()
            writeTag(out, TAG_NOTE_END)
            map[mapKey] = out.toString()
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
        if (elem instanceof TextNode) {
            return elem.wholeText
        }
        return elem.wholeText()
    }
    
    static writeTag(out, tag) {
        writeTagValue(out, tag, null, false)
    }
    
    static writeTagValue(out, tag, value, quote) {
        
        out << tag << ":"
        if (value) {
            if (quote) {
                value = quoteValue(value)
                // replace '--' with &#8212;
                value = value.replace("--", "&#8212;") 
            }
            out << value
        }
        out << "\n"
    }
    
    static quoteValue(value) {
        def sb = new StringBuilder()
        for (int i = 0; i < value.size(); i++) {
            char c = value.charAt(i)
            int pt = (int)c
            if (c == '&' || c == '#' || pt < 32 || pt > 126) {
                sb << '&#' << pt << ';'
            }
            else {
                sb << c
            }
        }
        return sb.toString()
    }
}