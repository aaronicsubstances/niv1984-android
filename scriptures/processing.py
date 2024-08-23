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
            else:
                related_f = os.path.join("..", "kjv1769-xml", f)
            work_on_xml(bnum, version_tags, [f, related_f], fout)
            fout.write("""</div>
    <script src="../js/jquery-3.7.1.min.js" type="text/javascript"></script>
    <script src="../js/base.js" type="text/javascript"></script>
</body>
</html>""")

def work_on_xml(bnum, version_tags, fs, fout):
    checkpoints = []
    output = {}
    dual_mode = len(version_tags) > 1
    for i in range(len(version_tags)):
        version_tag = version_tags[i]
        tree = ET.parse(fs[i])
        output_verses = []
        output_notes = []
        vcnt = 0
        for chapter in tree.getroot():
            assert chapter.tag == 'chapter', chapter.tag
            cnum = int(chapter.attrib["num"])
            if i == 0:
                checkpoints.append(-cnum)
            output_verses.append([])
            output_notes.append([])
            vnum = 0
            for verse in chapter:
                assert verse.tag in ['note', 'fragment', 'verse'], verse.tag
                gridClsStart, gridClsEnd = '', ''
                if verse.tag == "verse":
                    vnum = int(verse.attrib["num"])
                    if dual_mode:
                        gridClsStart = ' class="flex-item"><div class="flex-item-inner"'
                        gridClsEnd = '></div'
                    verse_id = ''
                    if i == 0:
                        vcnt += 1
                        verse_id = f" id='verse-{vcnt}'"
                    output_verses[-1].append(f"<div{verse_id}{gridClsStart}>[{vnum}] {coalesce_verse(version_tag, verse, cnum, vnum)}</div{gridClsEnd}>\n")
                elif verse.tag == 'fragment':
                    if not dual_mode:
                        output_verses[-1].append(f"<div class='fragment'>[{vnum}] {coalesce_verse(version_tag, verse, cnum, vnum)}</div>\n")
                else:
                    assert verse.tag == 'note'
                    assert list(verse.attrib.keys()) == ['num'], verse.attrib.keys()
                    output_notes[-1].append(f"<div>{coalesce_verse(version_tag, verse, cnum, vnum, False, verse.attrib['num'])}</div>\n")
    
            if i == 0:
                checkpoints.append(vcnt)
        output[version_tag] = {
            'v': output_verses,
            'n': output_notes
        }

    fout.write(f"<script type='text/javascript'>var checkpoints = {checkpoints};</script>\n")
    for cnum in range(1, CHAPTER_COUNTS[bnum-1] + 1):
        fout.write(f"<div id='chapter-{cnum}'>\n")
        fout.write(f"<h2>{'Psalm' if bnum == 19 else 'Chapter'} {cnum}</h2>\n")
        gridCls = ' class="flex-container"' if dual_mode else ''
        fout.write(f"<div{gridCls}>\n")
        if dual_mode:
            common_count = len(output[version_tags[0]]['v'][cnum-1])
            assert len(output[version_tags[1]]['v'][cnum-1]) == common_count
            for j in range(common_count):
                fout.write(f"{output[version_tags[0]]['v'][cnum-1][j]}\n")
                fout.write(f"{output[version_tags[1]]['v'][cnum-1][j]}\n")
        else:
            for item in output[version_tags[0]]['v'][cnum-1]:
                fout.write(f"{item}\n")
        gridClsStart, gridClsEnd = '', ''
        if dual_mode:
            gridClsStart = ' class="flex-item"><div class="flex-item-inner"'
            gridClsEnd = '></div'
        for j in range(len(version_tags)):
            fout.write(f"<div{gridClsStart}>\n")
            fout.write("<hr>\n")
            for item in output[version_tags[j]]['n'][cnum-1]:
                fout.write(f"{item}\n")
            fout.write("<hr>\n")
            fout.write(f"</div{gridClsEnd}>\n")
        fout.write("</div>\n")
        fout.write("</div>\n")

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
    
class FoutPhantom:
    def write(self, s):
        pass

if __name__ == '__main__':
    main()
    print("\n")
    main2()
