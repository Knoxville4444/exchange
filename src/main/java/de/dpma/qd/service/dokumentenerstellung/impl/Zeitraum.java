package de.dpma.qd.service.dokumentenerstellung.impl;


import java.time.Instant;
import java.util.Objects;

/**
 * Halboffener Zeitraum {@code [ab, bis)}. Fehlt {@code ab}, beginnt er in der unendlichen Vergangenheit;
 * fehlt {@code bis}, ist er unbefristet.
 */
public record Zeitraum(Instant ab, Instant bis) {

    public Zeitraum {
        if (ab != null && bis != null && !bis.isAfter(ab)) {
            throw FachlicherFehler.ungueltig("ZEITRAUM_UNGUELTIG", "bis (" + bis + ") muss nach ab (" + ab + ") liegen");
        }
    }

    public static Zeitraum unbegrenzt() {
        return new Zeitraum(null, null);
    }

    public boolean enthaelt(Instant zeitpunkt) {
        return hatBegonnen(zeitpunkt) && !istBeendet(zeitpunkt);
    }

    public boolean hatBegonnen(Instant zeitpunkt) {
        return ab == null || !zeitpunkt.isBefore(ab);
    }

    public boolean istBeendet(Instant zeitpunkt) {
        return bis != null && !zeitpunkt.isBefore(bis);
    }

    /**
     * Prüft, ob zwei Zeiträume sich vor {@code jetzt} identisch verhalten. Änderungen an Zeiträumen dürfen
     * nur in die Zukunft wirken – sonst wäre die Vergangenheit nicht mehr rekonstruierbar.
     *
     * @param alt  bisheriger Zeitraum ({@code null} = unbegrenzt)
     * @param neu  neuer Zeitraum ({@code null} = unbegrenzt)
     */
    public static boolean gleicheVergangenheit(Zeitraum alt, Zeitraum neu, Instant jetzt) {
        Zeitraum a = alt == null ? unbegrenzt() : alt;
        Zeitraum n = neu == null ? unbegrenzt() : neu;
        Instant endeA = a.bis != null && a.bis.isBefore(jetzt) ? a.bis : jetzt;
        Instant endeN = n.bis != null && n.bis.isBefore(jetzt) ? n.bis : jetzt;
        boolean leerA = a.ab != null && !a.ab.isBefore(endeA);
        boolean leerN = n.ab != null && !n.ab.isBefore(endeN);
        if (leerA || leerN) {
            return leerA == leerN;
        }
        return Objects.equals(a.ab, n.ab) && endeA.equals(endeN);
    }
}
