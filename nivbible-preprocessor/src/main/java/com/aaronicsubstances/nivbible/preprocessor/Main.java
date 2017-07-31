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
public class Main {
    public static void transformHtml(String inputPath, String outputPath) throws Exception {
        File inputFile = new File(inputPath);
        String chap = inputFile.getName().replace(".html", "");
        String bk = inputFile.getCanonicalFile().getParentFile().getName();
        Document doc = Jsoup.parse(inputFile, null);
        Element wrapperDiv = new Element("div");
        wrapperDiv.attr("id", createChapFragId(bk, chap));
        for (Element bodyChildElem : doc.body().children()) {
            if (bodyChildElem.tagName().equalsIgnoreCase("b")) {
                String chapText = bodyChildElem.text();
                if (chap.equals("001")) {
                    if (bk.equals("01-GEN")) {
                        wrapperDiv.appendElement("h1").text("Old Testament");
                    }
                    else if (bk.equals("40-MATT")) {
                        wrapperDiv.appendElement("h1").text("New Testament");                        
                    }
                    wrapperDiv.appendElement("h2").text(chapText.substring(0,
                            chapText.lastIndexOf(" ")));
                }
                wrapperDiv.appendElement("h3").text(chapText);
            }
            else if (bodyChildElem.tagName().equalsIgnoreCase("dl")) {
                // the verses. transform dl into ol.
                // skip dt, and transform dd into li.
                Element versesElem = wrapperDiv.appendElement("ol");
                int verseIndex = 1;
                for (Element dlChildElem : bodyChildElem.children()) {
                    if (dlChildElem.tagName().equalsIgnoreCase("dd")) {
                        Element verseElem = versesElem.appendElement("li");
                        // Create id before appending contents.
                        String fragmentId = createVerseFragId(bk, chap, verseIndex);
                        verseElem.attr("id", fragmentId).append(dlChildElem.html());
                        
                        verseIndex++;
                    }
                }
            }
            else if (bodyChildElem.tagName().equalsIgnoreCase("ol")) {
                // will happen twice. The second one is expected to be empty.
                // transform text of li's to turn verse numbers into links.
                wrapperDiv.appendElement("hr");
                Element footNotesElem = wrapperDiv.appendElement("ol");
                for (Element olChildEl : bodyChildElem.children()) {
                    if (olChildEl.tagName().equalsIgnoreCase("li")) {
                        Element footNoteElem = footNotesElem.appendElement("li");
                        Element aLink = olChildEl.child(0);
                        String fragId = aLink.attr("name");
                        aLink.removeAttr("name");
                        footNoteElem.attr("id", fragId);
                        String linkHtml = aLink.html();
                        Matcher verseNumMatcher = Pattern.compile("\\[(\\d+)\\]").matcher(linkHtml);
                        if (verseNumMatcher.find()) {
                            String verseNum = verseNumMatcher.group(1);
                            Element backLink = createVerseLink(verseNumMatcher.group(), 
                                    bk, chap, verseNum);
                            footNoteElem.appendChild(backLink);
                            linkHtml = linkHtml.substring(verseNumMatcher.end());
                        }
                        footNoteElem.append(linkHtml);
                    }
                }
            }
            else {
                // ignore line breaks.
            }
        }
        File outF = new File(outputPath);
        outF.getParentFile().mkdirs();
        FileUtils.write(outF, wrapperDiv.toString());
    }
    
    private static String createChapFragId(String bk, String chap) {
        return String.format("%s-%s", bk, chap);
    }
    
    private static String createVerseFragId(String bk, String chap, Object verseNum) {
        return String.format("%s-%s-%s", bk, chap, verseNum);
    }

    private static Element createVerseLink(String linkText, String bk, String chap, String verse) {
        Element a = new Element("a").attr("href", 
                String.format("#%s", createVerseFragId(bk, chap, verse)))
                .append(linkText);
        return a;
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
        for (File f : FileUtils.listFiles(inputDir, new String[]{"html"}, true)) {
            String s = f.getPath().substring(inputDir.getPath().length() + 1);
            System.out.format("Transforming %s\n...", s);
            String outputPath = new File(outputDir, s).getPath();
            transformHtml(f.getPath(), outputPath);
        }
        System.out.println("Done.");
    }
}
