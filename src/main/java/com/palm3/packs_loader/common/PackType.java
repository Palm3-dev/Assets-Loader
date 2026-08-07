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
    public String folderName() {
        return folderName;
    }

    /**
     * Converts this pack type to the minecraft one {@link net.minecraft.server.packs.PackType}.
     */
    public net.minecraft.server.packs.PackType toMcPackType() {
        switch (this) {
            case ASSETS -> {
                return net.minecraft.server.packs.PackType.CLIENT_RESOURCES;
            }

            case DATAPACK -> {
                return net.minecraft.server.packs.PackType.SERVER_DATA;
            }

            case BOTH -> {
                return null;
            }
        }
    }
}
