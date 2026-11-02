package de.dpma.qd.service.dokumentenerstellung.verwaltung.api;

import de.dpma.qd.service.dokumentenerstellung.verwaltung.service.port.AktuellerBenutzer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/** Benutzerkennung für die Audit-Felder aus dem Keycloak-Token ({@code preferred_username}, sonst {@code sub}). */
@Component
class SecurityAktuellerBenutzer implements AktuellerBenutzer {

    @Override
    public String kennung() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwt) {
            String name = jwt.getToken().getClaimAsString("preferred_username");
            return name != null ? name : jwt.getName();
        }
        return auth == null ? "unbekannt" : auth.getName();
    }
}
