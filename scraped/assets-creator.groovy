@Grab(group='xom', module='xom', version='1.3.5')

import nu.xom.*

class AssetsCreator {
    
	public static void main(String[] args) {
		final scriptDir = new File(AssetsCreator.class.protectionDomain.codeSource.location.toURI()).parentFile
		final assetsDir = "../Niv1984-Plus/app/src/main/assets/"
		
		def bibleVersionCode = args[0]
		
		def destDir
		if (args.size() > 1) {
			destDir = new File(args[1])
		}
		else {
			destDir = new File("$scriptDir/$assetsDir/$bibleVersionCode")
		}
		
		// don't automatically create dest dir.
		assert destDir.isDirectory()
		
		def patchFile 
		if (args.size() > 2) {
			patchFile = new File(args[2])
		}
		else {
			patchFile = new File(scriptDir, "${bibleVersionCode}.xml")
		}
		
		def patchXml = new Builder().build(patchFile).rootElement;
		assert patchXml.localName == "transferConfig"
		
		def sourceDir
		switch (bibleVersionCode) {
			case "kjv1769":
				sourceDir = "KJV1769 Bible"
				break
			case "niv1984":
				sourceDir = "NIV1984 Bible"
				break
			default:
				assert false, bibleVersionCode
		}
		sourceDir = new File(scriptDir, sourceDir)
		assert sourceDir.isDirectory()
		
		def patches = patchXml.getChildElements("patches")
		assert patches.size() == 1
		patches = patches.get(0).getChildElements("book")
		
        for (i in 1..66) {
			final commonBasename = String.format("%02d", i)
			final commonName = commonBasename + ".xml"
			def srcFile = new File(sourceDir, commonName)
            def destFile = new File(destDir, commonName)
            println "Generating ${destFile}..."
			
			def bookPatch = null
			for (el in patches) {
                def bookName = el.getAttributeValue("name")
				if (bookName.startsWith(commonBasename)) {
					bookPatch = el
					break
				}
			}
			if (!bookPatch) {
				destFile.bytes = srcFile.bytes
				continue
			}
			
			destFile.newWriter('utf-8').withWriter { writer ->
				srcFile.eachLine('utf-8') { line, oneBasedLineIndex ->
					// transform line if config says so.
					for (patch in bookPatch.getChildElements("patch")) {
						def lineNumber = Integer.parseInt(patch.getAttributeValue("num"))
						if (lineNumber == oneBasedLineIndex) {
							line = patch.value
							def deleteLine = Boolean.parseBoolean(patch.getAttributeValue("delete"))
							if (deleteLine) {
								line = null
							}
							break
						}
					}
					// only write if not marked as deleted.
					if (line != null) {
                        // convert embedded newlines to platform newline since xom seems to normalize them
						writer.write(line.replace("\n", System.lineSeparator()));
                        writer.write(System.lineSeparator());
					}
				}
			}
        }
	}
}
