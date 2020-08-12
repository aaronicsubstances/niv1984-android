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
    static final TAG_BLOCK_QUOTE = "block_quote"
    static final ATTR_KIND = "kind"
    static final ATTR_NUMBER = "num"

    static final TAG_NOTE = "note"
    static final TAG_NOTE_REF = "note_ref"

    enum BlockQuoteKind {
        LEFT, LEFT_INDENTED, RIGHT, CENTER
    }

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
            //if (i != 27-1) return;
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
            //System.exit(0)
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
        def chapterElem = doc.select(".chapter").get(0);
        def chapNum = chapterElem.classNames().find { it != "chapter" }
        assert chapNum && chapNum.startsWith("ch")
        chapNum = Integer.parseInt(chapNum.substring(2))
        def chapDescs = chapterElem.select(".d")
        assert chapDescs.size() < 2
        if (chapDescs) {
            processChapDesc(root, chapDescs.get(0))
        }
        def currVnum = 0
        def outVerseEl = null
        def footNoteList = []
        def noteNumber = 0
        def verseDivs = chapterElem.select(">div")
        for (verseDiv in verseDivs) {
            def quoteKind = null
            def quoteEl = null
            def quoteVerseNum = null
            // identify fragments of verses in quotations
            if (verseDiv.classNames().find { it.startsWith("q") }) {
                assert verseDiv.classNames().size() == 1
                def divCls = verseDiv.classNames()[0]
                assert divCls == 'q1' || divCls == 'q2' || divCls == 'qr' || divCls == 'qc'
                quoteKind = BlockQuoteKind.LEFT
                if (divCls == 'q2') {
                    quoteKind = BlockQuoteKind.LEFT_INDENTED.toString()
                }
                else if (divCls == 'qc') {
                    quoteKind = BlockQuoteKind.CENTER.toString()
                }
                else if (divCls == 'qr') {
                    quoteKind = BlockQuoteKind.RIGHT.toString()
                }
            }
            def verses = verseDiv.select(".verse")
            for (v in verses) {
                def vNum = v.classNames().find { it != "verse" }
                assert vNum && vNum.startsWith("v")
                vNum = Integer.parseInt(vNum.substring(1))
                if (vNum != currVnum) {
                    if (bookDir.contains("DAG") && chapNum == 10) {
                        // manually handle daniel chapter 10 later.
                        currVnum = vNum
                    }
                    else {
                        assert vNum == currVnum + 1
                        currVnum++
                    }
                    
                    // skip RC verses and
                    // manually transfer last 3 verses of daniel 3 to beginning of 4.
                    if (bookDir.contains("DAG") && chapNum == 3 && vNum > 23 && vNum < 91) {
                        continue
                    }
                    outVerseEl = new nu.xom.Element(TAG_VERSE)
                    root.appendChild(outVerseEl)
                    
                    def vNumStr = String.valueOf(vNum)
                    if (bookDir.contains("DAG")) {
                        if (chapNum == 3) {
                            if (vNum > 23) {
                                def rewrite = vNum + 23 - 90
                                if (rewrite > 30) {
                                    rewrite -= 30
                                }
                                vNumStr = "${rewrite}"
                            }
                        }
                        else if (chapNum == 4) {
                            vNumStr = "${vNum + 3}"
                        }
                        //manually transfer first verse of chapter 6 to last verse of chapter 5.
                        else if (chapNum == 6) {
                            if (vNum > 1) {
                                vNumStr = "${vNum - 1}"
                            }
                            else {
                                vNumStr = "31"
                            }
                        }
                    }
                    outVerseEl.addAttribute(new nu.xom.Attribute(ATTR_NUMBER, vNumStr))
                }
                
                // skip RC verses
                if (bookDir.contains("DAG") && chapNum == 3 && currVnum > 23 && currVnum < 91) {
                    continue
                }
                
                if (quoteKind && quoteVerseNum != vNum) {
                    if (quoteEl) {
                        // only add if it has non-ws value.
                        // add to previous.
                        if (quoteEl.value.trim()) {
                            root.getChild(root.childCount - 2).appendChild(quoteEl)
                        }
                    }
                    quoteEl = new nu.xom.Element(TAG_BLOCK_QUOTE)
                    if (quoteKind && quoteKind != BlockQuoteKind.LEFT) {
                        quoteEl.addAttribute(new nu.xom.Attribute(ATTR_KIND, quoteKind))
                    }
                    // let subsequent additions be to quote instead.
                    // but don't add to verse until we can later verify it is non empty.
                    assert outVerseEl
                    outVerseEl = quoteEl 
                    quoteVerseNum = vNum
                }
                for (c in v.children()) {
                    def elemClasses = c.classNames()
                    if (elemClasses) {
                        if (elemClasses.contains("content")) {
                            processContent(outVerseEl, c)
                        }
                        else if (elemClasses.contains("label")) {
                            assert elemClasses.size() == 1
                            assert !c.children()
                            // skip.
                        }
                        else if (elemClasses.contains("it")) {
                            assert elemClasses.size() == 1
                            assert c.children().size() == 1
                            processContent(outVerseEl, c.children().get(0), FancyContentKind.EM)
                        }
                        else if (elemClasses.contains("qs")) {
                            assert elemClasses.size() == 1
                            assert c.children().size() == 1
                            processContent(outVerseEl, c.children().get(0), FancyContentKind.SELAH)
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
                                // exclude note numbering on cross refs
                                def crossRefEl = new nu.xom.Element(TAG_NOTE)
                                crossRefEl.addAttribute(new nu.xom.Attribute(ATTR_KIND, "${NoteKind.CROSS_REFERENCES}"))
                                crossRefEl.appendChild(createOutContentEl("$vNum", NoteContentKind.REF_VERSE_START))
                                crossRefEl.appendChild(createOutContentEl(": " + getText(children[1])))
                                footNoteList << crossRefEl
                                
                            }
                            else {
                                assert elemClasses.contains("f")
                                processNoteF(cn, outVerseEl, c, currVnum, ++noteNumber, footNoteList)
                            }
                        }
                        else if (elemClasses.contains("wj")) {
                            assert elemClasses.size() == 1
                            assert c.children().size() > 0
                            def wjEl = new nu.xom.Element("wj")
                            outVerseEl.appendChild(wjEl)
                            for (grandChild in c.children()) {
                                def grandChildHtmlClasses = grandChild.classNames()
                                if (grandChildHtmlClasses.contains("content")) {
                                    processContent(wjEl, grandChild)
                                }
                                else {
                                    assertArrayEquals(grandChildHtmlClasses, ['note', 'f'])
                                    processNoteF(cn, wjEl, grandChild, currVnum, ++noteNumber, footNoteList)
                                }
                            }
                        }
                        else {
                            throw new Exception("Unexpected elem classes: " + elemClasses)
                        }
                    }
                }
            }
            if (quoteEl) {
                // only add if it has non-ws value.
                if (quoteEl.value.trim()) {
                    root.getChild(root.childCount - 1).appendChild(quoteEl)
                }
            }
        }
        footNoteList.each {
            // rewrite
            if (bookDir.contains("DAG")) {
                if (chapNum == 3 || chapNum == 4 || chapNum == 6) {
                    def firstContentEl = it.getChildElements("content")[0]
                    assert  firstContentEl.getAttributeValue("kind") == "REF_VERSE_START"
                    def refVerseNum = Integer.parseInt(firstContentEl.value)
                    
                    if (chapNum == 3) {
                        if (refVerseNum > 23) {
                            if (refVerseNum < 91) {
                                // don't add at all.
                                return;
                            }
                            
                            assert  it.getAttributeValue("kind") == "CROSS_REFERENCES"
                            if (refVerseNum > 97) {
                                // meant for chapter 4
                                refVerseNum -= 30
                            }
                            firstContentEl.removeChildren()
                            firstContentEl.appendChild("${refVerseNum + 23 - 90}")
                        }
                    }
                    else if (chapNum == 4) {
                        assert  it.getAttributeValue("kind") == "CROSS_REFERENCES"
                        firstContentEl.removeChildren()
                        firstContentEl.appendChild("${refVerseNum + 3}")
                    }
                    else if (chapNum == 6) {
                        if (refVerseNum > 1) {
                            refVerseNum--
                        }
                        else {
                            // manually insert cross reference for 5v31
                            refVerseNum = 31
                        }
                        assert  it.getAttributeValue("kind") == "CROSS_REFERENCES"
                        firstContentEl.removeChildren()
                        firstContentEl.appendChild("${refVerseNum}")
                    }
                }
            }
            root.appendChild(it)
        }
        return root
    }
    
    static processContent(root, c, type=null) {        
        assert c.classNames().size() == 1
        assert !c.children()
        root.appendChild(createOutContentEl(c, type))
    }
    
    static processNoteF(cn, root, n, vNum, noteNumber, footNoteList) {
        // locate first fr, and only fetch after that.
        boolean skip = true
        def noteEl = null
        for (c in n.children().get(1).children()) {
            def names = c.classNames()
            if (skip) {
                if (names.contains("fr")) {
                    skip = false
                    noteEl = new nu.xom.Element(TAG_NOTE)
                    noteEl.addAttribute(new nu.xom.Attribute(ATTR_NUMBER, "$noteNumber"))
                    def refTxt = getText(c)
                    def refTxtMatcher = refTxt =~ "^$cn\\.($vNum(?:\\D+\\d+)*)\\D+\$"
                    assert refTxtMatcher.find()
                    noteEl.appendChild(createOutContentEl("${refTxtMatcher.group(1)}", NoteContentKind.REF_VERSE_START))
                    def sep = refTxt.substring(refTxtMatcher.end())
                    if (!sep) {
                        sep = " "
                    }
                    noteEl.appendChild(createOutContentEl(sep))
                }
                continue
            }
            def contentKind = null
            if (names.contains("fk")) {
                contentKind = NoteContentKind.STRONG_EM
            }
            else if (names.contains("fq")) {
                contentKind = NoteContentKind.EM
            }
            else if (names.contains("ft")) {
                //contentKind = NoteContentKind.NONE
            }
            else {
                throw new AssertionError("Unexpected note f: " + n)
            }
            noteEl.appendChild(createOutContentEl(c, contentKind))
        }
        assert noteEl
        footNoteList << noteEl
        def noteRef = new nu.xom.Element(TAG_NOTE_REF)
        noteRef.appendChild("$noteNumber")
        root.appendChild(noteRef)
    }
    
    static processChapDesc(root, c) {
        assert c.children().size() == 1
        def child = c.children().get(0)
        assertArrayEquals(child.classNames(), ['content'])
        assert !child.children()
        def fragEl = new nu.xom.Element(TAG_CHAPTER_FRAGMENT)
        root.appendChild(fragEl)
        fragEl.appendChild(createOutContentEl(child, FancyContentKind.EM))
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
        el.appendChild(elemText)
        return el
    }
}