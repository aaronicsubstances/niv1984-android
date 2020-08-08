@Grab(group='org.jsoup', module='jsoup', version='1.13.1')

import groovy.io.FileType

import org.jsoup.Jsoup;

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
    
    static NOTE_KIND_HEADING = "heading"
    static NOTE_KIND_DIRECT_REF = "dref"
    static NOTE_KIND_OTHER = "other"

	public static void main(String[] args) {
        //assertSoleChapterClass()
        /*def wjClasses = new HashSet()
        def classes = gatherVerseDirectChildrenClasses(wjClasses)
        // note x, note f, label, content, wj, it, qs
        println "${classes.size()} classes found: $classes"
        // note f, content
        println "${wjClasses.size()} classes found: $wjClasses"*/
        assert quoteValue('a') == 'a'
        assert quoteValue('1') == '1'
        assert quoteValue('16*') == '16*'
        assert quoteValue(' a') == ' a'
        assert quoteValue('\na') == '&#10;a'
        assert quoteValue('\ra#') == '&#13;a&#35;'
        assert quoteValue('\r\na&') == '&#13;&#10;a&#38;'
        transformFiles()
	}
    
    static assertSoleChapterClass() {
        new File(".").eachFileRecurse(FileType.FILES) {
            if (!it.name.endsWith(".html")) {
                return
            }
            println "parsing $it..."
            def doc = Jsoup.parse(it, null);
            def elems = doc.select(".chapter");
            assert elems.size() == 1;
        }
    }
    
    static gatherVerseDirectChildrenClasses(wjClasses) {
        def classes = new HashSet()
        new File(".").eachFileRecurse(FileType.FILES) {
            if (!it.name.endsWith(".html")) {
                return
            }
            //println "parsing $it..."
            def doc = Jsoup.parse(it, null);
            def chapterElem = doc.select(".chapter").get(0);
            def verses = chapterElem.select(".verse")
            assert verses.size() > 0
            for (v in verses) {
                for (c in v.children()) {
                    def elemClasses = c.classNames()
                    if (elemClasses) {
                        classes.add(elemClasses)
                        if (elemClasses.contains("content") || elemClasses.contains("label")) {
                            assert !c.children()
                        }
                        if (elemClasses.contains("it")) {
                            // only found at JDG 12:6, the rest are all in Daniel 3 RC version.
                            //println("found 'it' at $it: " + v.classNames())
                            assert c.children().collect { it.classNames() }.flatten() == [ 'content' ]
                        }
                        if (elemClasses.contains("qs")) {
                            // found only at HAB 3:3, 9, 13
                            //println("found 'qs' at $it: " + v.classNames())
                            assert c.children().collect { it.classNames() }.flatten() == [ 'content' ]
                        }
                        if (elemClasses.contains("wj")) {
                            // found throughout gospels, acts and revelations
                            //println("found 'wj' at $it: " + v.classNames())
                            c.children().each { iti -> 
                                wjClasses.add(iti.classNames())
                                if (iti.classNames().contains("note")) {
                                    // mat 18:24, 
                                    // mat 20:23 - not in my twi bible
                                    // mrk 10:24 - not bracketed in my twi bible
                                    // Lk 16:9
                                    println("found 'note' in 'wj' at $it: ${v.classNames()}" + iti.classNames())
                                }
                            }
                        }
                        if (elemClasses.contains("note")) {
                            
                        }
                    }
                }
            }
        }
        return classes
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
                def chapterElem = doc.select(".chapter").get(0);
                def chapNum = chapterElem.classNames().find { it != "chapter" }
                assert chapNum && chapNum.startsWith("ch")
                chapNum = Integer.parseInt(chapNum.substring(2))
                def chapDescs = chapterElem.select(".d")
                assert chapDescs.size() < 2
                if (chapDescs) {
                    processChapDesc(out, chapDescs.get(0))
                }
                def verses = chapterElem.select(".verse")
                def currVnum = 0 
                for (v in verses) {
                    def vNum = v.classNames().find { it != "verse" }
                    assert vNum && vNum.startsWith("v")
                    vNum = Integer.parseInt(vNum.substring(1))
                    if (vNum != currVnum) {
                        if (chapNum == 10 && it.path.contains("DAG")) {
                            // manually handle daniel chapter 10 later.
                            currVnum = vNum
                        }
                        else {
                            assert vNum == currVnum + 1
                            currVnum++
                        }
                        if (currVnum > 1) {
                            writeTag(out, TAG_VERSE_END)
                        }
                        writeTagValue(out, TAG_VERSE_START, vNum, false)
                    }
                    for (c in v.children()) {
                        def elemClasses = c.classNames()
                        if (elemClasses) {
                            if (elemClasses.contains("content")) {
                                processContent(out, c)
                            }
                            else if (elemClasses.contains("label")) {
                                assert elemClasses.size() == 1
                                assert !c.children()
                                // skip.
                            }
                            else if (elemClasses.contains("it")) {
                                assert elemClasses.size() == 1
                                writeTagValue(out, TAG_FANCY_CONTENT, FANCY_CONTENT_TYPE_EM, false)
                                assert c.children().size() == 1
                                processContent(out, c.children().get(0))
                            }
                            else if (elemClasses.contains("qs")) {
                                assert elemClasses.size() == 1
                                writeTagValue(out, TAG_FANCY_CONTENT, FANCY_CONTENT_TYPE_SELAH, false)
                                assert c.children().size() == 1
                                processContent(out, c.children().get(0))
                            }
                            else if (elemClasses.contains("note")) {
                                assert elemClasses.size() == 2
                                def children = c.children()
                                assert children.size() == 2
                                assertArrayEquals(children[0].classNames(), ['label'])
                                assertArrayEquals(children[1].classNames(), ['body'])
                                assert !children[0].children()
                                
                                if (elemClasses.contains("x")) {
                                    assert !children[1].children()
                                    writeTagValue(out, TAG_NOTE_START, NOTE_KIND_OTHER, false)
                                    writeTagValue(out, TAG_NOTE_BODY, getText(children[1]), true)
                                    writeTag(out, TAG_NOTE_END)
                                }
                                else {
                                    assert elemClasses.contains("f")
                                    processNoteF(out, c, currVnum)
                                }
                            }
                            else if (elemClasses.contains("wj")) {
                                assert elemClasses.size() == 1
                                assert c.children().size() > 0
                                writeTag(out, TAG_WJ_START)
                                for (grandChild in c.children()) {
                                    def grandChildHtmlClasses = grandChild.classNames()
                                    if (grandChildHtmlClasses.contains("content")) {
                                        processContent(out, grandChild)
                                    }
                                    else {
                                        assertArrayEquals(grandChildHtmlClasses, ['note', 'f'])
                                        processNoteF(out, grandChild, currVnum)
                                    }
                                }
                                writeTag(out, TAG_WJ_END)
                            }
                            else {
                                throw new Exception("Unexpected elem classes: " + elemClasses)
                            }
                        }
                    }
                }
                assert currVnum > 0
                writeTag(out, TAG_VERSE_END)
            }
        }
    }
    
    static assertArrayEquals(a, b) {
        int minLen = Math.min(a.size(), b.size())
        for (int i = 0; i < minLen; i++) {
            assert a[i] == b[i]
        }
        assert a.size() == b.size()
    }
    
    static getText(elem) {
        return elem.wholeText()
    }
    
    static processContent(out, c) {        
        assert c.classNames().size() == 1
        assert !c.children()
        writeTagValue(out, TAG_CONTENT, getText(c), true)
    }
    
    static processNoteF(out, n, vNum) {
        // locate first fr, and only fetch after that.
        boolean skip = true
        for (c in n.children().get(1).children()) {
            def names = c.classNames()
            if (skip) {
                if (names.contains("fr")) {
                    skip = false
                    writeTagValue(out, TAG_NOTE_START, NOTE_KIND_DIRECT_REF, false)
                }
                continue
            }
            def isFancyNote = false
            if (names.contains("fk")) {
                isFancyNote = true
                writeTagValue(out, TAG_FANCY_NOTE_BODY, FANCY_NOTE_BODY_TYPE_STRONG_EM, false)
            }
            else if (names.contains("fq")) {
                isFancyNote = true
                writeTagValue(out, TAG_FANCY_NOTE_BODY, FANCY_NOTE_BODY_TYPE_EM, false)
            }
            if (isFancyNote || names.contains("ft")) {
                writeTagValue(out, TAG_NOTE_BODY, getText(c), true)
            }
            else {
                throw new AssertionError("Unexpected note f: " + n)
            }
        }
        assert !skip
        writeTag(out, TAG_NOTE_END)
    }
    
    static processChapDesc(out, c) {
        assert c.children().size() == 1
        def child = c.children().get(0)
        assertArrayEquals(child.classNames(), ['content'])
        assert !child.children()
        writeTag(out, TAG_CHAP_NOTE_START)
        writeTagValue(out, TAG_NOTE_BODY, getText(child), true)
        writeTag(out, TAG_CHAP_NOTE_END)
    }
    
    static writeTag(out, tag) {
        writeTagValue(out, tag, null, false)
    }
    
    static writeTagValue(out, tag, value, quote) {
        out << tag << ":"
        if (value) {
            if (quote) {
                value = quoteValue(value)
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