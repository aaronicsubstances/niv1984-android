import xml.etree.ElementTree as ET
import os

CHAPTER_COUNTS = [
    50, 40, 27, 36, 34, 24, 21, 4, 31, 24, 22, 25, 29, 36, 10, 13, 10, 42, 150,
    31, 12, 8, 66, 52, 5, 48, 12, 14, 3, 9, 1, 4, 7, 3, 3, 3, 2, 14, 4,
    28, 16, 24, 21, 28, 16, 16, 13, 6, 6, 4, 4, 5, 3, 6, 4, 3, 1, 13,
    5, 5, 3, 5, 1, 1, 1, 22
]

def main():
    version_tag = "kjv" if "kjv" in os.getcwd() else "niv"
    print(f"Generating for {[version_tag]}...")
    for f in os.listdir():
        if not f.endswith(".xml"):
            continue
        bnum = int(os.path.splitext(os.path.basename(f))[0])
        fgo = f'{f[:-4]}-{version_tag}.html'
        print(f"processing {f} into {fgo}...")
        #fout = FoutPhantom()
        #if True:
        with open(fgo, 'w', encoding='utf8') as fout:
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
    <div id="wrapper">
        """)
            work_on_xml(bnum, [version_tag], [f], fout)
            fout.write("""</div>
    <script src="../js/jquery-3.7.1.min.js" type="text/javascript"></script>
    <script src="../js/base.js" type="text/javascript"></script>
</body>
</html>""")

def main2():
    version_tag = "kjv" if "kjv" in os.getcwd() else "niv"
    version_tags = ["niv", "kjv"]
    print(f"Generating for {version_tags}...")
    for f in os.listdir():
        if not f.endswith(".xml"):
            continue
        bnum = int(os.path.splitext(os.path.basename(f))[0])
        fgo = f'{f[:-4]}-{"-".join(version_tags)}.html'
        print(f"processing {f} into {fgo}...")
        #fout = FoutPhantom()
        #if True:
        with open(fgo, 'w', encoding='utf8') as fout:
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
    <div id="wrapper">
        """)
            if version_tag == 'kjv':
                related_f = os.path.join("..", "niv1984-xml", f)
                # swap so niv comes first
                related_f, f = f, related_f
            else:
                related_f = os.path.join("..", "kjv1769-xml", f)
            work_on_xml(bnum, version_tags, [f, related_f], fout)
            fout.write("""</div>
    <script src="../js/jquery-3.7.1.min.js" type="text/javascript"></script>
    <script src="../js/base.js" type="text/javascript"></script>
</body>
</html>""")

def work_on_xml(bnum, version_tags, fs, fout):
    output = {}
    for i in range(len(version_tags)):
        version_tag = version_tags[i]
        tree = ET.parse(fs[i])
        output_verses = []
        output_notes = []
        for chapter in tree.getroot():
            assert chapter.tag == 'chapter', chapter.tag
            cnum = int(chapter.attrib["num"])
            output_verses.append([])
            output_notes.append([])
            vnum = 0
            for verse in chapter:
                assert verse.tag in ['note', 'fragment', 'verse'], verse.tag
                if verse.tag == "verse":
                    vnum = int(verse.attrib["num"])
                    output_verses[-1].append((verse.tag, f"[{vnum}] " + coalesce_verse(version_tag, verse, cnum, vnum)))
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
        output[version_tag] = {
            'v': output_verses,
            'n': output_notes
        }

    gridCls, gridClsStart, gridClsEnd = '', '', ''
    if len(version_tags) > 1:
        gridCls = ' class="flex-container"'
        gridClsStart = ' class="flex-item">\n<div class="flex-item-inner"'
        gridClsEnd = '>\n</div'
    bookmark_counts = []
    for version_tag in version_tags:
        bookmark_counts.append(version_tag)
        bookmark_counts.append(BookmarkGen())
    for cnum in range(1, CHAPTER_COUNTS[bnum-1] + 1):
        fout.write(f"<div id='chapter-{cnum}'{gridCls}>\n")
        for version_tag in version_tags:
            output_verses = output[version_tag]['v']
            output_notes = output[version_tag]['n']
            bookmark_counter = None
            for j in range(len(bookmark_counts)):
                if bookmark_counts[j] == version_tag:
                    bookmark_counter = bookmark_counts[j+1]
                    break
            assert bookmark_counter

            fout.write(f"<div{gridClsStart}>\n")

            fout.write(f"<h2 id='{version_tag}-bookmark-{bookmark_counter.inc(f"{cnum}")}'>{'Psalm' if bnum == 19 else 'Chapter'} {cnum}</h2>\n")
            for item in output_verses[cnum-1]:
                item_attrs = []
                if item[0] == 'fragment':
                    bookmark_desc = 'f'
                    item_attrs.append("class='fragment'")
                elif item[0] == 'fragment-heading':
                    bookmark_desc = 'h'
                    item_attrs.append("class='fragment heading'")
                else:
                    assert item[0] == 'verse'
                    bookmark_desc = f"{cnum}:{item[1][item[1].index('[')+1:item[1].index(']')]}"
                    item_attrs.append("class='verse'")
                item_attrs.append(f"id='{version_tag}-bookmark-{bookmark_counter.inc(bookmark_desc)}'")
                item_attrs = "".join((" " + a) for a in item_attrs)
                fout.write(f"<div{item_attrs}>{item[1]}</div>\n")

            # add footnotes
            fout.write(f"<div id='{version_tag}-bookmark-{bookmark_counter.inc('n')}'>\n")
            fout.write(f"<hr>\n")
            for item in output_notes[cnum-1]:
                fout.write(f"<div>{item}</div>\n")
            fout.write(f"<hr>\n")
            fout.write("</div>\n")

            fout.write(f"</div{gridClsEnd}>\n")
        fout.write("</div>\n")

    bookmarks = []
    for j in range(0, len(bookmark_counts), 2):
        bookmarks.append([bookmark_counts[j], bookmark_counts[j+1].acc])
    fout.write(f"<script type='text/javascript'>var BOOKMARKS = new Map({bookmarks});</script>\n")


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
        return self.cnt
    
class FoutPhantom:
    def write(self, s):
        pass

if __name__ == '__main__':
    main()
    print("\n")
    main2()
