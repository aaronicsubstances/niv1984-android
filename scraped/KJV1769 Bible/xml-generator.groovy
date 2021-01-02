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
        NONE, EM, STRONG_EM, SELAH
    }

    static final BIBLE_BOOK_CHAPTER_COUNT = [
        50, 40, 27, 36, 34, 24, 21, 4, 31, 24, 22, 25, 29, 36, 10, 13, 10, 42, 150,
        31, 12, 8, 66, 52, 5, 48, 12, 14, 3, 9, 1, 4, 7, 3, 3, 3, 2, 14, 4,
        28, 16, 24, 21, 28, 16, 16, 13, 6, 6, 4, 4, 5, 3, 6, 4, 3, 1, 13,
        5, 5, 3, 5, 1, 1, 1, 22]

	public static void main(String[] args) {
		final scriptDir = new File(XmlGenerator.class.protectionDomain.codeSource.location.toURI()).parentFile
        scriptDir.list().findAll {
            new File(scriptDir, it).isDirectory()
        }.sort(false).eachWithIndex { n, i ->
            def destFile = new File(scriptDir, String.format("%02d.xml", i+1))
            println "Generating ${destFile}..."
            def root = new nu.xom.Element(TAG_BOOK)
            def chapterCount =  BIBLE_BOOK_CHAPTER_COUNT[i]
            1.upto(chapterCount) { cn ->
                def chapterEl = processChapter(new File(scriptDir, n), cn)
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
        def chapterElem = doc.select("#div").get(0);
        def verses = chapterElem.select("p")
        def vNum = 0
        def ps119verse = null
        for (p in verses) {
            def aChild = p.children().get(0)
            def outVerseEl = null 
            vNum++
            for (n in aChild.childNodes()) {
                if (n instanceof TextNode) {                 
                    if (cn == 119 && vNum % 8 == 1) {
                        def txt = getText(n)
                        if (ps119verse != vNum) {
                            ps119verse = vNum
                            def headingMatcher = txt =~ /^(\w+)\.\s*/
                            assert(headingMatcher.find())
                            def fragEl = new nu.xom.Element(TAG_CHAPTER_FRAGMENT)
                            fragEl.addAttribute(new nu.xom.Attribute(ATTR_KIND, "${ChapterFragmentKind.HEADING}"))
                            root.appendChild(fragEl)
                            fragEl.appendChild(createOutContentEl(headingMatcher.group(1)))
                            txt = txt.substring(headingMatcher.end())
                        }
                        
                        if (outVerseEl == null) {
                            outVerseEl = new nu.xom.Element(TAG_VERSE)
                            root.appendChild(outVerseEl)
                            outVerseEl.addAttribute(new nu.xom.Attribute(ATTR_NUMBER, String.valueOf(vNum)))
                        }
                        
                        outVerseEl.appendChild(createOutContentEl(txt))
                    }
                    else if (bookDir.name.startsWith("19") && vNum == 1) {
                        def txt = getText(n)
                        def parenMatcher = txt =~ /^\(([^)]+)\)\s*/
                        if (parenMatcher) {
                            def fragEl = new nu.xom.Element(TAG_CHAPTER_FRAGMENT)
                            root.appendChild(fragEl)
                            fragEl.appendChild(createOutContentEl(parenMatcher.group(1), FancyContentKind.EM))
                            txt = txt.substring(parenMatcher.end())                            
                        }
                        
                        if (outVerseEl == null) {
                            outVerseEl = new nu.xom.Element(TAG_VERSE)
                            root.appendChild(outVerseEl)
                            outVerseEl.addAttribute(new nu.xom.Attribute(ATTR_NUMBER, String.valueOf(vNum)))
                        }
                        
                        outVerseEl.appendChild(createOutContentEl(txt))
                    }
                    else {
                    
                        if (outVerseEl == null) {
                            outVerseEl = new nu.xom.Element(TAG_VERSE)
                            root.appendChild(outVerseEl)
                            outVerseEl.addAttribute(new nu.xom.Attribute(ATTR_NUMBER, String.valueOf(vNum)))
                        }
                        
                        outVerseEl.appendChild(createOutContentEl(n))
                    }
                }
                else {
                    assert n instanceof Element
                    if (n.tagName() == 'span') {
                        if (n.classNames().contains('jesus')) {
                        
                            if (outVerseEl == null) {
                                outVerseEl = new nu.xom.Element(TAG_VERSE)
                                root.appendChild(outVerseEl)
                                outVerseEl.addAttribute(new nu.xom.Attribute(ATTR_NUMBER, String.valueOf(vNum)))
                            }
                        
                            def wjEl = new nu.xom.Element(TAG_WJ)
                            outVerseEl.appendChild(wjEl)
                            for (jChild in n.childNodes()) {
                                if (jChild instanceof TextNode) {
                                    wjEl.appendChild(createOutContentEl(jChild))
                                }
                                else {
                                    assert jChild instanceof Element
                                    assert jChild.tagName() == 'em'
                                    wjEl.appendChild(createOutContentEl(jChild, FancyContentKind.EM))
                                }
                            }
                        }
                        else {
                            // verse number. skip.
                        }
                    }
                    else {
                        assert n.tagName() == 'em'
                        
                        if (outVerseEl == null) {
                            outVerseEl = new nu.xom.Element(TAG_VERSE)
                            root.appendChild(outVerseEl)
                            outVerseEl.addAttribute(new nu.xom.Attribute(ATTR_NUMBER, String.valueOf(vNum)))
                        }
                        
                        outVerseEl.appendChild(createOutContentEl(n, FancyContentKind.EM))
                    }
                }
            }
        }
        return root
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
        el.appendChild(getText(elem))
        return el
    }
}