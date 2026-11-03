package de.dpma.qd.service.dokumentenerstellung.impl;

import java.time.Clock;
import java.time.Instant;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/** Liefert den aktuellen Zeitpunkt und erzeugt Audit-Angaben mit der Benutzerkennung aus dem Token. */
@Component
public class AuditErzeuger {

    private final Clock uhr;

    public AuditErzeuger(Clock uhr) {
        this.uhr = uhr;
    }

    public Instant jetzt() {
        return uhr.instant();
    }

    public Instant stichtagOderJetzt(Instant stichtag) {
        return stichtag == null ? jetzt() : stichtag;
    }

    public Audit neu() {
        return Audit.neu(jetzt(), benutzer());
    }

    public Audit geaendert(Audit audit) {
        return audit.geaendert(jetzt(), benutzer());
    }

    private String benutzer() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwt) {
            String name = jwt.getToken().getClaimAsString("preferred_username");
            return name != null ? name : jwt.getName();
        }
        return auth == null ? "unbekannt" : auth.getName();
    }
}
