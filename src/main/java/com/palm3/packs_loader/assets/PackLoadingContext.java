package com.palm3.packs_loader.assets;

/**
 * Record holds the necessary dependencies to load a pack.
 * @param assetsLoader The {@link AssetsLoader} instance, with your mod_id and the main temporary directory for packs.
 * @param jarLoadingInfos The {@link JarLoadingInfos} instance, representing all the jar and pack loader infos (icon, jar files, namespaces).
 * @param packInfos The {@link ResourcePackInfos} instance, containing the final pack infos.
 * @param distinguishEventFireTime If the {@link net.neoforged.neoforge.event.AddPackFindersEvent} firing moment should be distinguished (can be fired
 *                                 when mod is loaded and when packs are reloaded in-game). When true, the file copy will be avoided at resource reload.
 */
public record PackLoadingContext(AssetsLoader assetsLoader, JarLoadingInfos jarLoadingInfos, ResourcePackInfos packInfos, boolean distinguishEventFireTime) {}
