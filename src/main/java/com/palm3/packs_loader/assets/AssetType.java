package com.palm3.packs_loader.assets;

import java.io.File;
import java.util.EnumSet;

/**
 * Defines a type of minecraft asset.
 */
public enum AssetType {
    ITEM_MODELS("models" + File.separator + "item"),
    BLOCK_MODELS("models" + File.separator + "block"),
    ALL_MODELS("models"),
    BLOCK_STATE("blockstates"),
    LANG("lang"),
    SOUNDS("sounds.json");

    /**
     * The location of the asset inside the {@code pack_root/assets/<namespace>/} directory.
     * Can be a directory itself (like for {@code models/item}) or a file (like for {@code sounds.json}).
     * <br><b>NOTE:</b> if the current value is {@link #ALL_MODELS}, the value will be {@code models}, the main models directory!
     */
    public final String loc;

    AssetType(String loc) {
        this.loc = loc;
    }

    /**
     * @return {@code true} if the value of the enum is any type of model, otherwise {@code false}.
     */
    public boolean isModel() {
        return EnumSet.of(ITEM_MODELS, BLOCK_MODELS, ALL_MODELS).contains(this);
    }

    /**
     * @return {@code true} if the value of the enum is an item model (all or item), otherwise {@code false}.
     */
    public boolean isItemModel() {
        return EnumSet.of(ITEM_MODELS, ALL_MODELS).contains(this);
    }

    /**
     * @return {@code true} if the value of the enum is a block model (all or block), otherwise {@code false}.
     */
    public boolean isBlockModel() {
        return EnumSet.of(BLOCK_MODELS, ALL_MODELS).contains(this);
    }
}
