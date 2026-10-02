package ir;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Dizionario ordinato compresso con front coding a blocchi.
 * Termini a gruppi di BLOCCO: il primo e' scritto intero, gli altri come
 * (byte di prefisso in comune col precedente, byte di suffisso, suffisso). La ricerca fa una ricerca binaria
 * sulle teste dei blocchi e poi una scansione lineare dentro il blocco.
 */
public final class FrontCodedDictionary {
    private static final int BLOCCO = 8;

    private final byte[] dati;
    private final int[] offsetBlocchi;
    private final int n;

    public FrontCodedDictionary(List<String> terminiOrdinati) {
        n = terminiOrdinati.size();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        offsetBlocchi = new int[(n + BLOCCO - 1) / BLOCCO];
        byte[] prec = new byte[0];
        for (int i = 0; i < n; i++) {
            byte[] t = terminiOrdinati.get(i).getBytes(StandardCharsets.UTF_8);
            if (i % BLOCCO == 0) {
                offsetBlocchi[i / BLOCCO] = out.size();
                VByte.scrivi(out, t.length);
                out.writeBytes(t);
            } else {
                int p = 0;
                while (p < prec.length && p < t.length && prec[p] == t[p]) p++;
                VByte.scrivi(out, p);
                VByte.scrivi(out, t.length - p);
                out.write(t, p, t.length - p);
            }
            prec = t;
        }
        dati = out.toByteArray();
    }

    private FrontCodedDictionary(byte[] dati, int[] offsetBlocchi, int n) {
        this.dati = dati;
        this.offsetBlocchi = offsetBlocchi;
        this.n = n;
    }

    public void scrivi(DataOutputStream out) throws IOException {
        out.writeInt(n);
        out.writeInt(dati.length);
        out.write(dati);
        out.writeInt(offsetBlocchi.length);
        for (int o : offsetBlocchi) out.writeInt(o);
    }

    public static FrontCodedDictionary leggi(DataInputStream in) throws IOException {
        int n = in.readInt();
        byte[] dati = new byte[in.readInt()];
        in.readFully(dati);
        int[] off = new int[in.readInt()];
        for (int i = 0; i < off.length; i++) off[i] = in.readInt();
        return new FrontCodedDictionary(dati, off, n);
    }

    public int size() { return n; }

    /** Byte occupati: dati compressi + 4 byte di offset per blocco. */
    public int byteOccupati() { return dati.length + 4 * offsetBlocchi.length; }

    private String testaBlocco(int b) {
        int[] pos = {offsetBlocchi[b]};
        int len = VByte.leggi(dati, pos);
        return new String(dati, pos[0], len, StandardCharsets.UTF_8);
    }

    /** Id del termine (posizione nell'ordine alfabetico) oppure -1. */
    public int id(String termine) {
        int lo = 0, hi = offsetBlocchi.length - 1, blocco = -1;
        while (lo <= hi) { // ultimo blocco la cui testa e' <= termine
            int mid = (lo + hi) >>> 1;
            if (testaBlocco(mid).compareTo(termine) <= 0) { blocco = mid; lo = mid + 1; } else hi = mid - 1;
        }
        if (blocco < 0) return -1;
        int[] pos = {offsetBlocchi[blocco]};
        int len = VByte.leggi(dati, pos);
        byte[] cur = java.util.Arrays.copyOfRange(dati, pos[0], pos[0] + len);
        pos[0] += len;
        int id = blocco * BLOCCO;
        for (int k = 0; ; k++) {
            if (new String(cur, StandardCharsets.UTF_8).equals(termine)) return id + k;
            if (k + 1 == BLOCCO || id + k + 1 >= n) return -1;
            int p = VByte.leggi(dati, pos), s = VByte.leggi(dati, pos);
            byte[] nuovo = new byte[p + s];
            System.arraycopy(cur, 0, nuovo, 0, p);
            System.arraycopy(dati, pos[0], nuovo, p, s);
            pos[0] += s;
            cur = nuovo;
        }
    }

    public List<String> terms() {
        List<String> out = new ArrayList<>(n);
        int[] pos = {0};
        byte[] cur = new byte[0];
        for (int i = 0; i < n; i++) {
            if (i % BLOCCO == 0) {
                int len = VByte.leggi(dati, pos);
                cur = java.util.Arrays.copyOfRange(dati, pos[0], pos[0] + len);
                pos[0] += len;
            } else {
                int p = VByte.leggi(dati, pos), s = VByte.leggi(dati, pos);
                byte[] nuovo = new byte[p + s];
                System.arraycopy(cur, 0, nuovo, 0, p);
                System.arraycopy(dati, pos[0], nuovo, p, s);
                pos[0] += s;
                cur = nuovo;
            }
            out.add(new String(cur, StandardCharsets.UTF_8));
        }
        return out;
    }
}
