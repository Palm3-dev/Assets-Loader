package com.palm3.packs_loader.data;

import com.mojang.logging.LogUtils;
import com.palm3.packs_loader.common.FilesCopier;
import com.palm3.packs_loader.logging.PrettyLogging;
import net.minecraft.server.packs.PackType;
import net.neoforged.neoforge.event.AddPackFindersEvent;

import java.nio.file.Path;

import static com.palm3.packs_loader.PacksLoaderMain.DEF_PL_PARAMS;
import static com.palm3.packs_loader.logging.PrettyLogging.DEF_LINE;

@Deprecated(forRemoval = true)
public class DataLoader {
    private static final PrettyLogging PL = new PrettyLogging(LogUtils.getLogger(), DEF_PL_PARAMS);
    private static final FilesCopier COPIER = new FilesCopier(PL);

    private final Path tempDirectoryPath;
    private final String loaderModName;  // Mod name?

    @Deprecated(forRemoval = true)
    public DataLoader(String mod_id, Path tempDirectory, boolean hideContent) {
        loaderModName = mod_id;
        this.tempDirectoryPath = tempDirectory;
        COPIER.createDirectory(tempDirectoryPath, hideContent);
    }

    @Deprecated(forRemoval = true)
    public void loadDataPack(AddPackFindersEvent event, String packFolderName) {
        if (event.getPackType() != PackType.SERVER_DATA) return;

        Path packPath = this.tempDirectoryPath.resolve(packFolderName);

        PL.logCenteredI("Loading datapack '" + packFolderName + "' for mod '" + this.loaderModName + "'", DEF_LINE);


    }
}
