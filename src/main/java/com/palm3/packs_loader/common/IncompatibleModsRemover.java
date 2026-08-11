package com.palm3.packs_loader.common;

import com.mojang.logging.LogUtils;
import com.palm3.packs_loader.logging.Markers;
import com.palm3.packs_loader.logging.PrettyLogging;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.fml.loading.FMLLoader;

import java.io.FileNotFoundException;
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
            PL.logI("Creating directory for incompatible jars in main mods folder.", Markers.INIT.marker);
            Files.createDirectories(INCOMPATIBLE_JARS_DIR);
        } catch (IOException e) {
            throw new RuntimeException("IOException caught during 'incompatible_loader_jars' directory creation: " + e);
        }
    }

    protected static boolean isInvalidNeo(String issueTranslationKey) {
        return issueTranslationKey.equals("fml.modloadingissue.brokenfile.minecraft_forge") || issueTranslationKey.equals("fml.modloadingissue.brokenfile.fabric");
    }

    /**
     * Searches and saves in the {@link IncompatibleModsRemover#UNSUPPORTED_JARS} the incompatible jars names.
     */
    protected static void searchIncompatibilities() {
        PL.logI("Searching for incompatible loader mods.", Markers.INIT.marker);
        FMLLoader.getLoadingModList().getModLoadingIssues().forEach(loadingIssue -> {
            if (loadingIssue.affectedPath() != null) {
                String issuedFileString = loadingIssue.affectedPath().getFileName().toString();
                switch (MOD_LOADER) {
                    case FORGE -> {
                        /*
                        if (isInvalidForge(loadingIssue.translationKey())) {
                            PL.logI("Found incompatible mod jar file (FABRIC/NEO): " + issuedFileString, Markers.SEARCH.marker);
                            UNSUPPORTED_JARS.add(issuedFileString);
                        }*/
                        PL.logI("Forge: todo");
                    }
                    case NEOFORGE -> {
                        if (isInvalidNeo(loadingIssue.translationKey())) {
                            PL.logI("Found incompatible mod jar file (FABRIC/FORGE): " + issuedFileString, Markers.SEARCH.marker);
                            UNSUPPORTED_JARS.add(issuedFileString);
                        }
                    }
                    case FABRIC -> {
                        /*if (isInvalidFabric()) {
                            PL.logI("Found incompatible mod jar file (FORGE/NEO): " + issuedFileString, Markers.SEARCH.marker);
                            UNSUPPORTED_JARS.add(issuedFileString);
                        }*/
                        PL.logI("Fabric: todo");
                    }
                }
            }
        });
    }

    /**
     * Moves the incompatible jars in the incompatible jars directory.
     */
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

    /**
     * Gets a mod jar file path from the jar name.
     * <br><b>Specifically:</b> searches in the main mod directory the jar file and returns the path if exists. If it's not there (could be an incompatible file that has been moved)
     * it searches in the incompatible jars directory {@link IncompatibleModsRemover#INCOMPATIBLE_JARS_DIR}. If it's not found here, the mothed throws an {@link IOException}.
     * @param modJarFile The name of the mod jar file.
     * @return The {@link Path} of the jar file.
     * @throws FileNotFoundException If the jar file isn't found neither in the regular mod directory nor in the incompatible mods directory.
     */
    public static Path getModJarPath(String modJarFile) throws FileNotFoundException {
        List<Path> paths = List.of(MOD_DIR.resolve(modJarFile), INCOMPATIBLE_JARS_DIR.resolve(modJarFile));

        for (Path path : paths) {
            if (Files.exists(path) && Files.isRegularFile(path))
                return path;

            PL.conditionalI(path.equals(paths.getFirst()) && !Files.exists(path) , "Jar file '" + modJarFile + "' hasn't been found in regular mods directory, searching in incompatible dir.");
            PrettyLogging.conditionalThrow(!Files.isRegularFile(path), new IllegalArgumentException("The given file path is not a file!"));
        }

        throw new FileNotFoundException(
                "The given jar file with name '" + modJarFile + "' hasn't been found neither in the mod main directory 'mods' nor in the incompatible mods dir '"
                        + GAME_DIR.relativize(INCOMPATIBLE_JARS_DIR) + "'!"
        );
    }

    @SubscribeEvent
    public static void loadCompleteEvent(final FMLLoadCompleteEvent event) {
        if (!eventAlreadyFired) {
            searchIncompatibilities();
            moveIncompatibilities();
            eventAlreadyFired = true;  // Idk, fires two times ¯\_(ツ)_/¯
        }
    }
}
