package de.dpma.qd.service.dokumentenerstellung.api.v1;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import de.dpma.qd.service.dokumentenerstellung.api.v1.model.RestErrorInfoDto;
import de.dpma.qd.service.dokumentenerstellung.impl.FachlicherFehler;

/**
 * Bildet Fehler der Vorlagenverwaltung auf HTTP-Status und {@link RestErrorInfoDto} ab.
 * <p>
 * TODO (OP-04): Mit dem {@code ServiceExceptionHandler} aus {@code utils-spring-lib} abgleichen und ggf. dorthin überführen.
 */
@RestControllerAdvice(basePackageClasses = VorlagenExceptionHandler.class)
public class VorlagenExceptionHandler {

    private final Logger log = LogManager.getLogger(getClass());

    @ExceptionHandler(FachlicherFehler.class)
    ResponseEntity<RestErrorInfoDto> fachlich(FachlicherFehler fehler) {
        HttpStatus status = switch (fehler.art()) {
            case NICHT_GEFUNDEN -> HttpStatus.NOT_FOUND;
            case KONFLIKT -> HttpStatus.CONFLICT;
            case UNGUELTIG -> HttpStatus.BAD_REQUEST;
            case FORMAT_NICHT_UNTERSTUETZT -> HttpStatus.UNSUPPORTED_MEDIA_TYPE;
        };
        return antwort(status, fehler.errorId(), fehler.getMessage(), false, fehler.detail());
    }

    /** Parallele Änderung oder Unique-Constraint (z. B. gleichzeitig vergebene Versionsnummer). */
    @ExceptionHandler({OptimisticLockingFailureException.class, DataIntegrityViolationException.class})
    ResponseEntity<RestErrorInfoDto> gleichzeitig(RuntimeException e) {
        log.info("Konflikt durch parallele Änderung: {}", e.getClass().getSimpleName());
        return antwort(HttpStatus.CONFLICT, "GLEICHZEITIGE_AENDERUNG", "Die Daten wurden zwischenzeitlich geändert; bitte erneut versuchen", true, Map.of());
    }

    private static ResponseEntity<RestErrorInfoDto> antwort(HttpStatus status, String errorId, String meldung, boolean retry, Map<String, Object> detail) {
        return ResponseEntity.status(status).body(new RestErrorInfoDto(errorId, meldung, null, null, retry, detail.isEmpty() ? null : detail));
    }
}
