package com.palm3.packs_loader.data;

import com.mojang.logging.LogUtils;
import com.palm3.packs_loader.common.CommonMethods;
import com.palm3.packs_loader.logging.PrettyLogging;
import net.minecraft.server.packs.PackType;
import net.neoforged.neoforge.event.AddPackFindersEvent;

import java.nio.file.Path;

import static com.palm3.packs_loader.PacksLoaderMain.DEF_PL_PARAMS;

public class DataLoader {
    private static final PrettyLogging PL = new PrettyLogging(LogUtils.getLogger(), DEF_PL_PARAMS);
    private static final CommonMethods.Logged CM = new CommonMethods.Logged(PL);

    private final String tempDirectory;
    private final Path tempDirectoryPath;
    private final boolean tempDirIsHidden;
    private final String loaderModName;  // Mod name?

    public DataLoader(String tempDirectory, boolean hidden, String mod_id) {
        this.tempDirectory = tempDirectory;
        tempDirIsHidden = hidden;
        loaderModName = mod_id;
        this.tempDirectoryPath = CM.createDirectoryAndGetPath(tempDirectory, hidden);
    }

    public void loadDataPack(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA)
            return;

        //do
    }
}
