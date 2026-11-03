package de.dpma.qd.service.dokumentenerstellung.impl;

/** Technisches Format eines gespeicherten Inhalts. */
public enum Inhaltsformat {
    ODT("application/vnd.oasis.opendocument.text"),
    PDF("application/pdf"),
    XSD("application/xml");

    private final String mimeType;

    Inhaltsformat(String mimeType) {
        this.mimeType = mimeType;
    }

    public String mimeType() {
        return mimeType;
    }
}
