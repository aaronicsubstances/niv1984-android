@Grab(group='org.jsoup', module='jsoup', version='1.13.1')
@Grab(group='xom', module='xom', version='1.3.5')

import nu.xom.*

class XmlPostProcessor {
    public static void main(String[] args) {
        new File(".").list().findAll {
            it ==~ /\d{2}\.xml/
        }.sort(false).eachWithIndex { n, i ->
            def out = processOmissions(n)
            if (out) {
                println "Processing ${n}...\n$out"
            }
        }
    }
    
    static processOmissions(bn) {
        def out = new StringBuilder()
        def parser = new Builder()
        def doc = parser.build(new File(bn))
        def root = doc.rootElement
        assert root.localName == "book"
        for (chapter in root.getChildElements("chapter")) {
            def chapterNumber = chapter.getAttributeValue("num")
            def noteIndex = 0
            for (note in chapter.getChildElements("note")) {
                def allNoteText = note.value
                def matcher = allNoteText =~ /(?i)tete/
                if (matcher) {
                    out << "chapter $chapterNumber, note $noteIndex: $allNoteText\n"
                }
                noteIndex++
            }
        }
        return out.toString()
    }
}