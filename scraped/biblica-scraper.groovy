@Grab(group='org.apache.httpcomponents', module='httpclient', version='4.5.2')

import groovy.io.FileType

import org.apache.http.client.methods.*
import org.apache.http.entity.*
import org.apache.http.impl.client.*
import org.apache.http.util.EntityUtils

class BiblicaScraper {

    public static void main(String[] args) {
        def destDir = new File(args[0])
        def urlCodes
        switch (destDir.name) {
            case "Twer#e Kronkron 2012":
                urlCodes = [1461, "ASWDC"]
                break
            case "Nkwa As#em 2020":
                urlCodes = [2094, "ASNA"]
                break
            default:
                assert false, destDir.name
        }
        def chapterUrlPrefix = "https://www.bible.com/bible/${urlCodes[0]}/"
        
        def client = HttpClientBuilder.create().disableRedirectHandling().disableCookieManagement().build()
        
        def totalChapterCount = 0
        destDir.eachFile(FileType.DIRECTORIES) { bookDir ->
            def bookSepIdx = bookDir.name.indexOf("-")
            def bookNum = Integer.parseInt(bookDir.name.substring(0, bookSepIdx))
            def book = bookDir.name.substring(bookSepIdx + 1)
            def chapter = 1
            while (true) {
                def chapterFile = new File(bookDir, String.format("%03d.html", chapter))
                def chapterUrl = "${chapterUrlPrefix}${book}.${chapter}.${urlCodes[1]}"
                
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
                
                // skip 2 last books of Daniel in Roman Catholic
                if (bookNum == 27 && chapter == 13) {
                    break
                }
            }
            println("Done with ${bookDir.name}.")
        }
        println()
        assert totalChapterCount == 1189
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