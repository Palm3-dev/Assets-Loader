package com.palm3.packs_loader.common;

import net.minecraft.server.packs.PackType;

import javax.annotation.ParametersAreNonnullByDefault;
import java.nio.file.Path;

@ParametersAreNonnullByDefault
public record JarFilesCopyContext(Path jarFilePath, Path filesDestinationPath, String namespaceToCopy, PackType packType, boolean forceCopy, FilesCopier.LogCopyOption logCopyOption) {
}
