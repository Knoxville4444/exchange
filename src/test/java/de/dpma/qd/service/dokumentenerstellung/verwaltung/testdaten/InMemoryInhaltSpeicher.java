package de.dpma.qd.service.dokumentenerstellung.verwaltung.testdaten;

import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.InhaltRef;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.Inhaltsformat;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.service.port.InhaltSpeicher;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;

/** Inhaltsspeicher im Speicher, dedupliziert wie der Oracle-Adapter über SHA-256 und Format. */
public final class InMemoryInhaltSpeicher implements InhaltSpeicher {

    private final Map<String, InhaltRef> nachPruefsumme = new HashMap<>();
    private final Map<Long, byte[]> daten = new HashMap<>();
    private long naechsteId = 1;

    @Override
    public InhaltRef speichere(byte[] inhalt, Inhaltsformat format) {
        String sha = sha256(inhalt);
        return nachPruefsumme.computeIfAbsent(format + ":" + sha, k -> {
            InhaltRef ref = new InhaltRef(naechsteId++, sha, format, inhalt.length);
            daten.put(ref.id(), inhalt.clone());
            return ref;
        });
    }

    @Override
    public byte[] lade(InhaltRef ref) {
        return daten.get(ref.id()).clone();
    }

    public int anzahl() {
        return daten.size();
    }

    static String sha256(byte[] inhalt) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(inhalt));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
