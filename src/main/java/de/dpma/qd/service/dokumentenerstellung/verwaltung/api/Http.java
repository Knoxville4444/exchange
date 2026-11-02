package de.dpma.qd.service.dokumentenerstellung.verwaltung.api;

import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.fehler.UngueltigeAnfrageException;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.service.HochgeladeneDatei;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.service.InhaltDaten;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;

/** Hilfen für Uploads und Binär-Antworten. */
final class Http {

    static final String BASIS = "/api/dokumentenerstellung/v1";
    static final String X_FORMULAR_VERSION = "X-Formular-Version";

    private Http() {
    }

    static HochgeladeneDatei datei(MultipartFile datei) {
        if (datei == null) {
            return null;
        }
        if (datei.isEmpty()) {
            throw new UngueltigeAnfrageException("INHALT_LEER", "Datei '" + datei.getOriginalFilename() + "' ist leer");
        }
        try {
            return new HochgeladeneDatei(datei.getOriginalFilename(), datei.getBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static ResponseEntity<byte[]> binaer(InhaltDaten inhalt) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(inhalt.ref().format().mimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(inhalt.dateiname()).build().toString())
                .header(X_FORMULAR_VERSION, inhalt.version().toString())
                .body(inhalt.daten());
    }
}
