package ir;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Ricerca sopra InvertedIndex + KGramIndex (tutto mio). Query = parole separate da spazio, in AND.
 * Ogni parola: con '*' -> wildcard (OR dei termini espansi); presente nel dizionario -> esatta;
 * assente -> fallback fuzzy (OR dei termini simili).
 */
public final class Searcher {
    private final InvertedIndex index;
    private final KGramIndex kgrams;
    private final boolean fuzzy;

    public Searcher(InvertedIndex index, boolean fuzzy) {
        this.index = index;
        this.kgrams = new KGramIndex(index.terms(), 3);
        this.fuzzy = fuzzy;
    }

    public List<Integer> search(String query) {
        PostingList result = null;
        for (String parola : query.toLowerCase(Locale.ITALIAN).trim().split("\\s+")) {
            if (parola.isEmpty()) continue;
            PostingList p = postingsParola(parola);
            result = result == null ? p : PostingList.intersect(result, p, null);
        }
        List<Integer> out = new ArrayList<>();
        if (result != null) for (int i = 0; i < result.size(); i++) out.add(result.get(i));
        return out;
    }

    /** Termini del dizionario a cui la parola viene espansa. */
    public List<String> espandi(String parola) {
        if (parola.contains("*")) return kgrams.wildcard(parola);
        if (index.contains(parola)) return List.of(parola);
        return fuzzy ? kgrams.fuzzy(parola, 0.2, parola.length() <= 4 ? 1 : 2) : List.of();
    }

    private PostingList postingsParola(String parola) {
        PostingList u = PostingList.VUOTA;
        for (String t : espandi(parola)) u = PostingList.union(u, index.postings(t));
        return u;
    }
}
