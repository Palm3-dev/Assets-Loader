package com.palm3.packs_loader;

/// Represents a mod loader.
public enum ModLoader {
    FORGE("forge"),
    NEOFORGE("neoforge"),
    FABRIC("fabric");

    public final String name;

    ModLoader(String name) {
        this.name = name;
    }
}