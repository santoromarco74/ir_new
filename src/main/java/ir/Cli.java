package ir;

import java.io.IOException;
import java.util.List;

/** Prova da riga di comando: java -cp target/classes ir.Cli "query" [--no-fuzzy] [--ocr] [--file]  (--file = indice persistente di ir.Persistenza) */
public final class Cli {
    public static void main(String[] args) throws IOException {
        List<String> opz = List.of(args).subList(1, args.length);
        Indice ix = Persistenza.apri(opz.contains("--ocr"), opz.contains("--file"));
        Searcher s = new Searcher(ix, !opz.contains("--no-fuzzy"));
        List<Integer> res = s.search(args[0]);
        System.out.println(res.size() + " risultati");
        for (int id : res.subList(0, Math.min(10, res.size()))) System.out.println("  " + ix.doc(id));
    }
}
