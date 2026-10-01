package ir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Indice invertito scritto da me (nessuna libreria).
 * Dizionario ordinato (TreeMap) -> postings (docId crescenti, senza duplicati, con skip pointers).
 * I docId sono assegnati in ordine di inserimento, quindi le liste restano ordinate per costruzione.
 */
public class InvertedIndex {
    private final Map<String, List<Integer>> costruzione = new TreeMap<>();
    private final Map<String, PostingList> congelate = new HashMap<>();
    private final List<String> docs = new ArrayList<>();

    /** Aggiunge un documento (una riga articolo) e restituisce il suo docId. */
    public int add(String text) {
        return add(text, Map.of());
    }

    /** Come {@link #add(String)}, ma ogni termine passa prima per la mappa di correzione (es. OcrCorrector). */
    public int add(String text, Map<String, String> correzioni) {
        int id = docs.size();
        docs.add(text);
        congelate.clear();
        for (String t0 : Tokenizer.tokenize(text)) {
            String t = correzioni.getOrDefault(t0, t0);
            List<Integer> p = costruzione.computeIfAbsent(t, k -> new ArrayList<>());
            if (p.isEmpty() || p.get(p.size() - 1) != id) p.add(id);
        }
        return id;
    }

    public String doc(int id) { return docs.get(id); }

    public int size() { return docs.size(); }

    /** Termini del dizionario in ordine alfabetico. */
    public List<String> terms() { return new ArrayList<>(costruzione.keySet()); }

    public boolean contains(String term) { return costruzione.containsKey(term); }

    public PostingList postings(String term) {
        List<Integer> l = costruzione.get(term);
        if (l == null) return PostingList.VUOTA;
        return congelate.computeIfAbsent(term, k -> new PostingList(l.stream().mapToInt(Integer::intValue).toArray()));
    }

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
