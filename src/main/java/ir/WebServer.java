package ir;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Interfaccia web minimale: una barra di ricerca e due opzioni, sopra lo stesso {@link Searcher}.
 * Server HTTP della JDK (com.sun.net.httpserver), pagina HTML generata lato server, nessun framework.
 *
 * Uso: java -cp target/classes ir.WebServer [porta]   poi apri http://localhost:8080
 */
public final class WebServer {
    private final InvertedIndex base, corretto;
    private final Map<Boolean, Map<Searcher.ModoFuzzy, Searcher>> searcher = new HashMap<>();

    WebServer(InvertedIndex base) {
        this.base = base;
        this.corretto = OcrCorrector.applica(base, OcrCorrector.calcola(base));
        for (boolean ocr : new boolean[]{false, true}) {
            Map<Searcher.ModoFuzzy, Searcher> m = new EnumMap<>(Searcher.ModoFuzzy.class);
            for (Searcher.ModoFuzzy f : Searcher.ModoFuzzy.values()) m.put(f, new Searcher(ocr ? corretto : base, f));
            searcher.put(ocr, m);
        }
    }

    static InvertedIndex caricaCorpus() throws IOException {
        InvertedIndex ix = new InvertedIndex();
        List<String> righe = Files.readAllLines(Path.of("data/corpus.tsv"));
        for (String r : righe.subList(1, righe.size())) {
            String[] c = r.split("\t", 6);
            ix.add(c[4] + " " + c[5]);
        }
        return ix;
    }

    String pagina(String q, Searcher.ModoFuzzy modo, boolean ocr) {
        StringBuilder sb = new StringBuilder("<!doctype html><html lang=\"it\"><meta charset=\"utf-8\">"
                + "<title>Archivio bolle</title><body style=\"font-family:sans-serif;max-width:50em;margin:2em auto\">"
                + "<h1>Archivio bolle</h1><form method=\"get\"><input name=\"q\" size=\"50\" autofocus value=\"" + esc(q) + "\"> "
                + "<button>Cerca</button><br><label>Fuzzy <select name=\"fuzzy\">");
        for (Searcher.ModoFuzzy f : Searcher.ModoFuzzy.values()) {
            sb.append("<option value=\"").append(f).append(f == modo ? "\" selected>" : "\">").append(etichetta(f)).append("</option>");
        }
        sb.append("</select></label> <label><input type=\"checkbox\" name=\"ocr\" value=\"1\"")
                .append(ocr ? " checked" : "").append("> correzione OCR</label></form>");
        if (!q.isBlank()) {
            List<Integer> res = searcher.get(ocr).get(modo).search(q);
            sb.append("<p>").append(res.size()).append(" risultati (mostrati i primi 50)</p><ul>");
            for (int id : res.subList(0, Math.min(50, res.size()))) sb.append("<li>").append(esc(base.doc(id))).append("</li>");
            sb.append("</ul>");
        }
        return sb.append("<p><small>Usa * come jolly (es. <code>lava*</code>, <code>*frigo*</code>).</small></p></body></html>").toString();
    }

    private static String etichetta(Searcher.ModoFuzzy f) {
        return switch (f) {
            case NO -> "no";
            case FALLBACK -> "solo se la parola non esiste";
            case SEMPRE -> "sempre";
        };
    }

    static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    static Map<String, String> parametri(String query) {
        Map<String, String> m = new HashMap<>();
        if (query == null) return m;
        for (String kv : query.split("&")) {
            int i = kv.indexOf('=');
            if (i > 0) m.put(URLDecoder.decode(kv.substring(0, i), StandardCharsets.UTF_8), URLDecoder.decode(kv.substring(i + 1), StandardCharsets.UTF_8));
        }
        return m;
    }

    public static void main(String[] args) throws IOException {
        WebServer w = new WebServer(caricaCorpus());
        int porta = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        HttpServer srv = HttpServer.create(new InetSocketAddress("127.0.0.1", porta), 0);
        srv.createContext("/", ex -> {
            Map<String, String> p = parametri(ex.getRequestURI().getRawQuery());
            Searcher.ModoFuzzy modo;
            try { modo = Searcher.ModoFuzzy.valueOf(p.getOrDefault("fuzzy", "FALLBACK")); }
            catch (IllegalArgumentException e) { modo = Searcher.ModoFuzzy.FALLBACK; }
            byte[] body = w.pagina(p.getOrDefault("q", ""), modo, p.containsKey("ocr")).getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            ex.sendResponseHeaders(200, body.length);
            ex.getResponseBody().write(body);
            ex.close();
        });
        srv.start();
        System.out.println("http://localhost:" + porta);
    }
}
