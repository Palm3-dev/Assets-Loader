package com.palm3.assets_loader.assets;

/**
 * Used to create a couple of namespaces that need to be changed.
 *
 * @param oldNamespace The old namespace occurring in the resourcepack files.
 * @param newNamespace The new namespace that will replace the old one; also the name of the directory
 *                     inside the {@code assets} folder containing all the files (blockstates, models...).
 */
public record NamespaceCouple(String oldNamespace, String newNamespace) {}