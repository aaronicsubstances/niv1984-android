@Grab(group='org.apache.httpcomponents', module='httpclient', version='4.5.2')

import groovy.io.FileType

import org.apache.http.client.methods.*
import org.apache.http.entity.*
import org.apache.http.impl.client.*

class BiblicaScraper {

    public static void main(String[] args) {
        def destDir = new File(args[0])
        def urlCode
        switch (destDir.name) {
            case "Asante Twi Bible 2012":
                urlCode = 1461
                break
            default:
                assert false, destDir.name
        }
        def chapterUrlPrefix = "https://www.bible.com/bible/${urlCode}/"
        
        def client = HttpClientBuilder.create().disableRedirectHandling().disableCookieManagement().build()
        
        def totalChapterCount = 0
        destDir.eachFile(FileType.DIRECTORIES) { bookDir ->
            def book = bookDir.name.substring(bookDir.name.indexOf("-") + 1)
            def chapter = 1
            while (true) {
                def chapterFile = new File(bookDir, String.format("%03d.html", chapter))
                def chapterUrl = "${chapterUrlPrefix}${book}.${chapter}.ASWDC"
                
                println("Downloading ${bookDir.name}, ${chapterFile.name} from $chapterUrl...")     
                def res = makeHttpRequest(client, chapterUrl)
                if (res) {
                    chapterFile.bytes = res
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

        def response = client.execute(req)
        
        // redirect ends scraping for current book.
        if (response.getStatusLine().getStatusCode() != 200) {
            assert response.getStatusLine().getStatusCode() == 303;
            return null
        }
        
        def responseText = EntityUtils.toString(response.entity)
        return responseText.getBytes('utf-8')
    }
}

//https://www.livelingua.com/course/fsi/Twi(Akan)_Basic_Course
//https://babel.hathitrust.org/cgi/pt?id=hvd.32044004547915&view=1up&seq=45 - christaller grammar on twi
//https://babel.hathitrust.org/cgi/pt?id=uc1.31210006441156&view=1up&seq=11 - basic course pdf