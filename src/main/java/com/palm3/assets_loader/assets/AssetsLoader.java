package com.palm3.assets_loader.assets;

import com.mojang.logging.LogUtils;
import com.palm3.assets_loader.PrettyLogging;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.repository.*;
import net.minecraft.world.flag.FeatureFlagSet;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.AddPackFindersEvent;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static com.palm3.assets_loader.LoaderMain.*;
import static com.palm3.assets_loader.PrettyLogging.*;

//todo update here, add comments
/**
 * {@link AssetsLoader} is used to load Minecraft mods assets that cannot be used and published directly from your mod.
 * This class is used to extract those assets from a normally downloaded mod {@code .jar} file and use them in-game, with some other features.
 * The cool thing is that the mod version and loader are completely ignored.
 */
@ParametersAreNonnullByDefault
public class AssetsLoader {
    private static final PrettyLogging PL = new PrettyLogging(LogUtils.getLogger(), DEF_PL_PARAMS);
    private static final Path GAME_DIR = FMLPaths.GAMEDIR.get();
    private static final Path MOD_DIR = FMLPaths.MODSDIR.get();



    private void logModInfo() {
        PL.logLineI(false);
        PL.logCenteredI("External assets loader by Palm3", DEF_EMPTY_LINE, true, true);
        PL.logCenteredI("Loads assets in-game directly from a mod jar", DEF_EMPTY_LINE, true, true);
        PL.logCenteredI("Mod version: " + MOD_VERSION + "    Discord: " + DISCORD_LINK, DEF_EMPTY_LINE, true, true);
        PL.logI(PL.line1);
        PL.logI("Starting loading process -->");
    }





    //================== STATICS =====================
    private static void keepFuckingJavadocRenderedCauseItTriggersToSeeItNotRenderedBtwIfThereAreSettingsToChangeItIDontHaveTimeAndWillToFindThem() {}
    /**
     * Creates a directory inside the game folder {@code .minecraft}.
     * @param directory The directory you want to create, can also be a path.
     *                  E.g. {@code assets/temp}. Don't insert points in the path/folder name, they will get deleted.
     * @param hidden If the directory should be hidden.
     *               <br>If you're using a path, only the first (highest) directory of the path will be hidden.
     *               <br>E.g. in {@code assets/temp} only the {@code assets} directory will be hidden (and also named {@code .assets}).
     */
    public static void createDirectory(String directory, boolean hidden) {
        if (directory.contains(".")) directory = directory.replace(".", "");
        Path dir = hidden ? GAME_DIR.resolve("." + directory) : GAME_DIR.resolve(directory);

        if (!Files.exists(dir)) {
            PL.logI("Creating directory '" + directory + "' inside game folder...");

            try {
                Files.createDirectories(dir);
                PL.logI("Created directory " + dir);

                if (hidden) {
                    if (System.getProperty("os.name").toLowerCase().contains("win") && Files.getAttribute(dir, "dos:hidden") == (Boolean) false) {
                        Files.setAttribute(dir, "dos:hidden", true);
                        PL.logI("Windows detected. Applied attributes 'dos:hidden' to directory '" + GAME_DIR.relativize(dir) + "'");
                    } else if (Files.getAttribute(dir, "dos:hidden") == (Boolean) true) {
                        PL.logI("Windows detected. Attribute 'dos:hidden' of directory '" + GAME_DIR.relativize(dir) + "' already 'true'");
                    }
                }
            } catch (IOException e) {
                PL.logE("Exception caught during directory creation: " + e);
            }
        } else {
            PL.logI("Directory '" + directory + "' already present in game folder...");
        }
    }

    /**
     * Returns a directory inside the game folder {@code .minecraft} as {@link Path}.
     * @param directory The directory you want to get, can also be a path.
     *                  E.g. {@code assets/temp}. Don't insert points in the path/folder name, they will get deleted.
     * @param hidden If the directory is hidden.
     * @return The given string directory as {@link Path}.
     * @throws NoSuchFileException If the requested directory as path doesn't exist.
     */
    public static Path getDirectoryPath(String directory, boolean hidden) throws NoSuchFileException {
        if (directory.contains(".")) directory = directory.replace(".", "");
        Path dir = hidden ? GAME_DIR.resolve("." + directory) : GAME_DIR.resolve(directory);

        if (Files.exists(dir)) {
            return dir;
        } else {
            throw new NoSuchFileException("Directory '" + directory + "' you're trying to get as Path doesn't exist in game folder.");
        }
    }

    /**
     * Deletes a directory inside the game folder {@code .minecraft}.
     * @param directory The directory you want to delete, can also be a path.
     *                  E.g. {@code assets/temp}. Don't insert points in the path/folder name, they will get deleted.
     * @param deleteItself If you want to also delete the directory itself instead of the content only.
     * @param hidden If the directory is hidden.
     */
    public static void deleteDirectory(String directory, boolean deleteItself, boolean hidden) {
        if (directory.contains(".")) directory = directory.replace(".", "");
        Path dir = hidden ? GAME_DIR.resolve("." + directory) : GAME_DIR.resolve(directory);

        PL.logI("Deleting directory " + directory, 1, LogPos.BEFORE);

        if (Files.exists(dir)) {
            try (Stream<Path> pathStream = Files.walk(dir)) {
                pathStream.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        if (deleteItself) Files.deleteIfExists(path);
                        else if (!path.equals(dir)) Files.deleteIfExists(path);
                    } catch (IOException e) {
                        PL.logE("Exception caught during file/directory delete. Path: " + dir + " Exception: " + e);
                    }
                });
                if (deleteItself) PL.logI("Deleted directory '" + GAME_DIR.getParent().relativize(dir) + "' and its content.");
                else PL.logI("Deleted directory '" + GAME_DIR.getParent().relativize(dir) + "' content.");
            } catch (IOException e) {
                PL.logE("Error during directory files walk. Exception: " + e);
            }
        } else {
            PL.logI("Folder '" + GAME_DIR.getParent().relativize(dir) + "' does not exist, nothing to delete.");
        }
    }

    /**
     * Creates a {@link Pack} to be used in the {@link AddPackFindersEvent}.
     * @param internalPackId The game internal id of the pack. You will never see this, but remember to avoid creating duplicates.
     * @param packTitle The in-game title of the resourcepack.
     * @param packDescription The in-game description of the resourcepack.
     * @param packPath The path where to find the pack. Needs to be the folder containing the {@code assets} folder.
     * @param required If {@code true} the pack cannot be disabled in-game, otherwise it can.
     * @return The {@link Pack}.
     * @throws RuntimeException If the method fails creating a {@link PathPackResources}.
     */
    public static Pack createPack(String internalPackId, Component packTitle, Component packDescription, Path packPath, boolean required) throws RuntimeException {
        PackLocationInfo packLocationInfo = new PackLocationInfo(
                internalPackId,
                packTitle,
                PackSource.DEFAULT,
                Optional.empty()
        );

        try (PathPackResources pathPackResources = new PathPackResources(packLocationInfo, packPath)) {
            PackSelectionConfig selectionConfig = new PackSelectionConfig(required, Pack.Position.BOTTOM, false);
            Pack.Metadata metadata = new Pack.Metadata(packDescription, PackCompatibility.COMPATIBLE, FeatureFlagSet.of(), List.of(), false);

            return new Pack(
                    packLocationInfo,
                    BuiltInPackSource.fromName(path -> pathPackResources),
                    metadata,
                    selectionConfig
            );
        } catch (Exception e) {
            PL.logE("Exception caught during PathPackResources creation: " + e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Does the same as {@link AssetsLoader#createPack(String, Component, Component, Path, boolean)}.
     * @param internalPackId The game internal id of the pack. You will never see this, but remember to avoid creating duplicates.
     * @param packTitle The in-game title of the resourcepack.
     * @param packDescription The in-game description of the resourcepack.
     * @param packPath The path where to find the pack. Needs to be the folder containing the {@code assets} folder.
     * @param required If {@code true} the pack cannot be disabled in-game, otherwise it can.
     * @return The pack as a {@link RepositorySource} ready to be loaded.
     * @throws RuntimeException If the pack creation fails.
     */
    public static RepositorySource packRepositorySource(String internalPackId, Component packTitle, Component packDescription, Path packPath, boolean required) throws RuntimeException {
        return packConsumer -> packConsumer.accept(createPack(internalPackId, packTitle, packDescription, packPath, required));
    }

    /**
     * Copies the asset files from the jar file to the destination folder to be used as resourcepack.
     * <br><b>NOTE:</b> it doesn't copy the icon file, use {@link AssetsLoader#copyJarIcon(Path, String, Path, String)} for that.
     * @param jarFilePath The location of the jar file. E.g. {@code .minecraft/mods/my_mod_file-1.0.jar}
     * @param filesDestinationPath The destination pack where the files (blockstates, models, textures...) should go in.
     * @param namespaceToCopy The assets namespace you want to copy from the jar.
     * @param forceCopy If the file copy should be forced (replacing the old files), even if the files are already there.
     * @param logCopyOption Decide if you want to log always, when you replace files or never.
     * @see LogCopyOption
     */
    // Holy nested method, warning provided.
    public static void copyAssetsFromJar(Path jarFilePath, Path filesDestinationPath, String namespaceToCopy, boolean forceCopy, LogCopyOption logCopyOption) {
        try (FileSystem jarFileSystem = FileSystems.newFileSystem(jarFilePath, (ClassLoader) null)) {
            PL.logI("Creating jar file system...");

            Path jarAssets = jarFileSystem.getPath("assets", namespaceToCopy);
            // The location of all the assets files inside the jar file under the given namespace.

            PL.logI("Walking jar file assets and coping...");
            PL.logI("| --> Coping assets from directory: " + jarAssets + ". Jar file system root: " + jarFilePath + File.separator);
            PL.logI("--> | Coping assets inside directory: " + filesDestinationPath);

            try (Stream<Path> jarAssetsStream = Files.walk(jarAssets)) {
                // All used for logging
                AtomicInteger totalPaths = new AtomicInteger();
                AtomicInteger totalFiles = new AtomicInteger();
                AtomicInteger totalDirectories = new AtomicInteger();
                AtomicInteger copiedFiles = new AtomicInteger();
                AtomicInteger copiedDirectories = new AtomicInteger();
                AtomicBoolean allAlreadyExist = new AtomicBoolean(true);
                AtomicInteger replacedFiles = new AtomicInteger();

                jarAssetsStream.forEach(path -> {
                    Path relativeJarAsset = jarAssets.relativize(path);
                    // The path of the jar asset (single file here) relative to the jar file assets.
                    // It's the path but starting only from under 'assets/namespaceToCopy/<-- from here'.

                    Path copyTarget = filesDestinationPath.resolve(relativeJarAsset.toString());
                    // After the relativization, this is the full destination path of the file/folder.

                    // Logging purpose
                    totalPaths.incrementAndGet();
                    if (Files.isRegularFile(path)) totalFiles.incrementAndGet();
                    if (Files.isDirectory(path)) totalDirectories.incrementAndGet();
                    if (allAlreadyExist.get() && !Files.exists(copyTarget))
                        allAlreadyExist.set(false);

                    if ((Files.exists(copyTarget) && forceCopy) || !Files.exists(copyTarget)) {
                        if (Files.isRegularFile(path)) {
                            try (InputStream is = Files.newInputStream(path)) {
                                PL.conditionalI(logCopyOption.canLog(copyTarget), "Coping file '" + relativeJarAsset + "'");
                                Files.copy(is, copyTarget, StandardCopyOption.REPLACE_EXISTING);
                                if (!Files.exists(copyTarget)) copiedFiles.incrementAndGet();
                                else replacedFiles.incrementAndGet();
                            } catch (IOException e) {
                                PL.logE("Exception caught during copy of file " + path + " inside destination directory " + GAME_DIR.relativize(filesDestinationPath) + ". Exception: " + e);
                            }

                        } else if (Files.isDirectory(path)) {
                            try {
                                PL.conditionalI(logCopyOption.canLog(copyTarget), "Coping directory '" + relativeJarAsset + "'");
                                Files.createDirectories(copyTarget);
                                copiedDirectories.incrementAndGet();
                            } catch (IOException e) {
                                PL.logE("Exception caught during creation of directory " + copyTarget + ". Exception: " + e);
                            }

                        } else {
                            PL.logW("Path " + path + " is neither a file nor a directory.");
                        }
                    }
                });

                PL.logCenteredI("Copy results", PL.line2, true, true);
                PL.logI("Total elements: " + totalPaths.get());
                PL.logI("Total directories: " + totalDirectories.get());
                PL.logI("Total files: " + totalFiles.get());
                PL.logI("Copied directories: " + copiedDirectories.get());
                PL.logI("Copied files: " + copiedFiles.get());
                PL.logI("Replaced files: " + replacedFiles.get());
                PL.conditionalI(allAlreadyExist.get(), "-> No copy done, already existing files.");
                PL.logI(PL.line2);

            } catch (IOException e) {
                PL.logE("Exception caught during files walk: " + e);
            }
        } catch (IOException e) {
            PL.logE("Exception caught during jar file system creation: " + e);
        }
    }

    /**
     * Copies the icon file from a mod jar.
     * <br><b>NOTE:</b> should be used to copy an icon from a mod jar file only.
     * @param jarFilePath The path of the mod jar.
     * @param iconFileName The name of the icon file you want to copy.
     * @param iconDestinationFolderPath The destination folder where the icon will be copied into.
     * @param newIconFileName The new icon file name. If {@code null} it will remain the old one given in 'iconFileName'.
     */
    public static void copyJarIcon(Path jarFilePath, String iconFileName, Path iconDestinationFolderPath, @Nullable String newIconFileName) {
        try (FileSystem jarFileSystem = FileSystems.newFileSystem(jarFilePath)) {
            String iconFile = iconFileName;
            if (!iconFileName.endsWith(".png")) iconFile += ".png";

            Path jarIconPath = jarFileSystem.getPath(iconFile);

            if (Files.exists(jarIconPath) && Files.isRegularFile(jarIconPath)) {
                try (InputStream is = Files.newInputStream(jarIconPath)) {
                    PL.logI("Coping icon file '" + jarIconPath + "' from jar file");

                    String newIconFile = newIconFileName != null ? newIconFileName : iconFile;
                    if (newIconFileName != null && !newIconFileName.endsWith(".png")) newIconFile += ".png";

                    Files.copy(is, iconDestinationFolderPath.resolve(newIconFile), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    PL.logE("Exception caught during copy of icon file " + jarIconPath + ". Exception: " + e);
                }
            } else {
                PL.logW("No icon file found in jar file. Searched path: " + jarIconPath);
            }
        } catch (IOException e) {
            PL.logE("Exception caught during jar file system creation: " + e);
        }






    }

    /**
     * Copies an icon file from the given path.
     * <br><b>NOTE:</b> the method replaces a possibly already existing icon in the destination.
     * @param iconPath The path of the icon file.
     * @param iconDestinationFolderPath The destination folder where the icon will be copied into.
     * @param newIconFileName The new icon file name. If {@code null} it will remain the old one (last part of the path, the file).
     * @param morelDebug If more debug infos on the icon file you're trying to copy should be printed.
     *                   <br>It's here for testing purposes only, should always be set false when you're sure the copy works.
     */
    public static void copyPngIcon(Path iconPath, Path iconDestinationFolderPath, @Nullable String newIconFileName, boolean morelDebug) {
        if (morelDebug) {
            PL.logI("Debug infos about the icon file you're trying to copy:");
            PL.logI("Given icon path: " + iconPath);
            PL.logI("Readable: " + Files.isReadable(iconPath));
            PL.logI("Exists: " + Files.exists(iconPath));
            PL.logI("Is regular file: " + Files.isRegularFile(iconPath));
            PL.logI("Path ends with '.png': " + iconPath.getFileName().toString().endsWith(".png"));
        }

        if (Files.exists(iconPath) && Files.isRegularFile(iconPath) && iconPath.getFileName().toString().endsWith(".png")) {
            try (InputStream is = Files.newInputStream(iconPath)) {
                PL.logI("Coping PNG icon file '" + iconPath + "'");

                String newIconFile = newIconFileName != null ? newIconFileName : iconPath.getFileName().toString();
                if (newIconFileName != null && !newIconFileName.endsWith(".png")) newIconFile += ".png";

                Files.copy(is, iconDestinationFolderPath.resolve(newIconFile), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                PL.logE("Exception caught during copy of PNG icon file " + iconPath + ". Exception: " + e);
            }
        } else {
            PL.logW("No PNG icon file found. Searched path: " + iconPath);
        }
    }

    public enum LogCopyOption {
        ALWAYS_LOG,
        LOG_NONEXISTENT,
        NEVER_LOG;

        LogCopyOption() {}

        private boolean canLog(Path copyTarget) {
            if (this == ALWAYS_LOG) return true;
            return this == LOG_NONEXISTENT && !Files.exists(copyTarget);

        }
    }

    /**
     * Loads one mod jar assets to a resourcepack. Creates the temporary directory if it doesn't exist.
     * @param event The add pack finders event.
     * @param tempDirectory The temporary directory that will contain the resourcepack.
     * @param hidden If the temporary directory is hidden.
     * @param modJarFile The mod jar file name, including the {@code .jar} extension.
     * @param jarAssetsNamespace The namespace you want to copy.
     * @param newNamespace The new namespace, set to null to keep the old one.
     * @param iconFileName The name of the jar icon file.
     * @param packInfos An instance of {@link ResourcePackInfos}.
     * @param forceCopy If the copy of the fle should be forced, replacing the existent ones.
     * @param logCopyOption Defines the type of copy logs.
     * @throws NoSuchFileException If the temp directory doesn't exist (should never happen).
     */
    public static void loadPackFromJar(AddPackFindersEvent event, String tempDirectory, boolean hidden, String modJarFile, String jarAssetsNamespace, @Nullable String newNamespace,
                                       String iconFileName, ResourcePackInfos packInfos, boolean forceCopy, LogCopyOption logCopyOption) throws NoSuchFileException {
        modJarFile = modJarFile.endsWith(".jar") ? modJarFile : modJarFile + ".jar";
        createDirectory(tempDirectory, hidden);
        try {
            Path jarFilePath = MOD_DIR.resolve(modJarFile);
            Path packPath = getDirectoryPath(tempDirectory, hidden).resolve(packInfos.packFolderName());
            Path assetsDestinationPath = packPath.resolve("assets").resolve(newNamespace == null ? jarAssetsNamespace : newNamespace);

            PL.logI("Starting assets loading process ->");
            copyAssetsFromJar(jarFilePath, assetsDestinationPath, jarAssetsNamespace, forceCopy, logCopyOption);
            copyJarIcon(jarFilePath, iconFileName, packPath, "pack");

            event.addRepositorySource(packRepositorySource(packInfos.packFolderName(), packInfos.packTitle(), packInfos.packDescription(), packPath, packInfos.requiredPack()));

            if (packInfos.deletePack()) {
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    deleteDirectory(tempDirectory + File.separator + packInfos.packFolderName(), true, hidden);
                }));
            }
        } catch (IOException e) {
            throw new NoSuchFileException("The directory '" + getDirectoryPath(tempDirectory, hidden) + "' doesn't exist in the game folder.");
        }
    }





    /**
     * @deprecated - Unsafe
     * <br>
     * Copies the files from the jar to the destination folder to be used as resourcepack.
     * @param jarFilePath The location of the jar file. E.g. {@code .minecraft/mods/my_mod_file-1.0.jar}
     * @param destinationPath The destination path where the files should go in (for 'files' it's intended all the files inside the {@code assets/namespace/*} folder).
     * @param namespaceToCopy The assets namespace you want to copy from the jar.
     * @param iconFileName The name of the icon file inside the root of the jar file (without the file extension).
     * @param iconDestinationPath The destination folder where the icon should go inside.
     *                            Set to null if you set {@code .minecraft/temporary_dir/resourcepack_name/assets/namespace} as {@code destinationPath}
     *                            to automatically copy the icon inside the resourcepack root.
     * @param forceCopy If the file copy should be forced, even if the files are already there.
     * @param logCopy If the copied files should be PL.logged (suggested disabling it for large mods with lots of assets).
     */
    @Deprecated(forRemoval = true)
    public static void copyAssetsFromJar_unsafe(Path jarFilePath, Path destinationPath, String namespaceToCopy, String iconFileName, @Nullable Path iconDestinationPath, boolean forceCopy, boolean logCopy) {
        try (FileSystem jarFileSystem = FileSystems.newFileSystem(jarFilePath, (ClassLoader) null)) {
            PL.logI("Creating jar file system...");

            Path jarAssets = jarFileSystem.getPath("/assets/" + namespaceToCopy);
            // The location of all the assets files inside the jar file under the given namespace.

            try {
                PL.logI("Walking jar file assets and coping...");
                PL.logI("| --> Coping assets from directory: " + jarAssets + ". Jar file system root: " + jarFilePath + "/");
                PL.logI("--> | Coping assets inside directory: " + destinationPath);
                if (logCopy) PL.logI(PL.line2);

                AtomicInteger totalFiles = new AtomicInteger(0);
                AtomicInteger totalDirectories = new AtomicInteger(0);
                AtomicInteger copiedFiles = new AtomicInteger(0);
                AtomicInteger copiedDirectories = new AtomicInteger(0);

                Files.walk(jarAssets).forEach(path -> {  // Walk all jar assets.

                    Path relativeJarAsset = jarAssets.relativize(path);
                    // The path of the jar asset (single file here) relative to the jar file assets.
                    // It's the path but starting only from under 'assets/namespaceToCopy/<-- from here'.

                    Path copyTarget = destinationPath.resolve(relativeJarAsset.toString());
                    // After the relativization, this is the full destination path of the file/folder.

                    if (!Files.exists(copyTarget) && !forceCopy) {
                        // If current path is a directory, create a directory, otherwise copy the file.

                        //DIR
                        if (Files.isDirectory(path)) {
                            try {
                                if (logCopy) PL.logI("Coping directory " + relativeJarAsset);
                                Files.createDirectories(copyTarget);
                            } catch (IOException e) {
                                PL.logE("Exception caught during creation of directory " + copyTarget + ". Exception: " + e);
                            }

                            copiedDirectories.incrementAndGet();

                            //FILE
                        } else {
                            try (InputStream is = Files.newInputStream(path)) {
                                if (logCopy) PL.logI("Coping file " + relativeJarAsset);
                                Files.copy(is, copyTarget, StandardCopyOption.REPLACE_EXISTING);
                            } catch (IOException e) {
                                PL.logE("Exception caught during copy of file " + path + " inside destination directory " + GAME_DIR.relativize(destinationPath) + ". Exception: " + e);
                            }

                            copiedFiles.incrementAndGet();

                        }

                    } else if (logCopy) {
                        //DIR
                        if (Files.isDirectory(copyTarget))
                            PL.logI("Directory " + relativeJarAsset + " already exists.");
                            //FILE
                        else
                            PL.logI("File " + relativeJarAsset + " already exists.");
                    }

                    if (Files.isDirectory(path)) totalDirectories.incrementAndGet();
                    else totalFiles.incrementAndGet();

                });

                PL.logI("--------------- Copy results ---------------");
                PL.logI("Total directories: " + totalDirectories.get());
                PL.logI("Total files: " + totalFiles.get());
                PL.logI("Copied directories: " + copiedDirectories.get());
                PL.logI("Copied files: " + copiedFiles.get());
                if (copiedDirectories.get() == 0 && copiedFiles.get() == 0) PL.logI("-> No copy done, already existing files.");
                PL.logI("--------------------------------------------");

            } catch (IOException e) {
                PL.logE("Exception caught during files walk: " + e);
            }

            // Icon file
            Path iconPath = jarFileSystem.getPath("/" + iconFileName + ".png");
            if (Files.exists(iconPath)) {
                try (InputStream is = Files.newInputStream(iconPath)) {
                    PL.logI("Coping icon file " + iconPath + " from jar file");
                    Files.copy(is,
                            iconDestinationPath == null ? destinationPath.getParent().getParent().resolve("pack.png")
                                    : iconDestinationPath.resolve("pack.png"),
                            StandardCopyOption.REPLACE_EXISTING
                    );
                } catch (IOException e) {
                    PL.logE("Exception caught during copy of icon file " + iconPath + ".png. Exception: {" + e);
                }
            } else {
                PL.logW("No icon file found in jar file. Searched: " + iconPath);
            }

        } catch (IOException e) {
            PL.logE("Exception caught during jar file system creation: " + e);
        }
    }

}