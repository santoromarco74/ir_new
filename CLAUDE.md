# Archivio Bolle — ripartenza da zero

## Premesse (vincolanti per ogni sessione)

- **Natura del progetto.** Complemento a un esame magistrale già sostenuto (Information Retrieval, laurea magistrale in Computer Engineering, Università di Pavia). Non è una tesi.
- **Valore massimo: 4 punti.** L'impegno deve essere giusto, **non eccessivo**.
- **Linguaggio:** Java (requisito del corso). Risposte e documentazione in italiano.
- **Ripartenza da zero.** La versione precedente si è sovraingegnerizzata. Si riparte da codice nuovo; il vecchio si consulta solo come riferimento. "Da zero" non significa "poco": significa che ogni componente deve avere una ragione precisa (vedi sotto), e niente strutture "per il futuro".

## Scopo

Un sistema che cerca **articoli su bolle/DDT scansionati** (testo OCR, quindi rumoroso), con **indici e algoritmi di recupero sviluppati da me**, e una valutazione sperimentale come complemento.

## Indicazioni del docente (vincolo centrale)

> «L'idea potrebbe essere interessante ma vorrei evitare che si riduca alla sola attività di benchmark delle diverse query di ricerca su uno o più indici che vengono costruiti e alimentati da una libreria di terze parti. Le chiedo quindi di dare al progetto che sta realizzando un significativo contributo in termini di strutture dati e algoritmi per l'indicizzazione e il recupero dei dati. Inoltre nel documento dovrà essere evidente cosa è stato sviluppato da lei e quanto invece viene offerto dalle librerie. Cioè il benchmark deve essere un importante complemento al suo lavoro, ma non vorrei che si limitasse solo a questo.»

Conseguenze operative:

1. **Il peso sta nelle strutture dati e negli algoritmi**, non nel benchmarking. Il benchmark è un complemento.
2. **Gli indici del sistema li costruisco io** (indice invertito, indice a k-grammi, ecc.). Le librerie di terze parti (SQLite FTS5, Lucene o altre) non fanno il lavoro centrale: al più servono come **termine di confronto**, dichiarate come tali.
3. **Nel documento finale deve essere evidente cosa è sviluppato da me e cosa viene da librerie.** Tenere aggiornata una tabella «sviluppato da me / libreria» per ogni componente, da riportare nella relazione.

## Componenti in perimetro

Sviluppati da me (strutture dati e algoritmi):

- indice invertito e ricerca a trigrammi (con fallback fuzzy)
- ricerca wildcard (indice a k-grammi sui termini)
- skip list sulle liste di postings
- compressione degli indici (dizionario, postings)
- correzione OCR mirata

Da libreria (non è un mio contributo, va dichiarato come tale nella tabella «mio / libreria»):

- fastText (n-grammi di carattere appresi): solo come modello di confronto, usato via libreria

Contorno:

- interfaccia web minimale: una barra di ricerca e le opzioni, nient'altro, sopra la stessa ricerca
- benchmark e test collection ridotta, con le metriche del corso (precisione, richiamo, F1, accuratezza; MAP solo se serve)
- relazione che documenta le strutture, con il confine «mio / libreria» ben visibile

L'elenco è aperto («…»): altre strutture si aggiungono solo se motivate.

## Generazione del corpus (da mostrare nella relazione)

Il corpus non si copia dal vecchio progetto: si genera da un **campione piccolo** (10-15 bolle, TIFF o PDF) con una pipeline riproducibile, così nella relazione si vede come nasce:

1. **OCR** — scansione → testo grezzo. Strumento esterno, **non mio**: Tesseract, lingua italiana (`ita`).
   - Download: https://github.com/tesseract-ocr/tesseract (build Windows: https://github.com/UB-Mannheim/tesseract/wiki). Già installato sulla macchina di Marco.
2. **Parsing** — testo grezzo → righe articolo (+ numero e data del documento). **Mio**: regex e logica scritte da me.
3. **Pulizia tracciata** — le righe scartate si registrano, non si cancellano in silenzio. **Mio**.
4. **Output** — `data/corpus.tsv`, versionato.

Le scansioni sono dati aziendali: **non vanno nel repo** (solo script e corpus già estratto). Il parser si scrive guardando testo OCR reale di un campione, non a memoria.

## Ancora fuori perimetro

Embedding semantici, menu di esperimenti, script di consegna, più relazioni separate, ablation oltre quelle necessarie a giustificare una scelta.

## Regole operative

1. Ogni componente deve rispondere a: *che struttura dati o algoritmo porta di mio?* Se non porta nulla, non si fa.
2. Se un componente usa una libreria, dichiararlo subito nel codice e nella tabella «mio / libreria».
3. Il benchmark non diventa mai il centro del lavoro.
4. Se non è chiaro cosa serve per la consegna, **chiedere** invece di supporre.
5. Nessuna struttura "per il futuro": solo ciò che serve ora.

## Archivio

Il materiale della versione precedente (proposta originale con gli «Esiti», README) è solo riferimento storico.
