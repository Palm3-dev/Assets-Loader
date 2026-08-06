package com.palm3.packs_loader.common;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.nio.file.Path;

@ParametersAreNonnullByDefault
public record JarIconCopyContext(Path jarFilePath, String iconFileName, Path iconDestinationPath, @Nullable String newIconFileName) {
}
