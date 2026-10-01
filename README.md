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
| Compressione: front coding sul dizionario, gap + VByte sui postings (`FrontCodedDictionary`, `VByte`, `CompressedIndex`) | mio | fatto |
| Correzione OCR mirata sui termini (scambi confusabili 0/o 1/i 5/s 6/g 8/b 7/t verso termini con df maggiore, `OcrCorrector`) | mio | fatto |
| OCR (Tesseract `ita`, rotazione via OSD; `scripts/ocr.sh`) | libreria/strumento esterno | fatto: 134 scansioni → `data/ocr/` |
| Parsing OCR → `data/corpus.tsv` + righe escluse tracciate (`ir.corpus`, regex JDK) | mio | fatto |
| Test collection (ricerca dell'articolo noto, rilevanza per codice) e metriche P/R/F1/accuratezza (`Benchmark`) | mio | fatto: `data/risultati_benchmark.txt` |
| fastText (solo confronto) | libreria | da fare |
