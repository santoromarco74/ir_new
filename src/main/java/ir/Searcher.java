package ir;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Ricerca sopra InvertedIndex + KGramIndex (tutto mio). Query = parole separate da spazio, in AND.
 * Ogni parola: con '*' -> wildcard (OR dei termini espansi); altrimenti dipende dal {@link ModoFuzzy}:
 * NO = solo esatta; FALLBACK = fuzzy solo se la parola non e' nel dizionario; SEMPRE = sempre OR dei termini simili.
 */
public final class Searcher {
    private final InvertedIndex index;
    private final KGramIndex kgrams;
    private final ModoFuzzy modo;

    public enum ModoFuzzy { NO, FALLBACK, SEMPRE }

    public Searcher(InvertedIndex index, ModoFuzzy modo) {
        this.index = index;
        this.kgrams = new KGramIndex(index.terms(), 3);
        this.modo = modo;
    }

    public Searcher(InvertedIndex index, boolean fuzzyFallback) {
        this(index, fuzzyFallback ? ModoFuzzy.FALLBACK : ModoFuzzy.NO);
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
        if (modo == ModoFuzzy.SEMPRE || (modo == ModoFuzzy.FALLBACK && !index.contains(parola))) {
            return kgrams.fuzzy(parola, 0.2, parola.length() <= 4 ? 1 : 2);
        }
        return index.contains(parola) ? List.of(parola) : List.of();
    }

    private PostingList postingsParola(String parola) {
        PostingList u = PostingList.VUOTA;
        for (String t : espandi(parola)) u = PostingList.union(u, index.postings(t));
        return u;
    }
}
