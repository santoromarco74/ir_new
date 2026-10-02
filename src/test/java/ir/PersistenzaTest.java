package ir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PersistenzaTest {
    private InvertedIndex indice() {
        InvertedIndex ix = new InvertedIndex();
        Random r = new Random(5);
        String[] parole = {"tv", "led", "lavatrice", "lavasciuga", "slim", "cuffia", "bt", "nero", "7kg", "8gb", "smart", "perché", "friggitrice", "frigo"};
        for (int d = 0; d < 300; d++) {
            StringBuilder sb = new StringBuilder("cod" + d + " ");
            for (int k = 0; k < 5; k++) sb.append(parole[r.nextInt(parole.length)]).append(' ');
            ix.add(sb.toString());
        }
        return ix;
    }

    @Test
    void salvaERicaricaDannoLoStessoIndice(@TempDir Path dir) throws IOException {
        InvertedIndex ix = indice();
        Path f = dir.resolve("indice.bin");
        new CompressedIndex(ix).salva(f);
        CompressedIndex c = CompressedIndex.carica(f);
        assertEquals(ix.size(), c.size());
        assertEquals(ix.terms(), c.terms());
        for (int d = 0; d < ix.size(); d++) assertEquals(ix.doc(d), c.doc(d));
        for (String t : ix.terms()) assertArrayEquals(ix.postings(t).toArray(), c.postings(t).toArray());
    }

    @Test
    void laRicercaSuIndiceCaricatoDaFileEquivaleAQuellaInMemoria(@TempDir Path dir) throws IOException {
        InvertedIndex ix = indice();
        Path f = dir.resolve("indice.bin");
        new CompressedIndex(ix).salva(f);
        Indice c = CompressedIndex.carica(f);
        for (Searcher.ModoFuzzy m : Searcher.ModoFuzzy.values()) {
            Searcher a = new Searcher(ix, m), b = new Searcher(c, m);
            for (String q : List.of("lavatrice 7kg", "lava*", "*frig*", "friggitrici", "perché smart", "assente")) {
                assertEquals(a.search(q), b.search(q), q + " " + m);
            }
        }
    }

    @Test
    void fileNonValidoERifiutato(@TempDir Path dir) throws IOException {
        Path f = dir.resolve("falso.bin");
        Files.write(f, new byte[]{1, 2, 3, 4, 5, 6, 7, 8});
        assertThrows(IOException.class, () -> CompressedIndex.carica(f));
    }
}
