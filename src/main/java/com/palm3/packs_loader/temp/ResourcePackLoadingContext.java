package com.palm3.packs_loader.temp;

import com.palm3.packs_loader.assets.AssetsLoader;

/**
 * Record holds the necessary dependencies to load a pack.
 * @param assetsLoader The {@link AssetsLoader} instance, with your mod_id and the main temporary directory for packs.
 * @param jarLoadingInfos The {@link JarLoadingInfos} instance, representing all the jar and pack loader infos (icon, jar files, namespaces).
 * @param packInfos The {@link ResourcePackInfos} instance, containing the final pack infos.
 */
public record ResourcePackLoadingContext(AssetsLoader assetsLoader, JarLoadingInfos jarLoadingInfos, ResourcePackInfos packInfos) {}
