package com.palm3.packs_loader.common;

import javax.annotation.Nullable;

public enum PackType {
    ASSETS("assets"),
    DATAPACK("data"),
    BOTH("both");

    private final String folderName;

    PackType(String folderName) {
        this.folderName = folderName;
    }

    /**
     * @return the folder name of the corresponding pack type. {@code null} if the enum value is {@link PackType#BOTH}.
     */
    public @Nullable String absoluteFolderName() {
        if (this == PackType.BOTH) return null;
        return folderName;
    }

    /**
     * @return the folder name of the corresponding pack type. {@code "both"} if the enum value is {@link PackType#BOTH}.
     */
    public @Nullable String folderName() {
        return folderName;
    }
}
