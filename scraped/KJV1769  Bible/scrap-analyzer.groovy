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
    static TAG_NOTE_START = "note_start";
    static TAG_NOTE_END = "note_end";
    static TAG_NOTE_BODY = "note_body";
    static TAG_WJ_START = "wj_start";
    static TAG_WJ_END = "wj_end";
    static TAG_VERSE_START = "v_start";
    static TAG_VERSE_END = "v_end";
    static TAG_FANCY_CONTENT = "fancy_content"; // CSV of blockquote, emphasis
    static TAG_FANCY_NOTE_BODY = "fancy_note_body"; // CSV of keyword, emphasis
    
    static FANCY_CONTENT_TYPE_EM = "em"
    static FANCY_CONTENT_TYPE_SELAH = "selah"
    static FANCY_NOTE_BODY_TYPE_EM = "em"
    static FANCY_NOTE_BODY_TYPE_STRONG_EM = "strong_em"
    static NOTE_KIND_DIRECT_REF = "dref"
    static NOTE_KIND_OTHER = "other"

	public static void main(String[] args) {
        //assertSoleChapterClass()
        assert quoteValue('a') == 'a'
        assert quoteValue('1') == '1'
        assert quoteValue('16*') == '16*'
        assert quoteValue(' a') == ' a'
        assert quoteValue('\na') == '&#10;a'
        assert quoteValue('\ra#') == '&#13;a&#35;'
        assert quoteValue('\r\na&') == '&#13;&#10;a&#38;'
        //verifyFiles()
        transformFiles()
	}
    
    static assertSoleChapterClass() {
        new File(".").eachFileRecurse(FileType.FILES) {
            if (!it.name.endsWith(".html")) {
                return
            }
            println "parsing $it..."
            def doc = Jsoup.parse(it, null);
            def elems = doc.select("#div");
            assert elems.size() == 1;
        }
    }
    
    static verifyFiles() {    
        new File(".").eachFileRecurse(FileType.FILES) {
            if (!it.name.endsWith(".html")) {
                return
            }
            println "parsing $it..."
            def doc = Jsoup.parse(it, null);
            def chapterElem = doc.select("#div").get(0);
            def verses = chapterElem.select("p")
            int vNum = 0
            for (p in verses) {
                def pChildren = p.children()
                assert pChildren.size() == 1
                def aChild = pChildren.get(0)
                assert aChild.tagName() == 'a'
                def spans = []
                for (n in aChild.childNodes()) {
                    if (!(n instanceof TextNode)) {
                        assert n instanceof Element
                        if (n.tagName() == 'span') {
                            def nClassNames = n.classNames()
                            assert nClassNames.size() == 1
                            if (!nClassNames.contains('versehover')) {
                                assert nClassNames.contains('jesus')
                                def jChildren = n.childNodes()
                                for (jChild in jChildren) {
                                    if (!(jChild instanceof TextNode)) {
                                        assert jChild instanceof Element
                                        assert jChild.tagName() == 'em'
                                    }
                                }
                            }
                            spans << n
                        }
                        else {
                            assert n.tagName() == 'em'
                        }
                    }
                }
                def vNumSpan = spans.find { it.classNames().contains("versehover") }
                assert vNumSpan
                assert vNumSpan.text() == "${vNum + 1}"
                vNum++
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
                def chapterElem = doc.select("#div").get(0);
                def verses = chapterElem.select("p")
                def vNum = 0 
                for (p in verses) {
                    def aChild = p.children().get(0)
                    writeTagValue(out, TAG_VERSE_START, ++vNum, false)
                    for (n in aChild.childNodes()) {
                        if (n instanceof TextNode) {
                            processContent(out, n)
                        }
                        else {
                            assert n instanceof Element
                            if (n.tagName() == 'span') {
                                if (n.classNames().contains('jesus')) {
                                    writeTag(out, TAG_WJ_START)                                    
                                    for (jChild in n.childNodes()) {
                                        if (jChild instanceof TextNode) {
                                            processContent(out, jChild)
                                        }
                                        else {
                                            assert jChild instanceof Element
                                            assert jChild.tagName() == 'em'
                                            writeTagValue(out, TAG_FANCY_CONTENT, FANCY_CONTENT_TYPE_EM, false)
                                            processContent(out, jChild)
                                        }
                                    }
                                    writeTag(out, TAG_WJ_END)
                                }
                            }
                            else {
                                assert n.tagName() == 'em'
                                writeTagValue(out, TAG_FANCY_CONTENT, FANCY_CONTENT_TYPE_EM, false)
                                processContent(out, n)
                            }
                        }
                    }
                    writeTag(out, TAG_VERSE_END)
                }
            }
        }
    }
    
    static getText(elem) {
        if (elem instanceof TextNode) {
            return elem.wholeText
        }
        else {
            return elem.wholeText()
        }
    }
    
    static processContent(out, c) {
        writeTagValue(out, TAG_CONTENT, getText(c), true)
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