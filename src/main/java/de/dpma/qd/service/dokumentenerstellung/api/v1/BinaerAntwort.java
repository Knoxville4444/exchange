package de.dpma.qd.service.dokumentenerstellung.api.v1;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import de.dpma.qd.service.dokumentenerstellung.impl.Inhaltsformat;
import de.dpma.qd.service.dokumentenerstellung.impl.VersionsNummer;

/** Baut Binär-Antworten mit Dateiname und Header {@code X-Formular-Version}. */
final class BinaerAntwort {

    static final String X_FORMULAR_VERSION = "X-Formular-Version";

    private BinaerAntwort() {
    }

    static ResponseEntity<Resource> von(byte[] daten, Inhaltsformat format, String dateiname, VersionsNummer version) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(format.mimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(dateiname).build().toString())
                .header(X_FORMULAR_VERSION, version.toString())
                .body(new ByteArrayResource(daten));
    }
}
