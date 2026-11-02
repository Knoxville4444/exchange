package de.dpma.qd.service.dokumentenerstellung.verwaltung.api;

import de.dpma.qd.service.dokumentenerstellung.verwaltung.api.Dtos.RestErrorInfo;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.fehler.FachlicherFehler;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.fehler.KonfliktException;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.fehler.NichtGefundenException;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.fehler.NichtUnterstuetzterInhaltException;
import de.dpma.qd.service.dokumentenerstellung.verwaltung.domain.fehler.UngueltigeAnfrageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

/** Bildet Fehler der Vorlagenverwaltung auf HTTP-Status und {@link RestErrorInfo} ab. */
@RestControllerAdvice(basePackageClasses = Fehlerbehandlung.class)
class Fehlerbehandlung {

    private static final Logger LOG = LoggerFactory.getLogger(Fehlerbehandlung.class);

    @ExceptionHandler(FachlicherFehler.class)
    ResponseEntity<RestErrorInfo> fachlich(FachlicherFehler fehler) {
        HttpStatus status = switch (fehler) {
            case NichtGefundenException e -> HttpStatus.NOT_FOUND;
            case KonfliktException e -> HttpStatus.CONFLICT;
            case UngueltigeAnfrageException e -> HttpStatus.BAD_REQUEST;
            case NichtUnterstuetzterInhaltException e -> HttpStatus.UNSUPPORTED_MEDIA_TYPE;
        };
        return antwort(status, fehler.errorId(), fehler.getMessage(), false, fehler.detail());
    }

    /** Parallele Änderung oder Unique-Constraint (z. B. gleichzeitig vergebene Versionsnummer). */
    @ExceptionHandler({OptimisticLockingFailureException.class, DataIntegrityViolationException.class})
    ResponseEntity<RestErrorInfo> gleichzeitig(RuntimeException e) {
        LOG.info("Konflikt durch parallele Änderung: {}", e.getMessage());
        return antwort(HttpStatus.CONFLICT, "GLEICHZEITIGE_AENDERUNG",
                "Die Daten wurden zwischenzeitlich geändert; bitte erneut versuchen", true, Map.of());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<RestErrorInfo> zuGross(MaxUploadSizeExceededException e) {
        return antwort(HttpStatus.CONTENT_TOO_LARGE, "DATEI_ZU_GROSS", e.getMessage(), false, Map.of());
    }

    private static ResponseEntity<RestErrorInfo> antwort(HttpStatus status, String errorId, String meldung, boolean retry,
                                                         Map<String, Object> detail) {
        return ResponseEntity.status(status).body(new RestErrorInfo(errorId, meldung, null, null, retry, detail));
    }
}
