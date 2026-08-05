package com.palm3.packs_loader;

import com.mojang.logging.LogUtils;
import com.palm3.packs_loader.logging.Markers;
import com.palm3.packs_loader.logging.PrettyLogging;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.fml.loading.FMLLoader;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.palm3.packs_loader.PacksLoaderMain.*;

@EventBusSubscriber
public class IncompatibleModsRemover {
    private static final PrettyLogging PL = new PrettyLogging(LogUtils.getLogger(), DEF_PL_PARAMS);
    public static final Path INCOMPATIBLE_JARS_DIR = MOD_DIR.resolve("incompatible_loader_jars");
    public static final List<String> UNSUPPORTED_JARS = new ArrayList<>();
    private static boolean eventAlreadyFired = false;

    static {
        try {
            PL.logI("Creating directory for incompatible jars in mods folder.", Markers.INIT.marker);
            Files.createDirectories(INCOMPATIBLE_JARS_DIR);
        } catch (IOException e) {
            PL.logE("Exception caught during 'incompatible_loader_jars' directory creation: " + e, Markers.INIT.marker);
        }
    }

    private static boolean isInvalidNeo(String issueTranslationKey) {
        return issueTranslationKey.equals("fml.modloadingissue.brokenfile.minecraft_forge") || issueTranslationKey.equals("fml.modloadingissue.brokenfile.fabric");
    }

    //todo
    private static boolean isInvalidForge(String translationKey) {
        return false;
    }

    //todo
    private static boolean isInvalidFabric() {
        return false;
    }

    protected static void searchIncompatibilities() {
        PL.logI("Searching for incompatible loader mods.", Markers.INIT.marker);
        FMLLoader.getLoadingModList().getModLoadingIssues().forEach(loadingIssue -> {
            if (loadingIssue.affectedPath() != null) {
                String issuedFileString = loadingIssue.affectedPath().getFileName().toString();
                switch (MOD_LOADER) {
                    case FORGE -> {
                        if (isInvalidForge(loadingIssue.translationKey())) {
                            PL.logI("Found incompatible mod jar file (FABRIC/NEO): " + issuedFileString, Markers.SEARCH.marker);
                            UNSUPPORTED_JARS.add(issuedFileString);
                        }
                    }
                    case NEOFORGE -> {
                        if (isInvalidNeo(loadingIssue.translationKey())) {
                            PL.logI("Found incompatible mod jar file (FABRIC/FORGE): " + issuedFileString, Markers.SEARCH.marker);
                            UNSUPPORTED_JARS.add(issuedFileString);
                        }
                    }
                    case FABRIC -> {
                        if (isInvalidFabric()) {
                            PL.logI("Found incompatible mod jar file (FORGE/NEO): " + issuedFileString, Markers.SEARCH.marker);
                            UNSUPPORTED_JARS.add(issuedFileString);
                        }
                    }
                }
            }
        });
    }

    protected static void moveIncompatibilities() {
        UNSUPPORTED_JARS.forEach(jarFile -> {
            try {
                PL.logI("Moving incompatible loader jar: '" + jarFile + "'", Markers.MOVE.marker);
                Files.move(MOD_DIR.resolve(jarFile), INCOMPATIBLE_JARS_DIR.resolve(jarFile));
            } catch (IOException e) {
                PL.logE("Exception caught during incompatible file transfer in 'incompatible_loader_jars' directory: " + e, Markers.MOVE.marker);
            }
        });
    }

    @SubscribeEvent
    public static void loadCompleteEvent(final FMLLoadCompleteEvent event) {
        if (!eventAlreadyFired) {
            searchIncompatibilities();
            moveIncompatibilities();
        }
        eventAlreadyFired = true;  // Idk, fires two times ¯\_(ツ)_/¯
    }
}
