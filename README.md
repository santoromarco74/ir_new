# Archivio Bolle

Ricerca di articoli su bolle/DDT scansionati (testo OCR) con indici sviluppati da me.
Linee guida del progetto: vedi `CLAUDE.md`.

## Tabella «sviluppato da me / libreria»

| Componente | Mio / libreria | Stato |
|---|---|---|
| Tokenizer | mio | fatto |
| Indice invertito (dizionario + postings, ricerca AND) | mio | fatto |
| Skip list sui postings | mio | da fare |
| Ricerca a trigrammi + fuzzy | mio | da fare |
| Wildcard (indice a k-grammi) | mio | da fare |
| Compressione dizionario/postings | mio | da fare |
| Correzione OCR | mio | da fare |
| OCR (Tesseract `ita`, rotazione via OSD; `scripts/ocr.sh`) | libreria/strumento esterno | fatto: 134 scansioni → `data/ocr/` |
| Parsing OCR → `data/corpus.tsv` + righe escluse tracciate (`ir.corpus`, regex JDK) | mio | fatto |
| fastText (solo confronto) | libreria | da fare |
