package com.palm3.assets_loader.assets;

public record PackLoadingContext(AssetsLoader assetsLoader, JarLoadingInfos jarLoadingInfos, ResourcePackInfos packInfos, boolean distinguishEventFireTime) {}
