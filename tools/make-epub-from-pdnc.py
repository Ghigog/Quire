#!/usr/bin/env python3
"""Turn a PDNC novel's plain text into an EPUB the desktop app can actually read.

QUI-046's companion. `tools/fetch-pdnc.sh` gives 28 real, out-of-copyright novels as
`novel_text.txt`; `quire read` wants an EPUB. This is the bridge, so the desktop app can be
pointed at a real book — a large cast, untagged dialogue, century-old prose — instead of at
the 26-line fixture.

Nothing here is committed as a book (CLAUDE.md §8): PDNC carries no licence, and the point
of the script is that the text is fetched, not stored.

Usage:
    tools/fetch-pdnc.sh
    tools/make-epub-from-pdnc.py Emma --out ~/Desktop/Quire-books
    tools/make-epub-from-pdnc.py --list
"""
import argparse
import html
import os
import re
import sys
import zipfile

PDNC = os.environ.get("PDNC_HOME", os.path.expanduser("~/.cache/quire/pdnc"))
CHAPTER = re.compile(r"^(CHAPTER|LETTER)\s+[\dIVXLC]+\b", re.I)
FLUFF = re.compile(r"^(VOLUME|PART)\b", re.I)


def paragraphs(text):
    return [p.strip() for p in re.split(r"\n\s*\n", text) if p.strip()]


def split_chapters(paras):
    """Chapters from the first CHAPTER heading on, so chapter 0 is real content.

    Front matter (title, author, volume) is dropped rather than voiced: a listener wants the
    book, and `quire read --list` should name chapters a reader would recognise.
    """
    starts = [i for i, p in enumerate(paras) if CHAPTER.match(p)]
    if not starts:
        return [("Chapter 1", paras)]
    chapters = []
    for n, start in enumerate(starts):
        end = starts[n + 1] if n + 1 < len(starts) else len(paras)
        body = [p for p in paras[start + 1:end] if not FLUFF.match(p)]
        if body:
            chapters.append((paras[start], body))
    return chapters


def write_epub(book_id, title, chapters, out_path):
    xhtml = (
        '<?xml version="1.0" encoding="utf-8"?>'
        '<html xmlns="http://www.w3.org/1999/xhtml"><head><title>{t}</title></head>'
        "<body>{body}</body></html>"
    )
    items, spine = [], []
    with zipfile.ZipFile(out_path, "w") as z:
        # The mimetype entry must come first and must be stored, not deflated.
        z.writestr(zipfile.ZipInfo("mimetype"), "application/epub+zip", zipfile.ZIP_STORED)
        z.writestr(
            "META-INF/container.xml",
            '<?xml version="1.0"?><container version="1.0" '
            'xmlns="urn:oasis:names:tc:opendocument:xmlns:container"><rootfiles>'
            '<rootfile full-path="OEBPS/content.opf" '
            'media-type="application/oebps-package+xml"/></rootfiles></container>',
        )
        for i, (heading, body) in enumerate(chapters):
            parts, seen = [], set()
            for para in [heading] + body:
                if para in seen:
                    continue
                seen.add(para)
                parts.append("<p>%s</p>" % html.escape(re.sub(r"\s+", " ", para)))
            z.writestr("OEBPS/ch%d.xhtml" % i, xhtml.format(t="Chapter %d" % (i + 1), body="\n".join(parts)))
            items.append('<item id="c%d" href="ch%d.xhtml" media-type="application/xhtml+xml"/>' % (i, i))
            spine.append('<itemref idref="c%d"/>' % i)
        z.writestr(
            "OEBPS/content.opf",
            '<?xml version="1.0" encoding="utf-8"?><package xmlns="http://www.idpf.org/2007/opf" '
            'version="3.0" unique-identifier="id"><metadata '
            'xmlns:dc="http://purl.org/dc/elements/1.1/">'
            '<dc:identifier id="id">%s</dc:identifier><dc:title>%s</dc:title>'
            "<dc:language>en</dc:language></metadata><manifest>%s</manifest>"
            "<spine>%s</spine></package>" % (book_id, html.escape(title), "".join(items), "".join(spine)),
        )


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("novel", nargs="?", help="a directory name under PDNC's data/")
    parser.add_argument("--out", default="~/Desktop/Quire-books", help="where to write the .epub")
    parser.add_argument("--max-chapters", type=int, default=0, help="0 for all of them")
    parser.add_argument("--list", action="store_true", help="show the novels available and exit")
    args = parser.parse_args()

    if args.list or not args.novel:
        available = sorted(os.listdir(os.path.join(PDNC, "data"))) if os.path.isdir(os.path.join(PDNC, "data")) else []
        if not available:
            sys.exit("no PDNC novels found — run tools/fetch-pdnc.sh first")
        print("\n".join(available))
        return

    source = os.path.join(PDNC, "data", args.novel, "novel_text.txt")
    if not os.path.isfile(source):
        sys.exit("no %s — is %r one of the names from --list?" % (source, args.novel))

    chapters = split_chapters(paragraphs(open(source, encoding="utf-8").read()))
    if args.max_chapters:
        chapters = chapters[: args.max_chapters]

    out_dir = os.path.expanduser(args.out)
    os.makedirs(out_dir, exist_ok=True)
    out = os.path.join(out_dir, "%s.epub" % args.novel)
    write_epub(args.novel, args.novel, chapters, out)

    words = sum(len(p.split()) for _, body in chapters for p in body)
    print("wrote %s — %d chapters, %d paragraphs, %d words" % (out, len(chapters), sum(len(b) for _, b in chapters), words))


if __name__ == "__main__":
    main()
