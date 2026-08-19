package com.palm3.packs_loader.common;

/**
 * Represents a mod loader.
 * <br><b>NOTE:</b> this enum represents NeoForge 1.20.5+ (with namespace {@code c}) since before that release
 * the NeoForge namespace was still {@code forge}.
 */
public enum ModLoader {
    FORGE("forge"),
    NEOFORGE("neoforge"),
    FABRIC("fabric");

    public final String name;

    ModLoader(String name) {
        this.name = name;
    }

    public String getNamespace() {
        return switch (this) {
            case FABRIC, NEOFORGE -> "c";
            case FORGE -> "forge";
        };
    }
}