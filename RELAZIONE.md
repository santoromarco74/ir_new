# Archivio Bolle — relazione

Complemento all'esame di Information Retrieval (laurea magistrale in Computer Engineering, Università di Pavia).

## 1. Obiettivo

Un sistema che cerca **articoli su bolle di trasporto (DDT) scansionate**. Il testo arriva da un OCR e quindi è rumoroso: codici e modelli letti male, caratteri scambiati (`0/O`, `5/S`, `6/G`…), righe capovolte. Il lavoro è concentrato sulle **strutture dati e sugli algoritmi di indicizzazione e recupero**, scritti da me. Il benchmark è solo un complemento che verifica se le scelte servono.

## 2. Cosa è mio e cosa viene da librerie

| Componente | File | Mio / libreria |
|---|---|---|
| OCR della scansione | `scripts/ocr.sh` | **Libreria/strumento esterno**: Tesseract (lingua `ita`, rilevamento orientamento OSD), ImageMagick (`convert`), poppler (`pdftoppm`). Lo script che li lancia è mio, i programmi no |
| Parsing OCR → righe articolo, numero e data | `ir.corpus.OcrParser` | **Mio** (solo `java.util.regex` della JDK) |
| Pulizia tracciata, `corpus.tsv`, `righe_escluse.tsv` | `ir.corpus.CorpusBuilder` | **Mio** |
| Tokenizzazione | `Tokenizer` | **Mio** |
| Indice invertito (dizionario ordinato + postings) | `InvertedIndex` | **Mio** (usa `TreeMap`/`ArrayList` della JDK come contenitori) |
| Liste di postings con skip pointers, intersezione, unione | `PostingList` | **Mio** |
| Indice a trigrammi sui termini | `KGramIndex` | **Mio** |
| Ricerca wildcard | `KGramIndex.wildcard` | **Mio** |
| Ricerca fuzzy (Jaccard sui trigrammi + distanza di edit) | `KGramIndex.fuzzy`, `EditDistance` | **Mio** |
| Compressione: gap + variable byte, front coding | `VByte`, `FrontCodedDictionary`, `CompressedIndex` | **Mio** |
| Correzione OCR mirata | `OcrCorrector` | **Mio** |
| Motore di ricerca che compone le parti | `Searcher` | **Mio** |
| Interfaccia web minimale | `WebServer` | **Mio**, sopra il server HTTP della JDK (`com.sun.net.httpserver`); nessun framework |
| Test collection e metriche | `Benchmark` | **Mio** |
| Test automatici | `src/test` | JUnit 5 (libreria, solo per i test) |
| fastText (modello di confronto) | `FastTextConfronto` | **LIBRERIA di terzi**: fastText (Facebook) tramite il wrapper Java JFastText (`com.github.vinhkhuc:jfasttext` 0.5, JNI con libreria nativa inclusa, dipendenza `org.bytedeco:javacpp`). Addestramento e vettori sono della libreria; mio è solo l'uso come espansione dei termini (§8) |

Nessuna libreria di indicizzazione (Lucene, SQLite FTS5 o simili) è usata nel sistema: tutti gli indici sono strutture in memoria scritte da me. L'unica libreria di terzi che entra nel codice di ricerca è fastText, e solo come termine di confronto nel benchmark: non è usata da `Searcher`, dalla riga di comando né dall'interfaccia web.

## 3. Corpus

Il corpus nasce da una pipeline riproducibile a quattro passi, a partire da 134 scansioni di un solo fornitore (132 TIFF e 2 PDF, 1-7 pagine ciascuna).

1. **OCR** (`scripts/ocr.sh`, strumento esterno). Una parte delle pagine è capovolta di 180° e produce testo illeggibile. Prima dell'OCR ogni pagina passa per il rilevamento di orientamento di Tesseract (OSD) e viene ruotata. Il testo grezzo è in `data/ocr/`, un file per scansione, con `\f` come separatore di pagina.
2. **Parsing** (`OcrParser`). Una riga è un articolo se ha la forma `codice MARCA descrizione… quantità`: codice di 5-7 cifre, marca di 3-4 lettere maiuscole, tolleranza al rumore OCR davanti alla riga. Numero e data del documento vengono dall'intestazione (`006220 Pg 1/1 03/02/2026`). I token di solo rumore (punteggiatura, `i`/`l` isolate) sono rimossi dalla descrizione.
3. **Pulizia tracciata** (`CorpusBuilder`). Ogni riga non articolo è scritta in `data/righe_escluse.tsv` con un motivo (`indirizzo`, `intestazione_documento`, `riferimento_fattura_ordine`, `luogo_destinazione`, `altro`). I documenti senza nessuna riga articolo, cioè con un altro layout di fornitore, sono scartati interi e registrati.
4. **Output**: `data/corpus.tsv`.

Risultato: 115 documenti con articoli, **1067 righe articolo**, 6470 righe escluse e registrate, 19 documenti scartati perché di layout diverso (spedizioni GLS, COOP ITALIA, bolle con righe del tipo `sku descrizione CL n PZ`). Il dizionario ha 3046 termini. In 5 documenti numero e data non sono leggibili.

## 4. Indice invertito e skip list

`InvertedIndex` associa a ogni termine una lista di docId crescenti, uno per riga articolo. Il dizionario è un `TreeMap`, quindi ordinato.

`PostingList` è un `int[]` con skip pointers. Il passo è la radice quadrata della lunghezza: dalle posizioni multiple del passo si può saltare di un passo quando il docId di arrivo è ancora minore o uguale a quello cercato. L'intersezione con skip è verificata contro un merge lineare di riferimento su coppie casuali (stessi risultati, meno confronti).

Sulle 214 query del benchmark, i confronti fra docId nell'intersezione di due termini sono:

| Intersezione | Merge lineare | Con skip |
|---|---|---|
| modello AND parola | 8718 | 2343 |
| modello AND `2` (df 312) | 45824 | 6552 |

Il secondo caso è favorevole agli skip di proposito: il termine `2` è molto frequente, ma è una quantità e non una parola. Non ho misurato i tempi di esecuzione, solo i confronti.

## 5. Trigrammi, wildcard e fuzzy

`KGramIndex` indicizza i **termini del dizionario** (non i documenti) per trigrammi, con `$` ai bordi: `vite` → `$vi`, `vit`, `ite`, `te$`. Ogni trigramma punta alla lista ordinata dei termini che lo contengono.

- **Wildcard** (`lava*`, `*frigo*`, `l*glie`). Si prendono i trigrammi dei pezzi fissi del pattern, si intersecano le liste, poi si filtra con un confronto diretto sul pattern, senza regex. Se nessun pezzo ha almeno 3 caratteri si scandisce il dizionario.
- **Fuzzy**. Si contano i trigrammi condivisi col termine della query e si tengono i candidati con Jaccard ≥ 0,2. Il filtro è volutamente largo perché la verifica vera è la distanza di Levenshtein (`EditDistance`), con massimo 1 errore per parole di al più 4 lettere e 2 per le altre.

`Searcher` compone il tutto. Una query è una sequenza di parole in AND. Per ogni parola: con `*` è wildcard; altrimenti dipende dalla modalità fuzzy: *no*, *solo se la parola non è nel dizionario*, *sempre*. Ogni parola si espande in un insieme di termini, di cui si fa l'unione dei postings.

## 6. Compressione

- **Postings**: gap fra docId consecutivi, poi codifica a lunghezza variabile (7 bit per byte, bit alto sull'ultimo byte).
- **Dizionario**: front coding a blocchi di 8 termini. Il primo è scritto intero, gli altri come prefisso condiviso più suffisso. La ricerca è binaria sulle teste dei blocchi, poi lineare nel blocco.

`CompressedIndex` è la versione compressa, a sola lettura, dell'indice. È verificata per equivalenza (stessi termini, stessi postings) ma non è collegata al `Searcher`.

| Struttura | Non compressa | Compressa | Rapporto |
|---|---|---|---|
| Postings (4 byte per docId) | 50588 byte | 16876 byte | 33% |
| Dizionario (UTF-8 + 1 byte di lunghezza per termine) | 22947 byte | 19400 byte | 85% |

I postings guadagnano molto. Il dizionario poco, perché i termini sono in gran parte codici e modelli che condividono pochi prefissi. Le scelte di baseline e di dimensione del blocco sono arbitrarie; non ho misurato il costo in tempo della decodifica.

## 7. Correzione OCR mirata

`OcrCorrector` lavora sui termini del dizionario. Corregge **solo** scambi fra caratteri che l'OCR confonde (`0/o`, `1/i`, `5/s`, `6/g`, `8/b`, `7/t`, al massimo due scambi per termine) e **solo verso un termine già presente nel dizionario con più documenti**. Non fa correzioni a distanza di edit generica: i numeri che differiscono davvero (`4GB`/`8GB`) non vengono toccati. Sono esclusi i termini con meno di 4 caratteri e quelli fatti solo di cifre, che sono troppo ambigui: una prima versione senza queste regole produceva correzioni sbagliate come `17 → it` e `46 → 4g`.

La tabella dei caratteri confondibili è stata ricavata dai dati: righe con lo stesso codice articolo, lette in modo diverso in bolle diverse.

**Valutazione.** Come verità di riferimento uso lo stesso corpus: per le righe con lo stesso codice e descrizione di pari lunghezza in token, la lettura più frequente è considerata corretta. È una stima, non un'annotazione manuale. Su 3046 termini:

- 22 correzioni proposte: **11 confermate**, 1 contraddetta (`gliv → 6liv`, dove con ogni probabilità sbaglia il riferimento, perché `6LIV` sta per «6 livelli», ma non posso dimostrarlo), 10 non verificabili;
- 50 errori noti, **11 recuperati**. Gli altri sono di altro tipo (cifre diverse, lettere cadute) e il correttore non li tocca di proposito.

## 8. Valutazione sperimentale

**Test collection.** È una ricerca dell'articolo noto. Per ogni codice articolo presente in almeno due righe (214 codici) la query è «modello + prima parola descrittiva» oppure «prefisso del modello + `*`», presa dalla lettura più frequente. I documenti rilevanti sono **tutte le righe con quel codice**, comprese quelle in cui l'OCR ha letto male modello o parola. Il giudizio di rilevanza è quindi automatico, non manuale. In 65 query almeno una riga rilevante contiene modello o parola letti diversamente: è il sottoinsieme in cui si vede l'effetto dell'OCR. Le metriche sono precisione, richiamo, F1 e accuratezza, mediate sulle query.

| Sistema | Set | P | R | F1 |
|---|---|---|---|---|
| esatta | tutte (214) | 0,995 | 0,847 | 0,892 |
| esatta | con righe lette diversamente (65) | 1,000 | 0,496 | 0,655 |
| fuzzy solo se assente | tutte / con righe lette diversamente | come esatta | come esatta | come esatta |
| fuzzy sempre | tutte | 0,847 | 0,981 | 0,884 |
| fuzzy sempre | con righe lette diversamente | 0,865 | 0,937 | 0,873 |
| correzione OCR | tutte | 0,985 | 0,853 | 0,893 |
| correzione OCR | con righe lette diversamente | 0,965 | 0,516 | 0,657 |
| correzione OCR + fuzzy sempre | tutte | 0,846 | 0,981 | 0,883 |
| fastText (**libreria**, confronto) | tutte | 0,444 | 0,992 | 0,577 |
| fastText (**libreria**, confronto) | con righe lette diversamente | 0,513 | 0,973 | 0,634 |
| wildcard (prefisso del modello) | tutte | 0,860 | 0,890 | 0,831 |
| wildcard (prefisso del modello) | con righe lette diversamente | 0,897 | 0,638 | 0,695 |

(Tabella completa, con l'accuratezza e l'indice corretto, in `data/risultati_benchmark.txt`.)

**Lettura dei risultati.**

- Il fuzzy «solo se assente» non cambia nulla: i termini della query esistono già nel dizionario e il fuzzy non scatta.
- Il fuzzy «sempre» porta il richiamo da 0,847 a 0,981 ma abbassa la precisione a 0,847: fonde prodotti diversi con codici vicini. È un compromesso, non un miglioramento netto.
- La correzione OCR aumenta di poco il richiamo (da 0,496 a 0,516 sulle query con righe lette diversamente) e abbassa un po' la precisione. Quattro delle 22 correzioni non hanno nessuna riga con lo stesso codice a supporto (`1286b → 128gb`, `1p64 → ip64`, `arfoelo5 → arfoel05`, `spale → 5pale`); in due di queste anche il codice è stato letto male, quindi una correzione plausibile può essere contata come errore dal giudizio automatico.
- Gli skip riducono i confronti, come in §4.
- **fastText (libreria, confronto).** `FastTextConfronto` addestra un modello skipgram con n-grammi di carattere (3-6) sul solo testo del corpus; ogni termine del dizionario ha un vettore (anche quelli mai visti ne ricevono uno dai loro n-grammi) e una parola della query si espande nei termini a coseno più alto (al massimo 5 vicini con coseno ≥ 0,9), poi si usano gli stessi postings e la stessa intersezione del resto del sistema. Ha il richiamo più alto (0,992) ma la precisione più bassa (0,444): su un corpus di 1067 righe i vettori sono poco discriminanti e molti termini vicini per n-grammi non sono lo stesso prodotto (per esempio `8gb`/`6gb`). I parametri sono stati fissati a priori e **non ottimizzati sul benchmark**: una soglia di coseno più severa sposterebbe il compromesso verso la precisione, ma non l'ho provata per non tarare il confronto sul test. Con un corpus più grande il risultato potrebbe cambiare. Non supporta i caratteri jolly, quindi non compare nella riga wildcard. L'addestramento usa un solo thread per ridurre la variabilità, ma non ho verificato che i risultati siano identici tra esecuzioni.

## 9. Limiti

- Un solo fornitore e un corpus piccolo (1067 righe): i numeri mostrano un comportamento, non una prestazione generale.
- Il giudizio di rilevanza è automatico (stesso codice) e il codice stesso può essere letto male.
- Gli errori OCR di tipo diverso dagli scambi confondibili non sono corretti.
- Il fuzzy parte solo se la parola non è nel dizionario (modalità «solo se assente»): un errore OCR che per caso è un'altra parola valida non viene recuperato.
- La quantità resta in fondo alla descrizione e non è un campo separato; con righe senza quantità non si distingue da un numero della descrizione.
- Una pagina (`20260703100204858`) resta capovolta anche dopo il rilevamento di orientamento.
- Il `CompressedIndex` non è usato dal `Searcher`. Non ho misurato tempi di esecuzione né il costo della decodifica.
- fastText è usato solo come confronto e con parametri non ottimizzati (§8). Il wrapper `jfasttext` è un pacchetto di terzi con libreria nativa inclusa (qui usata su Linux x86-64): su altre piattaforme il test corrispondente viene saltato e il benchmark non gira senza di essa.

## 10. Riproduzione

```
scripts/ocr.sh                                          # scansioni/ -> data/ocr/  (richiede tesseract-ocr-ita, imagemagick, poppler-utils)
mvn -q compile
CP="target/classes:$(mvn -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout)"   # classpath con le dipendenze (serve per Benchmark)
java -cp target/classes ir.corpus.CorpusBuilder         # data/ocr -> data/corpus.tsv, data/righe_escluse.tsv
java -cp target/classes ir.Cli "lava*" --ocr            # prova da riga di comando
java -cp target/classes ir.WebServer 8080               # interfaccia web su http://localhost:8080
java -cp target/classes ir.Compressione                 # spazio prima/dopo la compressione
java -cp target/classes ir.ValutaCorrezione             # valutazione della correzione OCR
java -cp "$CP" ir.Benchmark                           # data/risultati_benchmark.txt
mvn -q test                                             # test automatici
```

Le scansioni sono dati aziendali: la pipeline le legge da `scansioni/`; il lavoro prodotto (script, testo OCR, corpus estratto) sta in `scripts/` e `data/`.
