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
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import static com.palm3.packs_loader.PacksLoaderMain.*;

/**
 * This class is not meant to be used by other mods, this mod is the only one moving the incompatible jars for the other mods.
 * The class is used to move the incompatible mods found in the {@code mods} directory, that are needed to use the assets or data, but that
 * loaders mark as incompatible (while still allowing to play the game as NeoForge, for example, does).
 * <br>
 * <br>For example, with NeoForge:
 * with this class, the user needs to click {@code Proceed to main menu} only the first time they load their game,
 * then all the other times the loader won't "crash" since the first time the game loaded all the incompatible jars have been moved in the {@link #INCOMPATIBLE_JARS_DIR}.
 * <br>This is the main downside of this mod, the user needing to accept that message one time.
 */
@EventBusSubscriber
public class IncompatibleModsRemover {
    private static final PrettyLogging PL = new PrettyLogging(LogUtils.getLogger(), DEF_PL_PARAMS);
    public static final Path INCOMPATIBLE_JARS_DIR = MOD_DIR.resolve("incompatible_loader_jars");
    public static final List<String> UNSUPPORTED_JARS = new ArrayList<>();
    private static boolean eventAlreadyFired = false;
    public static int newIncompatibleJars = 0;

    static {
        try {
            PL.logI("Creating directory for incompatible jars in main mods folder.", Markers.INIT.marker);
            Files.createDirectories(INCOMPATIBLE_JARS_DIR);
        } catch (IOException e) {
            throw new RuntimeException("IOException caught during '" + INCOMPATIBLE_JARS_DIR.getFileName() + "' directory creation: " + e);
        }
    }





    // WORKING ON
    //--------- FROM HERE --------

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
                            newIncompatibleJars++;
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
    //-------- TO HERE ----------





    /**
     * Moves the incompatible jars in the incompatible jars directory.
     */
    protected static void moveIncompatibilities() {
        UNSUPPORTED_JARS.forEach(jarFile -> {
            try {
                PL.logI("Moving incompatible loader jar: '" + jarFile + "'", Markers.MOVE.marker);
                Files.move(MOD_DIR.resolve(jarFile), INCOMPATIBLE_JARS_DIR.resolve(jarFile), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                PL.logE("Exception caught during incompatible file transfer in 'incompatible_loader_jars' directory: " + e, Markers.MOVE.marker);
            }
        });
    }

    /**
     * Gets a mod jar file path from the jar name.
     * <br><b>Specifically:</b> searches in the main mods directory the jar file and returns the path if exists. If it's not there (could be an incompatible file that has been moved)
     * it searches in the incompatible jars directory {@link IncompatibleModsRemover#INCOMPATIBLE_JARS_DIR}. If it's not found here, the mothed throws an {@link IOException}.
     * @param modJarFile The name of the mod jar file.
     * @return The <b>absolute</b> {@link Path} of the jar file.
     * @throws FileNotFoundException If the jar file isn't found neither in the regular mods directory nor in the incompatible mods directory.
     */
    public static Path getModJarPath(String modJarFile) throws FileNotFoundException {
        List<Path> paths = List.of(MOD_DIR.resolve(modJarFile), INCOMPATIBLE_JARS_DIR.resolve(modJarFile));

        for (Path path : paths) {
            if (Files.exists(path) && Files.isRegularFile(path))
                return path;

            PL.conditionalI(path.equals(paths.getFirst()) && !Files.exists(path) , "Jar file '" + modJarFile + "' hasn't been found in regular mods directory, searching in incompatible dir.");
            PrettyLogging.conditionalThrow(Files.exists(path) && !Files.isRegularFile(path), new IllegalArgumentException("The file path '" + path + "' is not a file!"));
        }

        throw new FileNotFoundException(
                "The given jar file with name '" + modJarFile + "' hasn't been found neither in the mod main directory 'mods' nor in the incompatible mods dir '"
                        + GAME_DIR.relativize(INCOMPATIBLE_JARS_DIR) + "'!"
        );
    }

    /**
     * Gets a mod jar file path from the jar name.
     * <br><b>Specifically:</b> searches in the main mods directory the jar file and returns the path if exists. If it's not there (could be an incompatible file that has been moved)
     * it searches in the incompatible jars directory {@link IncompatibleModsRemover#INCOMPATIBLE_JARS_DIR}. If it's not found here, the mothed throws an {@link IOException}.
     * @param modJarFilePath The current path of the mod jar file, will take the name with {@link Path#getFileName()}.
     * @return The <b>absolute</b> {@link Path} of the jar file.
     * @throws FileNotFoundException If the jar file isn't found neither in the regular mod directory nor in the incompatible mods directory.
     */
    public static Path getModJarPath(Path modJarFilePath) throws FileNotFoundException {
        return getModJarPath(modJarFilePath.getFileName().toString());
    }

    /**
     * Does the same as {@link #getModJarPath(String)}, but handles the exception.
     * @param modJarFile The name of the mod jar file.
     * @return The <b>absolute</b> {@link Path} of the jar file.
     * @throws IllegalArgumentException If the jar file isn't found neither in the regular mod directory nor in the incompatible mods directory.
     */
    public static Path handledGetModJarPath(String modJarFile) {
        try {
            return getModJarPath(modJarFile);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("The mod jar file '" + modJarFile + "' hasn't been found. Searched in main 'mods' directory and in '" + GAME_DIR.relativize(INCOMPATIBLE_JARS_DIR) + " directory.");
        }
    }

    /**
     * Does the same as {@link #getModJarPath(Path)}, but handles the exception.
     * @param modJarFilePath The path of the mod jar file.
     * @return The <b>absolute</b> {@link Path} of the jar file.
     * @throws IllegalArgumentException If the jar file isn't found neither in the regular mod directory nor in the incompatible mods directory.
     */
    public static Path handledGetModJarPath(Path modJarFilePath) {
        return handledGetModJarPath(modJarFilePath.getFileName().toString());
    }

    @SubscribeEvent
    public static void loadCompleteEvent(final FMLLoadCompleteEvent event) {
        if (!eventAlreadyFired) {
            searchIncompatibilities();
            moveIncompatibilities();
            eventAlreadyFired = true;  // Idk, fires two times
        }
    }
}
