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
| OCR (Tesseract, `ita`) | libreria/strumento esterno | da fare |
| Parsing OCR → `data/corpus.tsv` | mio | da fare |
| fastText (solo confronto) | libreria | da fare |
