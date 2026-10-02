package ir;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Versione compressa e a sola lettura di un InvertedIndex (mia): dizionario con front coding,
 * postings come gap + VByte in un unico array. {@link #postings} decodifica al volo e restituisce una PostingList.
 * Si puo' salvare su file e ricaricare ({@link #salva}, {@link #carica}): e' l'indice persistente del sistema.
 * Nel file ci sono anche i testi dei documenti (non compressi), per poter mostrare i risultati.
 * L'indice a trigrammi non e' salvato: lo ricostruisce il Searcher dai termini a ogni avvio.
 */
public final class CompressedIndex implements Indice {
    private static final int MAGIC = 0x49524958; // "IRIX"
    private static final int VERSIONE = 1;

    private final FrontCodedDictionary dizionario;
    private final byte[] postings;
    private final int[] offset; // offset[id]..offset[id+1] = postings del termine id
    private final List<String> docs;

    private final long byteDizionarioNonCompresso;
    private final long byteNonCompressi;

    public CompressedIndex(InvertedIndex ix) {
        List<String> termini = ix.terms();
        dizionario = new FrontCodedDictionary(termini);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        offset = new int[termini.size() + 1];
        long raw = 0, rawDiz = 0;
        for (int i = 0; i < termini.size(); i++) {
            offset[i] = out.size();
            int[] p = ix.postings(termini.get(i)).toArray();
            out.writeBytes(VByte.codificaPostings(p));
            raw += 4L * p.length;
            rawDiz += 1 + termini.get(i).getBytes(StandardCharsets.UTF_8).length;
        }
        offset[termini.size()] = out.size();
        postings = out.toByteArray();
        byteNonCompressi = raw;
        byteDizionarioNonCompresso = rawDiz;
        docs = new ArrayList<>(ix.size());
        for (int d = 0; d < ix.size(); d++) docs.add(ix.doc(d));
    }

    private CompressedIndex(FrontCodedDictionary dizionario, byte[] postings, int[] offset, List<String> docs) {
        this.dizionario = dizionario;
        this.postings = postings;
        this.offset = offset;
        this.docs = docs;
        this.byteDizionarioNonCompresso = 0;
        this.byteNonCompressi = 0;
    }

    public void salva(Path file) throws IOException {
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(file)))) {
            out.writeInt(MAGIC);
            out.writeInt(VERSIONE);
            out.writeInt(docs.size());
            for (String d : docs) out.writeUTF(d);
            dizionario.scrivi(out);
            out.writeInt(postings.length);
            out.write(postings);
            out.writeInt(offset.length);
            for (int o : offset) out.writeInt(o);
        }
    }

    public static CompressedIndex carica(Path file) throws IOException {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
            if (in.readInt() != MAGIC) throw new IOException("non e' un indice di questo programma: " + file);
            if (in.readInt() != VERSIONE) throw new IOException("versione del file non supportata: " + file);
            List<String> docs = new ArrayList<>();
            for (int i = in.readInt(); i > 0; i--) docs.add(in.readUTF());
            FrontCodedDictionary d = FrontCodedDictionary.leggi(in);
            byte[] post = new byte[in.readInt()];
            in.readFully(post);
            int[] off = new int[in.readInt()];
            for (int i = 0; i < off.length; i++) off[i] = in.readInt();
            return new CompressedIndex(d, post, off, docs);
        }
    }

    @Override
    public boolean contains(String termine) { return dizionario.id(termine) >= 0; }

    @Override
    public List<String> terms() { return dizionario.terms(); }

    @Override
    public String doc(int id) { return docs.get(id); }

    @Override
    public int size() { return docs.size(); }

    @Override
    public PostingList postings(String termine) {
        int id = dizionario.id(termine);
        if (id < 0) return PostingList.VUOTA;
        return new PostingList(VByte.decodificaPostings(java.util.Arrays.copyOfRange(postings, offset[id], offset[id + 1])));
    }

    public long dizionarioCompresso() { return dizionario.byteOccupati(); }
    /** Dizionario non compresso: byte UTF-8 dei termini + 1 byte di lunghezza ciascuno (0 se l'indice e' stato caricato da file). */
    public long dizionarioNonCompresso() { return byteDizionarioNonCompresso; }
    public long postingsCompressi() { return postings.length; }
    /** Postings non compressi: 4 byte per docId (0 se l'indice e' stato caricato da file). */
    public long postingsNonCompressi() { return byteNonCompressi; }
}
