package com.palm3.assets_loader.assets.patchers;

import java.util.EnumSet;

public enum AssetType {
    ITEM_MODELS,
    BLOCK_MODELS,
    ALL_MODELS,
    BLOCK_STATE,
    LANG,
    SOUNDS;

    AssetType() {

    }

    public boolean isModel() {
        return EnumSet.of(ITEM_MODELS, BLOCK_MODELS, ALL_MODELS).contains(this);
    }
}
