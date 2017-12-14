/*
 *  (c) Aaronic Substances.
 */
package com.aaronicsubstances.nivbible.preprocessor;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 *
 * @author Aaron
 */
public class NewMain {
    private static int rank = 1;
    
    private static void appendMain(StringBuilder mainOut,
            String bname, int bnum, int cnum,
            int vnum, Object html)
    {
        mainOut.append(bnum < 40 ? 'O' : 'N');
        mainOut.append('\t');
        mainOut.append(bname);
        mainOut.append('\t');
        mainOut.append(bnum);
        mainOut.append('\t');
        mainOut.append(cnum);
        mainOut.append('\t');
        mainOut.append(vnum);
        mainOut.append('\t');
        String h = getMysqlRealScapeString(html.toString());
        mainOut.append(h.isEmpty() ? "\\N" : h);
        mainOut.append('\n');
    }
    
    public static String getMysqlRealScapeString(String str) {
        String data = null;
        if (str != null) {
            str = str.replace("\\", "\\\\");
            str = str.replace("'", "\\'");
            str = str.replace("\0", "\\0");
            str = str.replace("\n", "\\n");
            str = str.replace("\r", "\\r");
            str = str.replace("\"", "\\\"");
            str = str.replace("\\x1a", "\\Z");
            data = str;
        }
        return data;
    }
    
    private static void appendAux(StringBuilder auxOut,
            String bname, int bnum, int cnum,
            int vnum, Object html, String footnoteId)
    {
        auxOut.append(rank++);
        auxOut.append('\t');
        auxOut.append(bnum < 40 ? 'O' : 'N');
        auxOut.append('\t');
        auxOut.append(bname);
        auxOut.append('\t');
        auxOut.append(bnum);
        auxOut.append('\t');
        auxOut.append(cnum);
        auxOut.append('\t');
        auxOut.append(vnum);
        auxOut.append('\t');
        String h = getMysqlRealScapeString(html.toString());
        auxOut.append(h.isEmpty() ? "\\N" : h);
        auxOut.append('\t');
        auxOut.append(footnoteId.isEmpty() ? "\\N" : footnoteId);
        auxOut.append('\n');
    }
    
    public static void transformHtml(String inputPath,
            StringBuilder mainOut, StringBuilder auxOut) throws Exception {
        File inputFile = new File(inputPath);
        
        String chap = inputFile.getName().replace(".html", "");
        int cnum = Integer.parseInt(chap, 10);
        
        String bk = inputFile.getCanonicalFile().getParentFile().getName();
        int hyphenIndex = bk.indexOf('-');
        int bnum = Integer.parseInt(bk.substring(0,hyphenIndex), 10);
        String bname = bk.substring(hyphenIndex+1);
        
        Document doc = Jsoup.parse(inputFile, null);
        
        String chapTitle = null;
        
        for (Element bodyChildElem : doc.body().children()) {
            if (bodyChildElem.tagName().equalsIgnoreCase("b")) {
                chapTitle = bodyChildElem.text().trim();
                if (chap.equals("001")) {
                    if (bk.equals("01-GEN")) {
                        appendAux(auxOut, bname, bnum, cnum, 1, 
                                new Element("h1").text("Old Testament"), "");
                    }
                    else if (bk.equals("40-MATT")) {
                        appendAux(auxOut, bname, bnum, cnum, 1,
                                new Element("h1").text("New Testament"), "");
                    }
                }
                appendAux(auxOut, bname, bnum, cnum, 1, 
                        new Element("h3").text(chapTitle), "");
            }
            else if (bodyChildElem.tagName().equalsIgnoreCase("dl")) {
                // the verses. transform dl into ol.
                // transform dd into li.
                // used to skip dt but due to NIV's omission of some
                // verses such as Matthew 18:11, more care has to
                // be taken.
                int verseIndex = 1;
                for (Element dlChildElem : bodyChildElem.children()) {
                    if (dlChildElem.tagName().equalsIgnoreCase("dt")) {
                        int v = Integer.parseInt(dlChildElem.text());
                        if (v != verseIndex+1) {
                            // May have to generate more than 1 to match
                            // up with new verse, for example if progressing
                            // from Matthew 18:9 to Matthew 18:11.
                            while (verseIndex+1 < v) {
                                verseIndex++;
                                System.out.println("Generating extra verse for chap " +
                                        + cnum + " bk " + bk);
                                appendMain(mainOut, bname, bnum, cnum, verseIndex,
                                        "");
                            }
                        }
                        verseIndex = v;
                    }
                    else if (dlChildElem.tagName().equalsIgnoreCase("dd")) {
                        // Peculiar to Psalms.
                        if (bnum == 19) {
                            if (verseIndex == 1) {
                                String html = dlChildElem.html().trim();
                                if (html.startsWith(chapTitle)) {
                                    html = html.substring(chapTitle.length()).trim();

                                    if (html.length() > 0) {
                                        Element span= new Element("span").html(html);
                                        appendAux(auxOut, bname, bnum, cnum, 1, span, "");
                                        continue;
                                    }
                                }
                            }                            
                        }
                        // Peculiar to Psalm 119.
                        if (bnum == 19 && cnum == 119) {
                            if (verseIndex-1 == 0*8)
                            {
                                appendAux(auxOut, bname, bnum,
                                    cnum, verseIndex, "<h5>Aleph</h5>", "");                                     
                            }
                            if (verseIndex-1 == 1*8)
                            {
                                appendAux(auxOut, bname, bnum,
                                    cnum, verseIndex, "<h5>Beth</h5>", "");                                     
                            }
                            if (verseIndex-1 == 2*8)
                            {
                                appendAux(auxOut, bname, bnum,
                                    cnum, verseIndex, "<h5>Gimel</h5>", "");                                     
                            }
                            if (verseIndex-1 == 3*8)
                            {
                                appendAux(auxOut, bname, bnum,
                                    cnum, verseIndex, "<h5>Daleth</h5>", "");                                     
                            }
                            if (verseIndex-1 == 4*8)
                            {
                                appendAux(auxOut, bname, bnum,
                                    cnum, verseIndex, "<h5>He</h5>", "");                                     
                            }
                            if (verseIndex-1 == 5*8)
                            {
                                appendAux(auxOut, bname, bnum,
                                    cnum, verseIndex, "<h5>Waw</h5>", "");                                     
                            }
                            if (verseIndex-1 == 6*8)
                            {
                                appendAux(auxOut, bname, bnum,
                                    cnum, verseIndex, "<h5>Zayin</h5>", "");                                     
                            }
                            if (verseIndex-1 == 7*8)
                            {
                                appendAux(auxOut, bname, bnum,
                                    cnum, verseIndex, "<h5>Heth</h5>", "");                                     
                            }
                            if (verseIndex-1 == 8*8)
                            {
                                appendAux(auxOut, bname, bnum,
                                    cnum, verseIndex, "<h5>Teth</h5>", "");                                     
                            }
                            if (verseIndex-1 == 9*8)
                            {
                                appendAux(auxOut, bname, bnum,
                                    cnum, verseIndex, "<h5>Yodh</h5>", "");                                     
                            }
                            if (verseIndex-1 == 10*8)
                            {
                                appendAux(auxOut, bname, bnum,
                                    cnum, verseIndex, "<h5>Kaph</h5>", "");                                     
                            }
                            if (verseIndex-1 == 11*8)
                            {
                                appendAux(auxOut, bname, bnum,
                                    cnum, verseIndex, "<h5>Lamedh</h5>", "");                                     
                            }
                            if (verseIndex-1 == 12*8)
                            {
                                appendAux(auxOut, bname, bnum,
                                    cnum, verseIndex, "<h5>Mem</h5>", "");                                     
                            }
                            if (verseIndex-1 == 13*8)
                            {
                                appendAux(auxOut, bname, bnum,
                                    cnum, verseIndex, "<h5>Nun</h5>", "");                                     
                            }
                            if (verseIndex-1 == 14*8)
                            {
                                appendAux(auxOut, bname, bnum,
                                    cnum, verseIndex, "<h5>Samekh</h5>", "");                                     
                            }
                            if (verseIndex-1 == 15*8)
                            {
                                appendAux(auxOut, bname, bnum,
                                    cnum, verseIndex, "<h5>Ayin</h5>", "");                                     
                            }
                            if (verseIndex-1 == 16*8)
                            {
                                appendAux(auxOut, bname, bnum,
                                    cnum, verseIndex, "<h5>Pe</h5>", "");                                     
                            }
                            if (verseIndex-1 == 17*8)
                            {
                                appendAux(auxOut, bname, bnum,
                                    cnum, verseIndex, "<h5>Tsadhe</h5>", "");                                     
                            }
                            if (verseIndex-1 == 18*8)
                            {
                                appendAux(auxOut, bname, bnum,
                                    cnum, verseIndex, "<h5>Qoph</h5>", "");                                     
                            }
                            if (verseIndex-1 == 19*8)
                            {
                                appendAux(auxOut, bname, bnum,
                                    cnum, verseIndex, "<h5>Resh</h5>", "");                                     
                            }
                            if (verseIndex-1 == 20*8)
                            {
                                appendAux(auxOut, bname, bnum,
                                    cnum, verseIndex, "<h5>Sin and Shin</h5>", "");                                     
                            }
                            if (verseIndex-1 == 21*8)
                            {
                                appendAux(auxOut, bname, bnum,
                                    cnum, verseIndex, "<h5>Taw</h5>", "");                                     
                            }
                        }
                        appendMain(mainOut, bname, bnum, cnum, verseIndex,
                                dlChildElem.html());
                    }
                }
            }
            else if (bodyChildElem.tagName().equalsIgnoreCase("ol")) {
                // will happen twice. The second one is expected to be empty.
                // transform text of li's to turn verse numbers into links.
                for (Element olChildEl : bodyChildElem.children()) {
                    if (olChildEl.tagName().equalsIgnoreCase("li")) {
                        Element aLink = olChildEl.child(0);
                        String fragId = aLink.attr("name");
                        aLink.removeAttr("name");
                        String linkHtml = aLink.html();
                        
                        int vnum = 0;
                        Matcher verseNumMatcher = Pattern.compile("\\[(\\d+)\\]").matcher(linkHtml);
                        if (verseNumMatcher.find()) {
                            String verseNum = verseNumMatcher.group(1);
                            vnum = Integer.parseInt(verseNum, 10);
                            linkHtml = linkHtml.substring(verseNumMatcher.end());
                        }
                        
                        appendAux(auxOut, bname, bnum, cnum, vnum, linkHtml, 
                                fragId);
                    }
                }
            }
            else {
                // ignore line breaks.
            }
        }
    }
    
    public static void main(String[] args) throws Exception {
        File inputDir = new File(System.getProperty("user.dir"));
        if (args.length > 0) {
            inputDir = new File(args[0]).getCanonicalFile();
        }
        File outputDir = new File(inputDir, "_transformed");
        if (args.length > 1) {
            outputDir = new File(args[1]).getCanonicalFile();
        }
        outputDir.mkdirs();
        
        StringBuilder mainOut = new StringBuilder(),
                auxOut = new StringBuilder();
        for (File bkDir : inputDir.listFiles()) {
            if (!bkDir.isDirectory()|| !bkDir.getName().matches("\\d{2}-\\w{1,8}")) {
                continue;
            }
            
            for (File chapFile : bkDir.listFiles()) {
                if (!chapFile.isFile() || !chapFile.getName().matches("\\d{3}\\.html")) {
                    continue;
                }
                System.out.format("Transforming %s...\n", chapFile);                
                transformHtml(chapFile.getPath(), mainOut, auxOut);
            }
        }
        
        System.out.println();
        
        String outputPath = new File(outputDir, "niv.txt").getPath();
        File outF = new File(outputPath);
        System.out.format("Generating %s...\n", outputPath);
        FileUtils.write(outF, mainOut.toString());
        
        outputPath = new File(outputDir, "niv_extra.txt").getPath();
        outF = new File(outputPath);
        System.out.format("Generating %s...\n", outputPath);
        FileUtils.write(outF, auxOut.toString());
            
        System.out.println("\nDone.");
    }    
    
    // This is as first used in the android app.
    private static String getBookTitle(int oneBasedIndex)
    {
        switch (oneBasedIndex) {
            case 1:
                return "Genesis";
            case 2:
                return "Exodus";
            case 3:
                return "Leviticus";
            case 4:
                return "Numbers";
            case 5:
                return "Deuteronomy";
            case 6:
                return "Joshua";
            case 7:
                return "Judges";
            case 8:
                return "Ruth";
            case 9:
                return "1 Samuel";
            case 10:
                return "2 Samuel";
            case 11:
                return "1 Kings";
            case 12:
                return "2 Kings";
            case 13:
                return "1 Chronicles";
            case 14:
                return "2 Chronicles";
            case 15:
                return "Ezra";
            case 16:
                return "Nehemiah";
            case 17:
                return "Esther";
            case 18:
                return "Job";
            case 19:
                return "Psalms";
            case 20:
                return "Proverbs";
            case 21:
                return "Ecclesiastes";
            case 22:
                return "Songs of Solomon";
            case 23:
                return "Isaiah";
            case 24:
                return "Jeremiah";
            case 25:
                return "Lamentations";
            case 26:
                return "Ezekiel";
            case 27:
                return "Daniel";
            case 28:
                return "Hosea";
            case 29:
                return "Joel";
            case 30:
                return "Amos";
            case 31:
                return "Obadiah";
            case 32:
                return "Jonah";
            case 33:
                return "Micah";
            case 34:
                return "Nahum";
            case 35:
                return "Habakkuk";
            case 36:
                return "Zephaniah";
            case 37:
                return "Haggai";
            case 38:
                return "Zechariah";
            case 39:
                return "Malachi";
            case 40:
                return "Matthew";
            case 41:
                return "Mark";
            case 42:
                return "Luke";
            case 43:
                return "John";
            case 44:
                return "Acts";
            case 45:
                return "Romans";
            case 46:
                return "1 Corinthians";
            case 47:
                return "2 Corinthians";
            case 48:
                return "Galatians";
            case 49:
                return "Ephesians";
            case 50:
                return "Philippians";
            case 51:
                return "Colossians";
            case 52:
                return "1 Thessalonians";
            case 53:
                return "2 Thessalonians";
            case 54:
                return "1 Timothy";
            case 55:
                return "2 Timothy";
            case 56:
                return "Titus";
            case 57:
                return "Philemon";
            case 58:
                return "Hebrews";
            case 59:
                return "James";
            case 60:
                return "1 Peter";
            case 61:
                return "2 Peter";
            case 62:
                return "1 John";
            case 63:
                return "2 John";
            case 64:
                return "3 John";
            case 65:
                return "Jude";
            case 66:
                return "Revelations";
            default:
                throw new RuntimeException("Unexpected book: " + oneBasedIndex);
        }
    }
}
