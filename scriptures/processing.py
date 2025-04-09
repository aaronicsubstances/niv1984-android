import html
import json
import re
import os
import xml.etree.ElementTree as ET

CHAPTER_COUNTS = [
    50, 40, 27, 36, 34,
    24, 21, 4, 31, 24,
    22, 25, 29, 36, 10,
    13, 14, 16, 16, 42,
    150, 31, 12, 8, 19,
    51, 66, 52, 5, 6,
    48, 14, 14, 3, 9,
    1, 4, 7, 3, 3,
    3, 2, 14, 4, 16,
    15, 10, 12,
    
    28, 16, 24, 21, 28, 16, 16, 13, 6, 6, 4, 4, 5, 3, 6, 4, 3, 1, 13,
    5, 5, 3, 5, 1, 1, 1, 22
]

EXCEPTION_CHAPTER_COUNTS = {
    "cpdv": {
        "ESG": 15,
        "SIR": 52, # prologue
        "LAM": 6, # prologue
    }
}

BIBLE_CODES = [
    'GEN', 'EXO', 'LEV', 'NUM', 'DEU',
    'JOS', 'JDG', 'RUT', '1SA', '2SA',
    '1KI', '2KI', '1CH', '2CH', 'EZR',
    'NEH', 'TOB', 'JDT', 'ESG', 'JOB',
    'PSA', 'PRO', 'ECC', 'SNG', 'WIS',
    'SIR', 'ISA', 'JER', 'LAM', 'BAR',
    'EZK', 'DAG', 'HOS', 'JOL', 'AMO',
    'OBA', 'JON', 'MIC', 'NAM', 'HAB',
    'ZEP', 'HAG', 'ZEC', 'MAL', '1MA',
    '2MA', 'EST', 'DAN',
    
    'MAT', 'MRK', 'LUK', 'JHN', 'ACT',
    'ROM', '1CO', '2CO', 'GAL', 'EPH',
    'PHP', 'COL', '1TH', '2TH', '1TI',
    '2TI', 'TIT', 'PHM', 'HEB', 'JAS',
    '1PE', '2PE', '1JN', '2JN', '3JN',
    'JUD', 'REV',
]

def get_chapter_count(version_tag, bcode):
    if version_tag in EXCEPTION_CHAPTER_COUNTS:
        if bcode in EXCEPTION_CHAPTER_COUNTS[version_tag]:
            return EXCEPTION_CHAPTER_COUNTS[version_tag][bcode]
    bidx = BIBLE_CODES.index(bcode)
    return CHAPTER_COUNTS[bidx]

def main(version_tag, xml_dir):
    print(f"Generating for {version_tag}...")
    for f in os.listdir(xml_dir):
        if not f.endswith(".xml"):
            continue
        bcode = os.path.splitext(os.path.basename(f))[0]
        bcode = bcode[bcode.index("-")+1:]
        fgo = f'{bcode}.html'
        print(f"processing {f} into {fgo}...")
        #fout = FoutPhantom()
        #if True:
        with open(os.path.join(xml_dir, fgo), 'w', encoding='utf8') as fout:
            fout.write("""<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width">
    <meta name="color-scheme" content="dark light">
    <title></title>
    <link href="../css/base.css" rel="stylesheet" type="text/css">
</head>
<body>
    <div id="wrapper"><div class="booktext">
        """)
            work_on_xml(bcode, version_tag, os.path.join(xml_dir, f), fout)
            fout.write("""</div></div>
    <script src="../js/jquery-3.7.1.min.js" type="text/javascript"></script>
    <script src="../js/base.js" type="text/javascript"></script>
</body>
</html>""")

def work_on_xml(bcode, version_tag, file_path, fout):
    output = {}
    tree = ET.parse(file_path)
    preferred_chapter_titles = []
    preferred_cnums = []
    output_verses = []
    output_notes = []
    for chapter in tree.getroot():
        assert chapter.tag == 'chapter', chapter.tag
        cnum = int(chapter.attrib["num"])
        preferred_cnums.append(cnum)
        
        if "preferred_title" in chapter.attrib:
            preferred_chapter_titles.append(chapter.attrib["preferred_title"])
        else:
            preferred_chapter_titles.append(('Psalm' if bcode == 'PSA' else 'Chapter') + ' ' + str(cnum))

        output_verses.append([])
        output_notes.append([])
        vnum = 0
        for verse in chapter:
            assert verse.tag in ['note', 'fragment', 'verse'], verse.tag
            if verse.tag == "verse":
                vnum = int(verse.attrib["num"])
                output_verses[-1].append((verse.tag, f"<span class='verse-num'>[{vnum}]</span> " + coalesce_verse(version_tag, verse, cnum, vnum), vnum))
            elif verse.tag == 'fragment':
                heading_present = False
                if verse.attrib.keys():
                    assert list(verse.attrib.keys()) == ['kind'], e.attrib.keys
                    assert verse.attrib['kind'] == 'HEADING', f"at {cnum}:{vnum} {verse.attrib['kind']}"
                    heading_present = True
                output_verses[-1].append(('fragment-heading' if heading_present else verse.tag, coalesce_verse(version_tag, verse, cnum, vnum)))
            else:
                assert verse.tag == 'note'
                assert list(verse.attrib.keys()) == ['num'], verse.attrib.keys()
                output_notes[-1].append(coalesce_verse(version_tag, verse, cnum, vnum, False, verse.attrib['num']))

    chapter_count = get_chapter_count(version_tag, bcode)
    assert len(preferred_chapter_titles) == chapter_count, preferred_chapter_titles
    assert len(preferred_cnums) == chapter_count, preferred_cnums

    bookmark_counter = BookmarkGen()

    for cidx in range(chapter_count):
        cnum = preferred_cnums[cidx]
        elem_id = f" id='chapter-{cnum}'" if version_tag == "cpdv" else ''
        fout.write(f"<div{elem_id}>\n")

        fout.write(f"<div>\n")

        elem_id = f" id='{version_tag}-bookmark-{bookmark_counter.inc(f"{cnum}")}'"
        ch_title = preferred_chapter_titles[cidx]
        fout.write(f"<h2{elem_id}>{ch_title}</h2>\n")
        for item in output_verses[cidx]:
            item_attrs = []
            if item[0] == 'fragment':
                bookmark_desc = 'f'
                item_attrs.append("class='fragment'")
            elif item[0] == 'fragment-heading':
                bookmark_desc = 'h'
                item_attrs.append("class='fragment heading'")
            else:
                assert item[0] == 'verse'
                bookmark_desc = f"{cnum}-{item[2]}"
                item_attrs.append("class='verse'")
            item_attrs.append(f"id='{version_tag}-bookmark-{bookmark_counter.inc(bookmark_desc)}'")
            item_attrs = "".join((" " + a) for a in item_attrs)
            fout.write(f"<div{item_attrs}>{item[1]}</div>\n")

        # add footnotes
        elem_id = f" id='{version_tag}-bookmark-{bookmark_counter.inc('n')}'" if bookmark_counter else ''
        fout.write(f"<div{elem_id}>\n")
        fout.write(f"<hr>\n")
        for item in output_notes[cidx]:
            fout.write(f"<div>{item}</div>\n")
        fout.write(f"<hr>\n")
        fout.write("</div>\n")

        fout.write(f"</div>\n")
        fout.write("</div>\n")

    bookmarks = []
    for value in bookmark_counter.acc:
        sepIdx = value.index('-')
        bmDesc = value[sepIdx+1:]
        if version_tag == "cpdv" and re.fullmatch(r'\d+', bmDesc):
            bookmarks.append(f'chapter-{bmDesc}')
        bookmarks.append(f'{version_tag}-bookmark-{value}')

    fout.write(f"<span class='bookmarks' style='display:none'>{html.escape(json.dumps(bookmarks))}</span>")
    
def coalesce_verse(version_tag, v, cnum, vnum, wj=False, noteref=None):
    text = []
    clsw = ' class="wj"' if wj else ''
    for e in v:
        if not wj and e.tag == 'wj':
            text.append(coalesce_verse(version_tag, e, cnum, vnum, True))
            continue
        if e.tag == 'note_ref':
            text.append(f'<sup>[<a href="#{version_tag}-footnote-{cnum}-{e.text}" id="{version_tag}-verse-{cnum}-{e.text}">{chr(int(e.text) + 96)}</a>]</sup>')
            continue
        assert e.tag == 'content', e.tag
        if not e.text:
            continue
        if not e.text.strip():
            text.append(e.text)
            continue
        if e.attrib.keys():
            assert list(e.attrib.keys()) == ['kind'], e.attrib.keys()
            if e.attrib['kind'] == 'PICTOGRAM':
                assert cnum == 119
                assert vnum in [0, 8, 16, 24, 32, 40, 48, 56, 64, 72, 80, 88, 96, 104, 112, 120, 128, 136, 144, 152, 160, 168], vnum
                text.append(f"<span class='pictogram'>{e.text}</span>")
            elif e.attrib['kind'] == 'REF_VERSE_START':
                text.append(f"[{e.text}]")
            else:
                assert e.attrib['kind'] == 'EM', f"at {cnum}:{vnum} {e.attrib['kind']}"
                text.append(f"<i{clsw}>{e.text}</i>")
        else:
            text.append(f"<span{clsw}>{e.text}</span>")
    if noteref:
        text.insert(0, f"<a id='{version_tag}-footnote-{cnum}-{noteref}' href='#{version_tag}-verse-{cnum}-{noteref}'>{chr(int(noteref) + 96)}</a>. ")
    return ''.join(text)
    
class BookmarkGen:
    def __init__(self):
        self.cnt = 0
        self.acc = []
    def inc(self, desc):
        self.cnt+=1
        self.acc.append(f"{self.cnt}-{desc}")
        return f"{self.cnt}-{desc}"
    
class FoutPhantom:
    def write(self, s):
        pass

if __name__ == '__main__':
    main("cpdv", "cpdv-xml")
    main("drb1899", "drb1899-xml")
    main("bbe1965", "bbe1965-xml")
