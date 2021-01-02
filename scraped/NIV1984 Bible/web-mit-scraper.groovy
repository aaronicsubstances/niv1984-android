class WebMitScraper {
    
    public static void main(String[] args) {
        final scriptDir = new File(WebMitScraper.class.protectionDomain.codeSource.location.toURI()).parentFile
        def templateUrlSrc = new File(scriptDir, "web-mit-template-links.txt")
        def totalChapterCount = 0
        templateUrlSrc.eachLine { s, i ->
            def match = s =~ '/([^/]+)\\+\\d+\\.html$'
            assert match.find()

            def bookDir = new File(scriptDir, String.format("%02d-%s", i, match.group(1)))
            bookDir.mkdir()

            def chapter = 1
            try {
                while (true) {
                    def chapterFile = new File(bookDir, String.format("%03d.html", chapter))
                    def chapterUrl = s.replaceAll(/\d+\.html$/, "${chapter}.html")
                    
                    println("Downloading ${bookDir.name}, ${chapterFile.name} from $chapterUrl...")     
                    chapterFile.bytes = new URL(chapterUrl).text.getBytes('utf-8')
                    
                    chapter++
                    totalChapterCount++
                }
            }
            catch (FileNotFoundException fex) {
                println("Done with ${bookDir.name}.")
            }

            println()
        }

        assert totalChapterCount == 1189
        println("Done.")
    }
}