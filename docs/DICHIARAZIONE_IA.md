# Dichiarazione sull'utilizzo di strumenti di IA

Da allegare al progetto (Linee Guida IA Università di Pavia, Delibera CdA n. 153/2026). Completare i campi tra parentesi quadre.

---

**Dichiarazione sull'utilizzo di strumenti di Intelligenza Artificiale**

Nella stesura del presente progetto («Archivio Bolle», complemento all'esame di Information Retrieval) sono stati utilizzati i seguenti strumenti di Intelligenza Artificiale, come richiesto dalle Linee Guida per l'utilizzo dell'Intelligenza Artificiale dell'Università di Pavia (Delibera del Consiglio di Amministrazione n. 153/2026 del 22/05/2026):

- **Strumento utilizzato**: Claude (Anthropic), usato tramite Claude Code, assistente di programmazione, in sessioni di lavoro su ambiente cloud. Versione del modello: [da indicare, se richiesta].

- **Perimetro di applicazione**:
  - il codice Java del sistema (parser del testo OCR, indice invertito, skip list, indice a trigrammi con ricerca wildcard e fuzzy, compressione, persistenza dell'indice, correzione OCR, benchmark, interfaccia web) e i relativi test automatici; gli script di supporto (OCR, grafici) e la configurazione di Maven;
  - la bozza della relazione (testo, tabelle, formule ed esempi ricavati dall'esecuzione del codice) e della documentazione (README, istruzioni di riproduzione);
  - l'esecuzione dei programmi di misura (benchmark e valutazioni) e la correzione degli errori emersi.

  Obiettivi e vincoli del progetto sono descritti nel file `CLAUDE.md`, fornito dall'autore all'assistente. Le scansioni delle bolle e il testo OCR da esse ricavato sono stati elaborati nell'ambiente di lavoro dell'assistente.

- **Modalità di impiego**: generazione di codice su indicazioni dell'autore, poi eseguito e verificato con test automatici (anch'essi scritti con l'assistente); debugging; stesura e revisione della bozza della relazione e della documentazione su richiesta dell'autore; analisi dei risultati sperimentali.

- **Altri strumenti software**: Tesseract (OCR) e fastText sono componenti usati dal sistema e descritti al §2 della relazione; non sono strumenti usati per redigere il lavoro.

<!-- DA VERIFICARE PRIMA DI CONSEGNARE: la frase seguente va tenuta solo se è vera. Vedi le azioni consigliate nella risposta di Claude Code. -->
Si dichiara che tutti i contenuti generati con il supporto dell'IA sono stati criticamente verificati e rielaborati personalmente, e che l'autore si assume la piena responsabilità della correttezza e dell'originalità del lavoro presentato.

[Nome e cognome]
[Data]
