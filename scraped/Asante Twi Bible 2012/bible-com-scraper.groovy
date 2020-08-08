@Grab(group='org.apache.httpcomponents', module='httpclient', version='4.5.2')

import groovy.io.FileType

import org.apache.http.client.methods.*
import org.apache.http.entity.*
import org.apache.http.impl.client.*

class Scraper {

	public static void main(String[] args) {
		def chapterUrlPrefix = 'https://www.bible.com/bible/1461/'

		
		def client = HttpClientBuilder.create().disableRedirectHandling().disableCookieManagement().build()
		
		// book dir name.
		def totalChapterCount = 0
		new File(".").eachFile(FileType.DIRECTORIES) { bookDir ->
			def book = bookDir.name.substring(bookDir.name.indexOf("-") + 1)
			def chapter = 1
			while (true) {
				def chapterFile = new File(bookDir, String.format("%03d.html", chapter))
				def chapterUrl = "${chapterUrlPrefix}${book}.${chapter}.ASWDC"
				
				println("Downloading ${bookDir.name}, ${chapterFile.name} from $chapterUrl...")		
				def res = makeHttpRequest(client, chapterUrl)
				if (res) {
					chapterFile.text = res
					totalChapterCount++
				}
				else {
					break
				}
				
				chapter++
			}
			println("Done with ${bookDir.name}.")
		}
		println()
		assert totalChapterCount == 1189 + 2 // from book of Daniel in Roman Catholic
	}
	
	static makeHttpRequest(client, url) {
		def req = new HttpGet(url)

		req.addHeader("content-type", "text/html")

		// execute 

		def response = client.execute(req)
		// redirects breaks scraping.
		if (response.getStatusLine().getStatusCode() != 200) {
			assert response.getStatusLine().getStatusCode() == 303;
			return null
		}
		def bufferedReader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()))
		def responseText = bufferedReader.getText()
		return responseText
	}
}
//https://www.livelingua.com/course/fsi/Twi(Akan)_Basic_Course
//https://babel.hathitrust.org/cgi/pt?id=hvd.32044004547915&view=1up&seq=45 - christaller grammar on twi
//https://babel.hathitrust.org/cgi/pt?id=uc1.31210006441156&view=1up&seq=11 - basic course pdf