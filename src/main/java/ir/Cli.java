package ir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Prova da riga di comando: java -cp target/classes ir.Cli "query" [--no-fuzzy] */
public final class Cli {
    public static void main(String[] args) throws IOException {
        InvertedIndex ix = new InvertedIndex();
        List<String> righe = Files.readAllLines(Path.of("data/corpus.tsv"));
        for (String r : righe.subList(1, righe.size())) {
            String[] c = r.split("\t", 6);
            ix.add(c[4] + " " + c[5]);
        }
        Searcher s = new Searcher(ix, args.length < 2 || !args[1].equals("--no-fuzzy"));
        List<Integer> res = s.search(args[0]);
        System.out.println(res.size() + " risultati");
        for (int id : res.subList(0, Math.min(10, res.size()))) System.out.println("  " + ix.doc(id));
    }
}
