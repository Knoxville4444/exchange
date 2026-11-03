package de.dpma.qd.service.dokumentenerstellung.impl;

/**
 * Gespeicherter Status einer Version.
 * <ul>
 *     <li>{@code DRAFT}: Entwurf, änderbar und löschbar</li>
 *     <li>{@code FREIGEGEBEN}: unveränderlich; Sichtbarkeit wird über den Zeitraum gesteuert</li>
 * </ul>
 */
public enum VersionStatus {
    DRAFT,
    FREIGEGEBEN
}
