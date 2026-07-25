package com.palm3.assets_loader.assets.patchers;

import java.util.EnumSet;

/**
 * Defines a type of minecraft asset.
 */
public enum AssetType {
    ITEM_MODELS,
    BLOCK_MODELS,
    ALL_MODELS,
    BLOCK_STATE,
    LANG,
    SOUNDS;

    AssetType() {

    }

    @SuppressWarnings("all")
    public boolean isModel() {
        return EnumSet.of(ITEM_MODELS, BLOCK_MODELS, ALL_MODELS).contains(this);
    }

    public boolean isItemModel() {
        return EnumSet.of(ITEM_MODELS, ALL_MODELS).contains(this);
    }

    public boolean isBlockModel() {
        return EnumSet.of(BLOCK_MODELS, ALL_MODELS).contains(this);
    }
}
