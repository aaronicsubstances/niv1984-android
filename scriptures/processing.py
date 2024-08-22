import xml.etree.ElementTree as ET
import os

GRID_MODE = False

def main():
    version_tag = "kjv" if "kjv" in os.getcwd() else "niv"
    global GRID_MODE
    GRID_MODE = True if version_tag == "kjv" else False
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
        if version_tag == "kjv":
            fgo = f'{f[:-4]}-kjv-niv.html'
        print(f"processing {f} into {fgo}...")
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
    chapterCounts = []
    output = {}
    dual_mode = len(version_tags) > 1
    for i in range(len(version_tags)):
        version_tag = version_tags[i]
        tree = ET.parse(fs[i])
        output_verses = []
        output_notes = []
        chapterCounts.append([])
        for chapter in tree.getroot():
            assert chapter.tag == 'chapter', chapter.tag
            cnum = int(chapter.attrib["num"])
            output_verses.append([])
            output_notes.append([])
            chapterCounts[i].append(0)
            vnum = 0
            for verse in chapter:
                assert verse.tag in ['note', 'fragment', 'verse'], verse.tag
                gridClsStart, gridClsEnd = '', ''
                if verse.tag == "verse":
                    vnum = int(verse.attrib["num"])
                    chapterCounts[i][-1] = vnum
                    if GRID_MODE and dual_mode:
                        gridClsStart = ' class="flex-item"><div class="flex-item-inner"'
                        gridClsEnd = '></div'
                    inserted_version = ''
                    if dual_mode and not GRID_MODE:
                        inserted_version = f" <b>{version_tag.upper()}</b>"
                    output_verses[-1].append(f"<div{gridClsStart}>[{vnum}{inserted_version}] {coalesce_verse(version_tag, verse, cnum, vnum)}</div{gridClsEnd}>\n")
                elif verse.tag == 'fragment':
                    if not dual_mode:
                        output_verses[-1].append(f"<div class='fragment'>[{vnum}] {coalesce_verse(version_tag, verse, cnum, vnum)}</div>\n")
                else:
                    assert verse.tag == 'note'
                    assert list(verse.attrib.keys()) == ['num'], verse.attrib.keys()
                    output_notes[-1].append(f"<div>{coalesce_verse(version_tag, verse, cnum, vnum, False, verse.attrib['num'])}</div>\n")
            assert chapterCounts[i][-1]
        output[version_tag] = {
            'v': output_verses,
            'n': output_notes
        }

    for i in range(len(version_tags)):
        version_tag = version_tags[i]
        if i == 0:
            fout.write(f"<script type='text/javascript'>var chapterCounts = {chapterCounts[0]};</script>\n")
        else:
            assert chapterCounts[0] == chapterCounts[i]
    for cnum in range(1, len(chapterCounts[0]) + 1):
        fout.write(f"<div id='chapter-{cnum}'>\n")
        fout.write(f"<h2>{'Psalm' if bnum == 19 else 'Chapter'} {cnum}</h2>\n")
        gridCls = ' class="flex-container"' if GRID_MODE and dual_mode else ''
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
        if GRID_MODE and dual_mode:
            gridClsStart = ' class="flex-item"><div class="flex-item-inner"'
            gridClsEnd = '></div'
        for j in range(len(version_tags)):
            fout.write(f"<div{gridClsStart}>\n")
            if not dual_mode or GRID_MODE:
                fout.write("<hr>\n")
            elif not j:
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
            text.append(f'<sup>[<a href="#{version_tag}-footnote-{cnum}-{e.text}" id="{version_tag}-verse-{cnum}-{e.text}">{e.text}</a>]</sup>')
            continue
        assert e.tag == 'content', e.tag
        if v.tag not in ['verse', 'wj']:
            assert e.text is not None, f"found empty content el at {cnum}:{vnum}"
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
        text.insert(0, f"<a id='{version_tag}-footnote-{cnum}-{noteref}' href='#{version_tag}-verse-{cnum}-{noteref}'>{noteref}</a>. ")
    return ''.join(text)
    
class FoutPhantom:
    def write(self, s):
        pass

if __name__ == '__main__':
    main()
    print("\n")
    main2()
