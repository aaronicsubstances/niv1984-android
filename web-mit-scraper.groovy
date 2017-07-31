def templateUrlSrc = new File(args[0]) 
def destFolder = templateUrlSrc.canonicalFile.parentFile
totalChapterCount = 0
templateUrlSrc.eachLine { s, i ->
	match = s =~ '/([^/]+)\\+\\d+\\.html$'
	assert match.find()

	bookDir = new File(destFolder, String.format("%02d-%s", i, match.group(1)))
	bookDir.mkdir()

	chapter = 1
	try {
		while (true) {
			chapterFile = new File(bookDir, String.format("%03d.html", chapter))
			chapterUrl = s.replaceAll(/\d+\.html$/, "${chapter}.html")
			
			println("Downloading ${bookDir.name}, ${chapterFile.name} from $chapterUrl...")		
			chapterFile.text = new URL(chapterUrl).text
			
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