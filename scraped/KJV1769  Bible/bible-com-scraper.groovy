@Grab(group='org.apache.httpcomponents', module='httpclient', version='4.5.2')

import groovy.io.FileType

import org.apache.http.client.methods.*
import org.apache.http.entity.*
import org.apache.http.impl.client.*

class Scraper {
    static chapterCounts = [
        50, 40, 27, 36, 34, 24, 21, 4, 31, 24, 22, 25, 29, 36, 10, 13, 10, 42, 150,
        31, 12, 8, 66, 52, 5, 48, 12, 14, 3, 9, 1, 4, 7, 3, 3, 3, 2, 14, 4,
        28, 16, 24, 21, 28, 16, 16, 13, 6, 6, 4, 4, 5, 3, 6, 4, 3, 1, 13,
        5, 5, 3, 5, 1, 1, 1, 22
    ]
	public static void main(String[] args) {
        assert chapterCounts.sum() == 1189
        
		def chapterUrlPrefix = 'https://www.kingjamesbibleonline.org/'
		
		def client = HttpClientBuilder.create().disableRedirectHandling().disableCookieManagement().build()

		// book dir name.
		def totalChapterCount = 0
        def bookIndex = 0
		new File(".").eachFile(FileType.DIRECTORIES) { bookDir ->
            def chapterCount = chapterCounts[bookIndex]
			def book = bookDir.name.substring(bookDir.name.indexOf("-") + 1)
			1.upto(chapterCount) { chapter ->
				def chapterFile = new File(bookDir, String.format("%03d.html", chapter))
				def chapterUrl = "${chapterUrlPrefix}${book}-Chapter-${chapter}/"
				
				println("Downloading ${bookDir.name}, ${chapterFile.name} from $chapterUrl...")
		
				def res = makeHttpRequest(client, chapterUrl)                
				chapterFile.text = res
                totalChapterCount++
			}
			println("Done with ${bookDir.name}.")
            bookIndex++
		}
		println()
		assert totalChapterCount == 1189
	}
	
	static makeHttpRequest(client, url) {
		def req = new HttpGet(url)

		req.addHeader("content-type", "text/html")

		def response = client.execute(req)
		assert response.getStatusLine().getStatusCode() == 200;
		def bufferedReader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()))
		def responseText = bufferedReader.getText()
		return responseText
	}
}