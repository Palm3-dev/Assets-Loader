package com.palm3.assets_loader.assets;

import net.minecraft.network.chat.Component;

public record ResourcePackInfos(Component packTitle, Component packDescription, boolean requiredPack, boolean deletePack) {
}
