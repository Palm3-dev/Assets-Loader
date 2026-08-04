package com.palm3.packs_loader.assets;

import net.minecraft.network.chat.Component;

/**
 * Holds the final resourcepack infos.
 * @param packFolderName The name of the resourcepack folder root (containing the {@code assets} folder, {@code pack.png} etc...).
 * @param packTitle The in-game title of the resourcepack.
 * @param packDescription The in-game description of the resourcepack.
 * @param requiredPack If the pack is required and thus cannot be disabled in-game.
 * @param deletePack If the pack should be deleted after the game is closed.
 */
public record ResourcePackInfos(String packFolderName, Component packTitle, Component packDescription, boolean requiredPack, boolean deletePack) {}
