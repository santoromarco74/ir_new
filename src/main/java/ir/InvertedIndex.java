package ir;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Indice invertito scritto da me (nessuna libreria).
 * Dizionario ordinato (TreeMap) -> lista di postings (docId crescenti, senza duplicati).
 * I docId sono assegnati in ordine di inserimento, quindi le liste restano ordinate per costruzione.
 */
public class InvertedIndex {
    private final Map<String, List<Integer>> postings = new TreeMap<>();
    private final List<String> docs = new ArrayList<>();

    /** Aggiunge un documento (una riga articolo) e restituisce il suo docId. */
    public int add(String text) {
        int id = docs.size();
        docs.add(text);
        for (String t : Tokenizer.tokenize(text)) {
            List<Integer> p = postings.computeIfAbsent(t, k -> new ArrayList<>());
            if (p.isEmpty() || p.get(p.size() - 1) != id) p.add(id);
        }
        return id;
    }

    public String doc(int id) { return docs.get(id); }

    public int size() { return docs.size(); }

    public List<Integer> postings(String term) {
        return postings.getOrDefault(term, List.of());
    }

    /** AND di tutti i termini della query: intersezione a merge di liste ordinate. */
    public List<Integer> searchAnd(String query) {
        List<String> terms = Tokenizer.tokenize(query);
        if (terms.isEmpty()) return List.of();
        List<Integer> result = postings(terms.get(0));
        for (int i = 1; i < terms.size() && !result.isEmpty(); i++) {
            result = intersect(result, postings(terms.get(i)));
        }
        return result;
    }

    static List<Integer> intersect(List<Integer> a, List<Integer> b) {
        List<Integer> out = new ArrayList<>();
        int i = 0, j = 0;
        while (i < a.size() && j < b.size()) {
            int x = a.get(i), y = b.get(j);
            if (x == y) { out.add(x); i++; j++; }
            else if (x < y) i++;
            else j++;
        }
        return out;
    }
}
