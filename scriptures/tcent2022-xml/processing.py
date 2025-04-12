import json
import os
import re
from types import SimpleNamespace
import xml.etree.ElementTree as ET

def adjust():
    with open('ANT-footnotes.txt', encoding='utf8') as fp:
        replacements = fp.readlines()
    for fname in os.listdir("xml"):
        if not fname.endswith(".xml"):
            continue
        print(f"adjusting {fname}...")
        adjustOne(fname, replacements)
        
def adjustOne(destName, replacements):
    tree = ET.parse(os.path.join("xml", destName))
    destRoot = tree.getroot()
    startIdx = 0
    for lineIdx in range(len(replacements)):
        line = replacements[lineIdx].strip()
        if line == f"processing {destName}...":
            print(f"start found: {lineIdx}")
            startIdx = lineIdx + 1
            break
    endIdx = startIdx
    while endIdx < len(replacements):
        line = replacements[endIdx].strip()
        if not line or line.startswith(f"processing "):
            break
        endIdx += 1
    print(f"end found: {endIdx}")
    
    sep = "¦"
    textReplacements = {}
    for i in range(startIdx, endIdx):
        line = replacements[i].strip()
        if line.startswith("#"):
            print(f"skipping {line}...")
            continue
        if line.startswith(">"):
            parts = line[1:].split(sep)
            assert len(parts) == 2
            q, r = parts[0].strip(), parts[1].strip()            
            if r == "—":
                r = ""
            else:
                validateReplacement(r)
            assert q not in textReplacements
            for k in textReplacements.keys():
                assert not k.lower().startswith(q.lower()), q
                assert not q.lower().startswith(k.lower()), q
            textReplacements[q] = r
            continue
        m = re.match(r'(\d+):(\d+) ', line)
        assert m, line
        cnum, vnum = m.group(1), m.group(2)
        print(f" - adjusting {cnum}:{vnum}...")
        line = line[m.end():].strip()
        assert line.count(sep) == 1, line
        parts = line.split(sep)
        assert len(parts) == 2
        q, r = parts[0].strip(), parts[1].strip()
        assert r.count("ANT") == 1, r
        r = r[:r.rindex("ANT")].strip()
        if r == "—":
            r = ""
        else:
            validateReplacement(r)
        contentEl = locateContentForVerse(destRoot, cnum, vnum)
        current = contentEl.text
        assert contentEl.text.lower().count(q.lower()) == 1, f"{json.dumps(q)} not found exactly once in {json.dumps(contentEl.text)}"
        contentEl.text = normalize_spaces(contentEl.text.replace(q, r))

    #ET.indent(destRoot)
    #tree = ET.ElementTree(destRoot)
    with open(os.path.join("adjusted", destName), 'wb') as fp:
        tree.write(fp, xml_declaration=True, encoding='utf-8')

    if not textReplacements:
        return

    with open(os.path.join("adjusted", destName), encoding='utf8') as fp:
        entireText = fp.read()

    for r in textReplacements:
        assert entireText.count(r) == 1, r

    keysForPattern = (re.escape(k) for k in textReplacements.keys())
    pattern = re.compile('|'.join(keysForPattern))
    entireText = pattern.sub(lambda x: textReplacements[x.group()], entireText)

    with open(os.path.join("adjusted", destName), 'w', encoding='utf8') as fp:
        fp.write(entireText)

def overlap(start1, end1, start2, end2):
    """Does the range (start1, end1) overlap with (start2, end2)?"""
    return end1 >= start2 and end2 >= start1

def validateReplacement(r):
    for c in r:
        assert c != '%'
        if ord(c) > 125:
            assert c in ["ʋ", "‘", "’", "”", "“"], c
    for code in ["ANT", "BYZ", "CT", "HF", "PCK", "TR", "SCR", "ST", "ECM"]:
        assert code not in r, code
    
def locateContentForVerse(rootEl, cnum, vnum):
    el = None
    for child in rootEl:
        if child.get("num") == cnum:
            el = child
            break
    assert el is not None, f"chapter element not found for {cnum}:{vnum}"
    verseEl = None
    for child in el:
        if child.get("num") == vnum:
            verseEl = child
            break
    assert verseEl is not None, f"verse element not found for {cnum}:{vnum}"
    el = verseEl.find("content")
    assert el is not None
    return el

def main():
    with open('engtcent_usfx.xml', encoding='utf8') as fp:
        tree = ET.parse(fp)
        
    ANTfp = open('ANT-footnotes.txt', 'w', encoding='utf8')
    root = tree.getroot()
    bk_idx = 0
    for book in root.iter('book'):
        if book.get("id") == "INT" or book.get("id").startswith("XX"):
            continue
        if book.get("id") != "MAT":
            pass
            #continue
        bk_idx += 1
        destName = f"{bk_idx:02d}-{book.get("id")}.xml"
        print(f"\nprocessing {destName}...")
        ANTfp.write(f"\nprocessing {destName}...\n")
        data = None
        
        destRoot = ET.Element('book')
        for child in book:
            assert child.tag in ['id', 'h', 'toc', 'p', 'c', 's', 'b', 'q']
            if child.tag not in ['p', 'c', 's', 'b', 'q']:
                continue
            if child.tag == 'p' and 'sfm' in child.attrib and child.get('sfm') == 'mt':
                continue
            #print(child.tag)
            assert not (child.tail if child.tail is None else child.tail.strip()), child.tail
            if child.tag == 'c':
                assert list(child.attrib.keys()) == ["id"]
                if data:
                    assert not data.verses[-1]
                    destChapter = ET.SubElement(destRoot, "chapter")
                    destChapter.attrib["num"] = str(cnum)
                    for v in data.verses[:-1]:
                        if str(v[0]) == "0":
                            destVerse = ET.SubElement(destChapter, "fragment")
                            destVerse.attrib["kind"] = "HEADING"
                        else:
                            destVerse = ET.SubElement(destChapter, "verse")
                            destVerse.attrib["num"] = str(v[0])
                        destContent = ET.SubElement(destVerse, "content")
                        destContent.text = v[1]
                        #print(f"{v[0]} {v[1]}")
                data = SimpleNamespace(verses=[])
                cnum = int(child.get("id"))
                #print(cnum)
            elif child.tag == 's':
                assert list(child.attrib.keys()) == ["style"]
                assert child.get("style") == "s"
                heading = normalize_spaces("".join(child.itertext()))
                if not data.verses:
                    data.verses.append([0, heading])
                    data.verses.append([])
                elif not data.verses[-1]:
                    data.verses[-1] = [0, heading]
                    data.verses.append([])
                else:
                    vnum = data.verses[-1][0]
                    data.verses[-1][0] = str(vnum) + "-"
                    data.verses[-1][-1] = normalize_spaces(data.verses[-1][-1])
                    data.verses.append([0, heading])
                    data.verses.append(["-" + str(vnum), ''])
                #print(heading)
            elif child.tag == 'p':
                assert child.get("style") in ["p", "m", "nb"], child.attrib
                if child.get("style") == 'm':
                    assert child.attrib == {'sfm':'m', 'style':'m'}, child.attrib
                elif child.get("style") == 'nb':
                    assert child.attrib == {'sfm':'nb', 'style':'nb'}, child.attrib
                else:
                    assert child.attrib == {'style':'p'}, child.attrib
                processing_verse_block(child, data, ANTfp)
            elif child.tag == 'q':
                assert list(child.attrib.keys()) == ["style"], child.attrib
                assert child.get("style") == "q", child.attrib
                processing_verse_block(child, data, ANTfp)
        assert not data.verses[-1]
        destChapter = ET.SubElement(destRoot, "chapter")
        destChapter.attrib["num"] = str(cnum)
        for v in data.verses[:-1]:
            if str(v[0]) == "0":
                destVerse = ET.SubElement(destChapter, "fragment")
                destVerse.attrib["kind"] = "HEADING"
            else:
                destVerse = ET.SubElement(destChapter, "verse")
                destVerse.attrib["num"] = str(v[0])
            destContent = ET.SubElement(destVerse, "content")
            destContent.text = v[1]
            #print(f"{v[0]} {v[1]}")
            
        ET.indent(destRoot)
        tree = ET.ElementTree(destRoot)
        with open(os.path.join("xml", destName), 'wb') as fp:
            tree.write(fp, xml_declaration=True, encoding='utf-8')

    ANTfp.close()

def processing_verse_block(elem, data, ANTfp):
    if elem.text:
        data.verses[-1][-1] += elem.text
    for child in elem:
        assert child.tag in ["v", "ve", "w", "f", "wj", "sup"], child.tag
        if child.tag == 'wj':
            if child.text:
                data.verses[-1][-1] += child.text
            for grandChild in child:
                assert grandChild.tag in ["v", "ve", "w"], child.tag
                if grandChild.tag == 'v':
                    assert not data.verses or not data.verses[-1]
                    vnum = int(grandChild.get("id"))
                    if data.verses:
                        data.verses[-1] = [vnum, '']
                    else:
                        data.verses.append([vnum, ''])
                elif grandChild.tag == 've':
                    data.verses[-1][-1] = normalize_spaces(data.verses[-1][-1])
                    data.verses.append([])
                else:
                    data.verses[-1][-1] += grandChild.text
                if grandChild.tail:
                    data.verses[-1][-1] += grandChild.tail
        elif child.tag == 'v':
            assert not data.verses or not data.verses[-1]
            vnum = int(child.get("id"))
            if data.verses:
                data.verses[-1] = [vnum, '']
            else:
                data.verses.append([vnum, ''])
        elif child.tag == 've':
            data.verses[-1][-1] = normalize_spaces(data.verses[-1][-1])
            data.verses.append([])
        elif child.tag == 'w':
            data.verses[-1][-1] += child.text
        elif child.tag == 'f':
            processing_footnote_block(child, ANTfp)
        if child.tail:
            data.verses[-1][-1] += child.tail

def processing_footnote_block(elem, ANTfp):
    text = normalize_spaces("".join(elem.itertext()))
    assert re.match(r'\d+:\d+', text), text
    if "¦" in text and "ANT" in text:
        ANTfp.write(text + "\n")

def normalize_spaces(s, trim=True):
    s = re.sub(r'\s+', ' ', s)
    if trim:
        s = s.strip()
    return s

if __name__ == "__main__":
    #main()
    adjust()
