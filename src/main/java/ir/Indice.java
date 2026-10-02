package ir;

import java.util.List;

/** Vista di sola lettura di un indice: la usa {@link Searcher}, la implementano InvertedIndex (in memoria) e CompressedIndex (da file). */
public interface Indice {
    List<String> terms();

    boolean contains(String term);

    PostingList postings(String term);

    String doc(int id);

    int size();
}
