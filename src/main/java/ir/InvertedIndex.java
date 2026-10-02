package ir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Indice invertito (nessuna libreria).
 * Dizionario ordinato (TreeMap) -> postings (docId crescenti, senza duplicati, con skip pointers).
 * I docId sono assegnati in ordine di inserimento, quindi le liste restano ordinate per costruzione.
 */
public class InvertedIndex implements Indice {
    private final Map<String, List<Integer>> costruzione = new TreeMap<>();
    private final Map<String, List<Integer>> frequenze = new HashMap<>(); // parallela a costruzione
    private final Map<String, PostingList> congelate = new HashMap<>();
    private final List<String> docs = new ArrayList<>();
    private final List<Integer> lunghezze = new ArrayList<>();
    private long sommaLunghezze = 0;

    /** Aggiunge un documento (una riga articolo) e restituisce il suo docId. */
    public int add(String text) {
        return add(text, Map.of());
    }

    /** Come {@link #add(String)}, ma ogni termine passa prima per la mappa di correzione (es. OcrCorrector). */
    public int add(String text, Map<String, String> correzioni) {
        int id = docs.size();
        docs.add(text);
        congelate.clear();
        Map<String, Integer> conteggio = new java.util.LinkedHashMap<>();
        int n = 0;
        for (String t0 : Tokenizer.tokenize(text)) {
            conteggio.merge(correzioni.getOrDefault(t0, t0), 1, Integer::sum);
            n++;
        }
        for (Map.Entry<String, Integer> e : conteggio.entrySet()) {
            costruzione.computeIfAbsent(e.getKey(), k -> new ArrayList<>()).add(id);
            frequenze.computeIfAbsent(e.getKey(), k -> new ArrayList<>()).add(e.getValue());
        }
        lunghezze.add(n);
        sommaLunghezze += n;
        return id;
    }

    @Override
    public String doc(int id) { return docs.get(id); }

    @Override
    public int size() { return docs.size(); }

    /** Termini del dizionario in ordine alfabetico. */
    @Override
    public List<String> terms() { return new ArrayList<>(costruzione.keySet()); }

    @Override
    public boolean contains(String term) { return costruzione.containsKey(term); }

    @Override
    public PostingList postings(String term) {
        List<Integer> l = costruzione.get(term);
        if (l == null) return PostingList.VUOTA;
        return congelate.computeIfAbsent(term, k -> new PostingList(l.stream().mapToInt(Integer::intValue).toArray()));
    }

    @Override
    public int[] tf(String term) {
        List<Integer> l = frequenze.get(term);
        return l == null ? new int[0] : l.stream().mapToInt(Integer::intValue).toArray();
    }

    @Override
    public int lunghezza(int docId) { return lunghezze.get(docId); }

    @Override
    public double lunghezzaMedia() { return docs.isEmpty() ? 0 : (double) sommaLunghezze / docs.size(); }

    /** AND di tutti i termini della query, con skip pointers. */
    public List<Integer> searchAnd(String query) {
        List<String> terms = Tokenizer.tokenize(query);
        if (terms.isEmpty()) return List.of();
        PostingList result = postings(terms.get(0));
        for (int i = 1; i < terms.size() && result.size() > 0; i++) {
            result = PostingList.intersect(result, postings(terms.get(i)), null);
        }
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < result.size(); i++) out.add(result.get(i));
        return out;
    }
}
