// Text extraction from an EPUB file. Not a reader: Quire never renders a book — the
// reader's own app does that (QUI-002 is deferred). This opens the file once, at import,
// pulls the prose out, and is done with it.
//
// jsoup is ~450 KB against the 450 MB footprint (CLAUDE.md §6). An EPUB's chapters are
// XHTML and hand-rolled tag stripping fails on entities, nested inline markup and CDATA —
// the places a book quietly loses its text.
dependencies {
    api(project(":core:model"))
    implementation("org.jsoup:jsoup:1.21.1")
}
