# Archivio Bolle

Ricerca di articoli su bolle/DDT scansionati (testo OCR) con indici sviluppati da me.
Linee guida del progetto: vedi `CLAUDE.md`.

## Tabella «sviluppato da me / libreria»

| Componente | Mio / libreria | Stato |
|---|---|---|
| Tokenizer | mio | fatto |
| Indice invertito (dizionario + postings, ricerca AND) | mio | fatto |
| Skip list sui postings (`PostingList`) | mio | fatto |
| Indice a trigrammi sui termini + fuzzy (Jaccard + Levenshtein, `KGramIndex`, `EditDistance`) | mio | fatto |
| Wildcard (stesso indice a k-grammi, `*` ovunque) | mio | fatto |
| Compressione dizionario/postings | mio | da fare |
| Correzione OCR | mio | da fare |
| OCR (Tesseract `ita`, rotazione via OSD; `scripts/ocr.sh`) | libreria/strumento esterno | fatto: 134 scansioni → `data/ocr/` |
| Parsing OCR → `data/corpus.tsv` + righe escluse tracciate (`ir.corpus`, regex JDK) | mio | fatto |
| fastText (solo confronto) | libreria | da fare |
