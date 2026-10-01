package ir;

import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * Versione compressa e a sola lettura di un InvertedIndex (mia): dizionario con front coding,
 * postings come gap + VByte in un unico array. {@link #postings} decodifica al volo e restituisce una PostingList.
 */
public final class CompressedIndex {
    private final FrontCodedDictionary dizionario;
    private final byte[] postings;
    private final int[] offset; // offset[id]..offset[id+1] = postings del termine id

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
            rawDiz += 1 + termini.get(i).getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
        }
        offset[termini.size()] = out.size();
        postings = out.toByteArray();
        byteNonCompressi = raw;
        byteDizionarioNonCompresso = rawDiz;
    }

    public boolean contains(String termine) { return dizionario.id(termine) >= 0; }

    public List<String> terms() { return dizionario.terms(); }

    public PostingList postings(String termine) {
        int id = dizionario.id(termine);
        if (id < 0) return PostingList.VUOTA;
        return new PostingList(VByte.decodificaPostings(java.util.Arrays.copyOfRange(postings, offset[id], offset[id + 1])));
    }

    public long dizionarioCompresso() { return dizionario.byteOccupati(); }
    /** Dizionario non compresso: byte UTF-8 dei termini + 1 byte di lunghezza ciascuno. */
    public long dizionarioNonCompresso() { return byteDizionarioNonCompresso; }
    public long postingsCompressi() { return postings.length; }
    /** Postings non compressi: 4 byte per docId. */
    public long postingsNonCompressi() { return byteNonCompressi; }
}
