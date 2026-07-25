package com.palm3.assets_loader.assets;

import com.mojang.logging.LogUtils;
import com.palm3.assets_loader.PrettyLogging;
import com.palm3.assets_loader.assets.patchers.AssetType;
import com.palm3.assets_loader.assets.patchers.AssetsFilesNamespaceChanger;
import com.palm3.assets_loader.assets.patchers.ModelsChanger;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.repository.*;
import net.minecraft.world.flag.FeatureFlagSet;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.AddPackFindersEvent;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.palm3.assets_loader.LoaderMain.*;
import static com.palm3.assets_loader.PrettyLogging.*;

//todo add custom icon

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

    // Check lists of namespaces, jar files, etc. since they need to have matching values in order to be used.
    private static void checkListSizesAndThrow(List<String> modJarFiles, List<String> jarAssetsNamespaces, @Nullable List<String> newNamespaces) throws IllegalArgumentException {
        if (newNamespaces == null) {
            if (modJarFiles.size() != jarAssetsNamespaces.size()) {
                int jarFilesSize = modJarFiles.size();
                int jarNamespacesSize = jarAssetsNamespaces.size();
                String exceptionList;

                if (jarFilesSize > jarNamespacesSize) exceptionList = "'mod_jar_files'";
                else exceptionList = "'jar_assets_namespaces'";

                throw new IllegalArgumentException("All lists must be the same size! Affected list: " + exceptionList);
            }
        } else {
            if (modJarFiles.size() != jarAssetsNamespaces.size() || modJarFiles.size() != newNamespaces.size()) {
                int jarFilesSize = modJarFiles.size();
                int jarNamespacesSize = jarAssetsNamespaces.size();
                int newNamespacesSize = newNamespaces.size();
                String exceptionList;

                if (jarFilesSize > jarNamespacesSize || jarFilesSize > newNamespacesSize) exceptionList = "'mod_jar_files'";
                else if (jarNamespacesSize > jarFilesSize) exceptionList = "'jar_assets_namespaces'";
                else exceptionList = "'new_namespaces'";

                throw new IllegalArgumentException("All lists must be the same size! Affected list: " + exceptionList);
            }
        }
    }

    private void logModInfo() {
        if (!modInfoLogged) {
            PL.logLine(false);
            PL.logCentered("External assets loader by Palm3", DEF_EMPTY_LINE, true, true);
            PL.logCentered("Loads assets in-game directly from a mod jar", DEF_EMPTY_LINE, true, true);
            PL.logCentered("Mod version: " + MOD_VERSION + "    Discord: " + DISCORD_LINK, DEF_EMPTY_LINE, true, true);
            PL.logI(PL.line1);
            PL.logI("Starting loading process -->");
            modInfoLogged = true;
        }
    }

    public final Multiple multiple;
    public final Single single;
    private boolean modInfoLogged;
    private final String tempDirectory;

    public AssetsLoader(String tempDirectory) {
        modInfoLogged = false;
        multiple = new Multiple(this);
        single = new Single(this);
        this.tempDirectory = tempDirectory;
    }

    /// Return the directory as {@link Path} where the packs are located.
    public Path getPacksDir() {
        return GAME_DIR.resolve(tempDirectory);
    }


    public static class Single {
        private AssetsLoader assetsLoader;

        protected Single(AssetsLoader assetsLoader) {
            this.assetsLoader = assetsLoader;
        }

        /**
         * Loads assets from one jar and can change the final namespace.
         * @param event The {@link AddPackFindersEvent} event. Call this method in a method annotated with {@link net.neoforged.bus.api.SubscribeEvent}
         *              and with parameter only {@code AddPackFindersEvent event}.
         * @param modJarFile The name of the mod jar file including the file extension.
         * @param jarAssetsNamespace The namespace you want to load the assets from.
         * @param newNamespace The new namespace that the assets will have.
         * @param iconFileName The name (without the file extension) of the icon file of the mod.
         * @param resourcePackFolderName The name of the folder where the resourcepack will be created (inside temp dir, contains the {@code assets} folder, the icon...).
         * @param packTitle The in-game name of the resourcepack.
         * @param packDescription The in-game description of the resourcepack.
         * @param deletePackWhenQuit If the pack should be deleted when you close the game.
         * @param resourcePackCanBeDisabled If the pack can be moved in-game.
         */
        public void loadAssetsCustomFolderName(AddPackFindersEvent event, String modJarFile, String jarAssetsNamespace, String newNamespace, String iconFileName,
                                                      String resourcePackFolderName, Component packTitle, Component packDescription, boolean deletePackWhenQuit, boolean resourcePackCanBeDisabled, boolean logCopy) {
            assetsLoader.logModInfo();

            Path jarFilePath = MOD_DIR.resolve(modJarFile);

            createDirectory(assetsLoader.tempDirectory, true);

            Path packPath = GAME_DIR.resolve(assetsLoader.tempDirectory).resolve(resourcePackFolderName);
            Path target = packPath.resolve("assets").resolve(newNamespace);

            copyAssetsFromJar_unsafe(jarFilePath, target, jarAssetsNamespace, iconFileName, null, false, logCopy);

            AssetsFilesNamespaceChanger.singleNamespace(packPath, jarAssetsNamespace, newNamespace).changeAll();

            ModelsChanger.createNew(packPath, newNamespace, ModelsChanger.Loader.FORGE).changeModelsObjLoader(AssetType.ALL_MODELS);  // todo add conditional logging

            event.addRepositorySource(packRepositorySource(
                    resourcePackFolderName,
                    packTitle,
                    packDescription,
                    packPath,
                    !resourcePackCanBeDisabled
            ));

            if (deletePackWhenQuit) {
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    deleteDirectory(assetsLoader.tempDirectory, false, true);
                }));
            }
        }

        /**
         * Loads assets from one jar keeping the same namespace.
         * @param event The {@link AddPackFindersEvent} event. Call this method in a method annotated with {@link net.neoforged.bus.api.SubscribeEvent}
         *              and with parameter only {@code AddPackFindersEvent event}.
         * @param modJarFile The name of the mod jar file including the file extension.
         * @param jarAssetsNamespace The namespace you want to load the assets from.
         * @param iconFileName The name (without the file extension) of the icon file of the mod.
         * @param resourcePackFolderName The name of the folder where the resourcepack will be created (inside temp dir, contains the {@code assets} folder, the icon...).
         * @param packTitle The in-game name of the resourcepack.
         * @param packDescription The in-game description of the resourcepack.
         * @param deletePackWhenQuit If the pack should be deleted when you close the game.
         * @param resourcePackCanBeDisabled If the pack can be moved in-game.
         */
        public void loadAssetsCustomFolderName(AddPackFindersEvent event, String modJarFile, String jarAssetsNamespace, String iconFileName,
                                      String resourcePackFolderName, Component packTitle, Component packDescription, boolean deletePackWhenQuit, boolean resourcePackCanBeDisabled, boolean logCopy) {
            assetsLoader.logModInfo();

            Path jarFilePath = MOD_DIR.resolve(modJarFile);

            createDirectory(assetsLoader.tempDirectory, true);

            Path packPath = GAME_DIR.resolve(assetsLoader.tempDirectory).resolve(resourcePackFolderName);
            Path target = packPath.resolve("assets").resolve(jarAssetsNamespace);

            copyAssetsFromJar_unsafe(jarFilePath, target, jarAssetsNamespace, iconFileName, null, false, logCopy);

            event.addRepositorySource(packRepositorySource(
                    resourcePackFolderName,
                    packTitle,
                    packDescription,
                    packPath,
                    !resourcePackCanBeDisabled
            ));

            if (deletePackWhenQuit) {
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    deleteDirectory(assetsLoader.tempDirectory, false, true);
                }));
            }
        }

        /**
         * Loads assets from one jar and can change the final namespace. The resourcepack folder name will have the given jar namespace, not the new namespace.
         * @param event The {@link AddPackFindersEvent} event. Call this method in a method annotated with {@link net.neoforged.bus.api.SubscribeEvent}
         *              and with parameter only {@code AddPackFindersEvent event}.
         * @param modJarFile The name of the mod jar file including the file extension.
         * @param jarAssetsNamespace The namespace you want to load the assets from.
         * @param newNamespace The new namespace that the assets will have.
         * @param iconFileName The name (without the file extension) of the icon file of the mod.
         * @param packTitle The in-game name of the resourcepack.
         * @param packDescription The in-game description of the resourcepack.
         * @param deletePackWhenQuit If the pack should be deleted when you close the game.
         * @param resourcePackCanBeDisabled If the pack can be moved in-game.
         */
        public void loadAssets(AddPackFindersEvent event, String modJarFile, String jarAssetsNamespace, String newNamespace, String iconFileName,
                                      Component packTitle, Component packDescription, boolean deletePackWhenQuit, boolean resourcePackCanBeDisabled, boolean logCopy) {
            assetsLoader.logModInfo();

            Path jarFilePath = MOD_DIR.resolve(modJarFile);

            createDirectory(assetsLoader.tempDirectory, true);

            Path packPath = GAME_DIR.resolve(assetsLoader.tempDirectory).resolve(jarAssetsNamespace + "_mod_extracted_assets");
            Path target = packPath.resolve("assets").resolve(newNamespace);

            copyAssetsFromJar_unsafe(jarFilePath, target, jarAssetsNamespace, iconFileName, null, false, logCopy);

            AssetsFilesNamespaceChanger.singleNamespace(packPath, jarAssetsNamespace, newNamespace).changeAll();

            event.addRepositorySource(packRepositorySource(
                    jarAssetsNamespace + "_mod_extracted_assets",
                    packTitle,
                    packDescription,
                    packPath,
                    !resourcePackCanBeDisabled
            ));

            if (deletePackWhenQuit) {
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    deleteDirectory(assetsLoader.tempDirectory, false, true);
                }));
            }
        }

        /**
         * Loads assets from one jar keeping the same namespace. The resourcepack folder name will have the given jar namespace.
         * @param event The {@link AddPackFindersEvent} event. Call this method in a method annotated with {@link net.neoforged.bus.api.SubscribeEvent}
         *              and with parameter only {@code AddPackFindersEvent event}.
         * @param modJarFile The name of the mod jar file including the file extension.
         * @param jarAssetsNamespace The namespace you want to load the assets from.
         * @param iconFileName The name (without the file extension) of the icon file of the mod.
         * @param packTitle The in-game name of the resourcepack.
         * @param packDescription The in-game description of the resourcepack.
         * @param deletePackWhenQuit If the pack should be deleted when you close the game.
         * @param resourcePackCanBeDisabled If the pack can be moved in-game.
         */
        public void loadAssets(AddPackFindersEvent event, String modJarFile, String jarAssetsNamespace, String iconFileName,
                                      Component packTitle, Component packDescription, boolean deletePackWhenQuit, boolean resourcePackCanBeDisabled, boolean logCopy) {
            assetsLoader.logModInfo();

            Path jarFilePath = MOD_DIR.resolve(modJarFile);

            createDirectory(assetsLoader.tempDirectory, true);

            Path packPath = GAME_DIR.resolve(assetsLoader.tempDirectory).resolve(jarAssetsNamespace + "_mod_extracted_assets");
            Path target = packPath.resolve("assets").resolve(jarAssetsNamespace);

            copyAssetsFromJar_unsafe(jarFilePath, target, jarAssetsNamespace, iconFileName, null, false, logCopy);

            event.addRepositorySource(packRepositorySource(
                    jarAssetsNamespace + "_mod_extracted_assets",
                    packTitle,
                    packDescription,
                    packPath,
                    !resourcePackCanBeDisabled
            ));

            if (deletePackWhenQuit) {
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    deleteDirectory(assetsLoader.tempDirectory, false, true);
                }));
            }
        }
    }


    public static class Multiple {
        private AssetsLoader assetsLoader;

        protected Multiple(AssetsLoader assetsLoader) {
            this.assetsLoader = assetsLoader;
        }

        /**
         * Loads assets from multiple jars and can change the final namespaces. It creates only one final resourcepack with multiple namespaces.
         * @param event The {@link AddPackFindersEvent} event. Call this method in a method annotated with {@link net.neoforged.bus.api.SubscribeEvent}
         *              and with parameter only {@code AddPackFindersEvent event}.
         * @param modJarFiles A {@link List} containing the names of the mod jar files including the file extensions.
         * @param jarAssetsNamespaces A {@link List} containing the namespaces you want to load the assets from for each mod (remember to follow the previous list order).
         * @param newNamespaces A {@link List} containing the new namespaces that the assets will have (remember to follow the previous list order).
         * @param iconFileName The name (without the file extension) of the icon file of the mod. NOTRE: If multiple jars have the same icon name, the icon from the last mod in the jars list will be used.
         * @param resourcePackFolderName The name of the folder where the resourcepack will be created (inside temp dir, contains the {@code assets} folder, the icon...).
         * @param packTitle The in-game name of the resourcepack.
         * @param packDescription The in-game description of the resourcepack.
         * @param deletePackWhenQuit If the pack should be deleted when you close the game.
         * @param resourcePackCanBeDisabled If the pack can be moved in-game.
         */
        public void loadAssets(AddPackFindersEvent event, List<String> modJarFiles, List<String> jarAssetsNamespaces, List<String> newNamespaces, String iconFileName,
                               String resourcePackFolderName, Component packTitle, Component packDescription, boolean deletePackWhenQuit, boolean resourcePackCanBeDisabled, boolean logCopy) {

            // Lists are different, throw.
            checkListSizesAndThrow(modJarFiles, jarAssetsNamespaces, newNamespaces);

            assetsLoader.logModInfo();

            createDirectory(assetsLoader.tempDirectory, true);

            Path packPath = GAME_DIR.resolve(assetsLoader.tempDirectory).resolve(resourcePackFolderName);

            int jarsNumber = modJarFiles.size();
            PL.logI("Mod jar files to load: " + jarsNumber);
            for (int i = 0; i < jarsNumber; i++) {
                PL.logI("Jar file: " + (i + 1) + "/" + jarsNumber, 1, LogPos.BEFORE);
                Path jarFilePath = MOD_DIR.resolve(modJarFiles.get(i));
                String jarAssetsNamespace = jarAssetsNamespaces.get(i);
                Path target = packPath.resolve("assets").resolve(newNamespaces.get(i));

                copyAssetsFromJar_unsafe(jarFilePath, target, jarAssetsNamespace, iconFileName, null, false, logCopy);
            }

            PL.logI("Copied all assets from the jar files.");

            event.addRepositorySource(packRepositorySource(
                    resourcePackFolderName,
                    packTitle,
                    packDescription,
                    packPath,
                    !resourcePackCanBeDisabled
            ));

            if (deletePackWhenQuit) {
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    deleteDirectory(assetsLoader.tempDirectory, false, true);
                }));
            }
        }

        /**
         * Loads assets from multiple jars. It creates only one final resourcepack with multiple namespaces.
         * @param event The {@link AddPackFindersEvent} event. Call this method in a method annotated with {@link net.neoforged.bus.api.SubscribeEvent}
         *              and with parameter only {@code AddPackFindersEvent event}.
         * @param modJarFiles A {@link List} containing the names of the mod jar files including the file extensions.
         * @param jarAssetsNamespaces A {@link List} containing the namespaces you want to load the assets from for each mod (remember to follow the previous list order).
         * @param iconFileName The name (without the file extension) of the icon file of the mod. NOTRE: If multiple jars have the same icon name, the icon from the last mod in the jars list will be used.
         * @param resourcePackFolderName The name of the folder where the resourcepack will be created (inside temp dir, contains the {@code assets} folder, the icon...).
         * @param packTitle The in-game name of the resourcepack.
         * @param packDescription The in-game description of the resourcepack.
         * @param deletePackWhenQuit If the pack should be deleted when you close the game.
         * @param resourcePackCanBeDisabled If the pack can be moved in-game.
         */
        public void loadAssets(AddPackFindersEvent event, List<String> modJarFiles, List<String> jarAssetsNamespaces, String iconFileName,
                               String resourcePackFolderName, Component packTitle, Component packDescription, boolean deletePackWhenQuit, boolean resourcePackCanBeDisabled, boolean logCopy) {

            // Lists are different, throw.
            checkListSizesAndThrow(modJarFiles, jarAssetsNamespaces, null);

            assetsLoader.logModInfo();

            createDirectory(assetsLoader.tempDirectory, true);

            Path packPath = GAME_DIR.resolve(assetsLoader.tempDirectory).resolve(resourcePackFolderName);

            int jarsNumber = modJarFiles.size();
            PL.logI("Mod jar files to load: " + jarsNumber);
            for (int i = 0; i < jarsNumber; i++) {
                PL.logI("Jar file: " + (i + 1) + "/" + jarsNumber, 1, LogPos.BEFORE);
                Path jarFilePath = MOD_DIR.resolve(modJarFiles.get(i));
                String jarAssetsNamespace = jarAssetsNamespaces.get(i);
                Path target = packPath.resolve("assets").resolve(jarAssetsNamespace);

                copyAssetsFromJar_unsafe(jarFilePath, target, jarAssetsNamespace, iconFileName, null, false, logCopy);
            }

            PL.logI("Copied all assets from the jar files.");

            event.addRepositorySource(packRepositorySource(
                    resourcePackFolderName,
                    packTitle,
                    packDescription,
                    packPath,
                    !resourcePackCanBeDisabled
            ));

            if (deletePackWhenQuit) {
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    deleteDirectory(assetsLoader.tempDirectory, false, true);
                }));
            }
        }
    }



    //================== STATICS =====================
    // Use those if you want, but you need to know a little what you're doing.
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

    public static void copyAssetsFromJar_unfinished(Path jarFilePath, Path destinationPath, String namespaceToCopy, String iconFileName, @Nullable Path iconDestinationPath, boolean forceCopy, boolean logCopy) {
        try (FileSystem jarFileSystem = FileSystems.newFileSystem(jarFilePath, (ClassLoader) null)) {
            PL.logI("Creating jar file system...");

            Path jarAssets = jarFileSystem.getPath("/assets/" + namespaceToCopy);
            // The location of all the assets files inside the jar file under the given namespace.

            PL.logI("Walking jar file assets and coping...");
            PL.logI("| --> Coping assets from directory: " + jarAssets + ". Jar file system root: " + jarFilePath + "/");
            PL.logI("--> | Coping assets inside directory: " + destinationPath);

            try (Stream<Path> jarAssetsStream = Files.walk(jarAssets)) {
                AtomicInteger totalFiles = new AtomicInteger(0);
                AtomicInteger totalDirectories = new AtomicInteger(0);
                AtomicInteger copiedFiles = new AtomicInteger(0);
                AtomicInteger copiedDirectories = new AtomicInteger(0);

                jarAssetsStream.forEach(path -> {

                });
            } catch (IOException e) {
                PL.logE("Exception caught during files walk: " + e);
            }

                /*if (logCopy) PL.logI(PL.line2);



                Files.walk(jarAssets).forEach(path -> {  // Walk all jar assets.

                    Path relativeJarAsset = jarAssets.relativize(path);
                    // The path of the jar asset (single file here) relative to the jar file assets.
                    // It's the path but starting only from under 'assets/namespaceToCopy/<-- from here'.

                    Path copyTarget = destinationPath.resolve(relativeJarAsset.toString());
                    // After the relativization, this is the full destination path of the file/folder.

                    if (!Files.exists(copyTarget) && !forceCopy) {
                        // If current path is a directory, create a directory, otherwise copy the file.
                        if (Files.isDirectory(path)) {
                            try {
                                if (logCopy) PL.logI("Coping directory " + relativeJarAsset);
                                Files.createDirectories(copyTarget);
                            } catch (IOException e) {
                                PL.logE("Exception caught during creation of directory " + copyTarget + ". Exception: " + e);
                            }

                            copiedDirectories.incrementAndGet();

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
                        if (Files.isDirectory(copyTarget))
                            PL.logI("Directory " + relativeJarAsset + " already exists.");
                        else
                            PL.logI("File " + relativeJarAsset + " already exists.");
                    }

                    if (Files.isDirectory(path)) totalDirectories.incrementAndGet();
                    else totalFiles.incrementAndGet();

                });*/


            /*
                PL.logI("--------------- Copy results ---------------");
                PL.logI("Total directories: " + totalDirectories.get());
                PL.logI("Total files: " + totalFiles.get());
                PL.logI("Copied directories: " + copiedDirectories.get());
                PL.logI("Copied files: " + copiedFiles.get());
                if (copiedDirectories.get() == 0 && copiedFiles.get() == 0) PL.logI("-> No copy done, already existing files.");
                PL.logI("--------------------------------------------");*/


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