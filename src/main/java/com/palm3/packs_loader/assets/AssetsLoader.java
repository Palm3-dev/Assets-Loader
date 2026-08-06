package com.palm3.packs_loader.assets;

import com.mojang.logging.LogUtils;
import com.palm3.packs_loader.common.CommonMethods;
import com.palm3.packs_loader.common.FilesCopier;
import com.palm3.packs_loader.logging.Markers;
import com.palm3.packs_loader.logging.PrettyLogging;
import com.palm3.packs_loader.assets.patchers.AssetsFilesNamespaceChanger;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.repository.*;
import net.minecraft.world.flag.FeatureFlagSet;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import org.slf4j.Marker;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static com.palm3.packs_loader.PacksLoaderMain.*;
import static com.palm3.packs_loader.logging.PrettyLogging.*;

//todo finish the javadoc
/**
 * {@link AssetsLoader} is used to load Minecraft mods assets that cannot be used and published directly from your mod.
 * This class is used to extract those assets from a normally downloaded mod {@code .jar} file and use them in-game, with some other features.
 * The cool thing is that the mod version and loader are completely ignored.
 * <br><b>NOTE:</b> the loader could tell the user that an old or unsupported loader mod is unable to load, but you can still play the game just fine.
 * To remove the message the next times, use ---add here-----.
 */
@ParametersAreNonnullByDefault
public class AssetsLoader {
    private static final PrettyLogging PL = new PrettyLogging(LogUtils.getLogger(), DEF_PL_PARAMS);
    private static final CommonMethods.Logged CM = new CommonMethods.Logged(PL);
    private static final FilesCopier COPIER = new FilesCopier(PL);

    private final String tempDirectory;
    private final Path tempDirectoryPath;
    private final boolean tempDirIsHidden = true;
    private final String loaderModName;  // Mod name?

    /**
     * Used to create an instance of {@link AssetsLoader}. The class should be something like this:
     * <pre>
     *     {@code
     *      // If the directory starts with a point it will be hidden.
     *      public static final AssetsLoader LOADER =  AssetsLoader.newAssetLoader(Main.MOD_ID, ".temp_assets");
     *     }
     * </pre>
     * <br><b>IMPORTANT:</b> only one instance of this class should be created for your mod, unless you have specific requirements, obviously.
     * @param mod_id The mod id of <b>your</b> mod.
     * @param tempDirectory The temporary directory where all the loading processes and loading files will happen.
     */
    public AssetsLoader(String mod_id, String tempDirectory) {
        this.tempDirectory = tempDirectory;
        this.loaderModName = mod_id;
        tempDirectoryPath = Path.of("diocane-faah");
        //this.tempDirectoryPath = COPIER.createDirectoryAndGetPath(this.tempDirectory);
    }


    //================= STATIC LOADERS - FULL =================
    /**
     * Loads a pack in-game.
     * <p>
     *     <h3>How to use the method:</h3>
     *     <pre>
     *         {@code
     *          // Inside your main mod class (annotated with @Mod(MyMainClass.MOD_ID))
     *
     *          // You should have the global static AssetsLoader instance.
     *          public static final String MOD_ID = "my_mod_id";
     *          public static final AssetsLoader LOADER =  AssetsLoader.newAssetLoader(".temp_assets", true, Main.MOD_ID);
     *
     *          public MyMainClass(IEventBus modEventBus) {
     *              modEventBus.addListener(MyMainClass::addPackFinders);  // Add the subscribed method here.
     *          }
     *
     *          @SubscribeEvent
     *          public static void addPackFinders(AddPackFindersEvent event) {
     *              // Here you need these record instances:
     *              JarLoadingInfos jarLoadingInfos = new JarLoadingInfos(...);
     *              ResourcePackInfos packInfos = new ResourcePackInfos(...);
     *
     *              // Having these classes, now create a PackLoadingContext and pass the previous classes to the constructor.
     *              PackLoadingContext ctx = new PackLoadingContext(LOADER, jarLoadingInfos, packInfos);
     *
     *              // Finally, load your pack.
     *              AssetsLoader.loadPack(event, ctx);
     *          }
     *         }
     *     </pre>
     * </p>
     * @param event The {@link AddPackFindersEvent}.
     * @param context An instance of {@link ResourcePackLoadingContext}.
     * @see JarLoadingInfos
     * @see ResourcePackInfos
     * @see ResourcePackLoadingContext
     * @see net.neoforged.fml.common.Mod
     * @see net.neoforged.bus.api.SubscribeEvent
     * @see AddPackFindersEvent
     */
    public static void loadPack(AddPackFindersEvent event, ResourcePackLoadingContext context) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES)
            return;
        AssetsLoader assetsLoader = context.assetsLoader();
        JarLoadingInfos jarLoadingInfos = context.jarLoadingInfos();
        ResourcePackInfos packInfos = context.packInfos();
        Path packPath = assetsLoader.tempDirectoryPath.resolve(packInfos.packFolderName());

        PL.logCenteredI("Loading pack '" + packInfos.packFolderName() + "' for mod '" + assetsLoader.loaderModName + "'", DEF_LINE);

        PrettyLogging.StepProcessLogger jarsSPL = PL.new StepProcessLogger("Loading assets from mod jar file:", jarLoadingInfos.namespaceCouplesByJarFile().size(), Markers.EXTRACT.marker);
        jarLoadingInfos.namespaceCouplesByJarFile().forEach((modJarFile, namespaceCouples) -> {
            jarsSPL.incrementAndLog();
            PrettyLogging.StepProcessLogger coupleSPL = PL.new StepProcessLogger("Mod jar file: '" + modJarFile + "', namespace couple:", namespaceCouples.size(), Markers.EXTRACT.marker);

            Path jarFilePath = MOD_DIR.resolve(modJarFile);
            for (NamespaceCouple namespaceCouple : namespaceCouples) {
                coupleSPL.incrementAndLog("{" + namespaceCouple.toString() + "}");

                Path assetsDestinationPath = packPath.resolve("assets").resolve(namespaceCouple.newOrSameNamespace());
                copyAssetsFromJar(jarFilePath, assetsDestinationPath, namespaceCouple.oldNamespace(), jarLoadingInfos.forceCopyAssets(), jarLoadingInfos.logCopyOption(), Markers.EXTRACT.marker);
                if (jarLoadingInfos.jarIconFile().iconJarFile().equals(modJarFile))
                    copyJarIcon(jarFilePath, jarLoadingInfos.jarIconFile().iconFileName(), packPath, "pack", Markers.EXTRACT.marker);
            }
        });

        PL.logI("Adding resourcepack with internal id '" + packInfos.packFolderName() + "' to game packs.", Markers.LOAD.marker);
        event.addRepositorySource(packRepositorySource(packInfos.packFolderName(), packInfos.packTitle(), packInfos.packDescription(), packPath, packInfos.requiredPack()));

        if (packInfos.deletePack()) {
            PL.logI("Pack will be deleted when game is closed.", Markers.LOAD.marker);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> CM.deleteDirectory(assetsLoader.tempDirectory, false, assetsLoader.tempDirIsHidden)));
        }

        PL.logCenteredI("Pack '" + packInfos.packFolderName() + "' loading complete!", DEF_LINE);
    }

    /**
     * Loads one mod jar assets to a resourcepack. Creates the temporary directory if it doesn't exist.
     * @param event              The add pack finders event.
     * @param tempDirectory      The temporary directory that will contain the resourcepack.
     * @param hidden             If the temporary directory is hidden.
     * @param modJarFile         The mod jar file name, including the {@code .jar} extension.
     * @param jarAssetsNamespace The namespace you want to copy.
     * @param newNamespace       The new namespace, set to null to keep the old one.
     * @param iconFileName       The name of the jar icon file.
     * @param packInfos          An instance of {@link ResourcePackInfos}.
     * @param forceCopy          If the copy of the fle should be forced, replacing the existent ones.
     * @param logCopyOption      Defines the type of copy logs.
     * @deprecated This method is exposed if you want to use it for specific cases, but its usage is not recommended.
     * Use the other loaders instead, or create your own custom method with the provided methods in {@link AssetsLoader}.
     */
    @Deprecated
    public static void loadPackFromJar(AddPackFindersEvent event, String tempDirectory, boolean hidden, String modJarFile, String jarAssetsNamespace, @Nullable String newNamespace,
                                       String iconFileName, ResourcePackInfos packInfos, boolean forceCopy, LogCopyOption logCopyOption) {
        modJarFile = modJarFile.endsWith(".jar") ? modJarFile : modJarFile + ".jar";

        Path jarFilePath = MOD_DIR.resolve(modJarFile);
        Path packPath = CM.createDirectoryAndGetPath(tempDirectory, hidden).resolve(packInfos.packFolderName());
        Path assetsDestinationPath = packPath.resolve("assets").resolve(newNamespace == null ? jarAssetsNamespace : newNamespace);

        PL.logI("Starting assets loading process ->");
        copyAssetsFromJar(jarFilePath, assetsDestinationPath, jarAssetsNamespace, forceCopy, logCopyOption);
        copyJarIcon(jarFilePath, iconFileName, packPath, "pack");

        if (newNamespace != null && !newNamespace.equals(jarAssetsNamespace)) {
            AssetsFilesNamespaceChanger.singleNamespace(packPath, jarAssetsNamespace, newNamespace).changeAll();
        }

        event.addRepositorySource(packRepositorySource(packInfos.packFolderName(), packInfos.packTitle(), packInfos.packDescription(), packPath, packInfos.requiredPack()));

        if (packInfos.deletePack()) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                CM.deleteDirectory(tempDirectory + File.separator + packInfos.packFolderName(), true, hidden);
            }));
        }
    }




    //================== STATICS - LOADING PROCESS PARTS =====================
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
    @Deprecated(forRemoval = true)
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
    @Deprecated(forRemoval = true)
    public static RepositorySource packRepositorySource(String internalPackId, Component packTitle, Component packDescription, Path packPath, boolean required) throws RuntimeException {
        return packConsumer -> packConsumer.accept(createPack(internalPackId, packTitle, packDescription, packPath, required));
    }

    /**
     * Copies the asset files from the jar file to the destination folder to be used as resourcepack.
     * <br><b>NOTE:</b> it doesn't copy the icon file, use {@link AssetsLoader#copyJarIcon(Path, String, Path, String, Marker...)} for that.
     * @param jarFilePath The location of the jar file. E.g. {@code .minecraft/mods/my_mod_file-1.0.jar}
     * @param filesDestinationPath The destination path where the files (blockstates, models, textures...) will be copied into.
     * @param namespaceToCopy The assets namespace you want to copy from the jar.
     * @param forceCopy If the file copy should be forced (replacing the old destination files), even if the files are already there.
     * @param logCopyOption Decide if you want to log always, when you replace files or never.
     * @param marker Optional logger marker. If you put more than one only the first one will be used.
     * @see LogCopyOption
     */
    // Holy nested method, warning provided.
    @Deprecated(forRemoval = true)
    public static void copyAssetsFromJar(Path jarFilePath, Path filesDestinationPath, String namespaceToCopy, boolean forceCopy, LogCopyOption logCopyOption, Marker... marker) {
        try (FileSystem jarFileSystem = FileSystems.newFileSystem(jarFilePath, (ClassLoader) null)) {
            PL.logI("Creating jar file system...", marker);

            Path jarAssets = jarFileSystem.getPath("assets", namespaceToCopy);
            // The location of all the assets files inside the jar file under the given namespace.

            PL.logI("Walking jar file assets and coping...", marker);
            PL.logI("| --> Coping assets from directory: '" + jarAssets + "'. Jar file system root: '" + jarFilePath + File.separator + "'", marker);
            PL.logI("--> | Coping assets inside directory: '" + filesDestinationPath + "'", marker);

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
                                if (!Files.exists(copyTarget)) copiedFiles.incrementAndGet();
                                else replacedFiles.incrementAndGet();
                                PL.conditionalI(logCopyOption.canLog(copyTarget), "Coping file '" + relativeJarAsset + "'", marker);
                                Files.copy(is, copyTarget, StandardCopyOption.REPLACE_EXISTING);
                            } catch (IOException e) {
                                PL.logE("Exception caught during copy of file " + path + " inside destination directory " + GAME_DIR.relativize(filesDestinationPath) + ". Exception: " + e, marker);
                            }

                        } else if (Files.isDirectory(path)) {
                            try {
                                PL.conditionalI(logCopyOption.canLog(copyTarget), "Coping directory '" + relativeJarAsset + "'", marker);
                                Files.createDirectories(copyTarget);
                                copiedDirectories.incrementAndGet();
                            } catch (IOException e) {
                                PL.logE("Exception caught during creation of directory " + copyTarget + ". Exception: " + e, marker);
                            }

                        } else {
                            PL.logW("Path " + path + " is neither a file nor a directory.", marker);
                        }
                    }
                });

                PL.logCenteredI("Copy results", PL.line2, true, true, marker);
                PL.logI("Total elements: " + totalPaths.get(), marker);
                PL.logI("Total directories: " + totalDirectories.get(), marker);
                PL.logI("Total files: " + totalFiles.get(), marker);
                PL.logI("Copied directories: " + copiedDirectories.get(), marker);
                PL.logI("Copied files: " + copiedFiles.get(), marker);
                PL.logI("Replaced files: " + replacedFiles.get(), marker);
                PL.conditionalI(allAlreadyExist.get() && !forceCopy, "-> No copy done, already existing files.", marker);
                PL.logI(PL.line2, marker);

            } catch (IOException e) {
                PL.logE("Exception caught during files walk: " + e, marker);
            }
        } catch (IOException e) {
            PL.logE("Exception caught during jar file system creation: " + e, marker);
        }
    }

    /**
     * Copies the icon file from a mod jar.
     * <br><b>NOTE:</b> should be used to copy an icon from a mod jar file only.
     * @param jarFilePath The path of the mod jar.
     * @param iconFileName The name of the icon file you want to copy.
     * @param iconDestinationFolderPath The destination folder where the icon will be copied into.
     * @param newIconFileName The new icon file name. If {@code null} it will remain the old one given in 'iconFileName'.
     * @param marker Optional logger marker.
     */
    @Deprecated(forRemoval = true)
    public static void copyJarIcon(Path jarFilePath, String iconFileName, Path iconDestinationFolderPath, @Nullable String newIconFileName, Marker... marker) {
        try (FileSystem jarFileSystem = FileSystems.newFileSystem(jarFilePath)) {
            String iconFile = iconFileName;
            if (!iconFileName.endsWith(".png")) iconFile += ".png";

            Path jarIconPath = jarFileSystem.getPath(iconFile);

            if (Files.exists(jarIconPath) && Files.isRegularFile(jarIconPath)) {
                try (InputStream is = Files.newInputStream(jarIconPath)) {
                    PL.logI("Coping icon file '" + jarIconPath + "' from jar file", marker);

                    String newIconFile = newIconFileName != null ? newIconFileName : iconFile;
                    if (newIconFileName != null && !newIconFileName.endsWith(".png")) newIconFile += ".png";

                    Files.copy(is, iconDestinationFolderPath.resolve(newIconFile), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    PL.logE("Exception caught during copy of icon file " + jarIconPath + ". Exception: " + e, marker);
                }
            } else {
                PL.logW("No icon file found in jar file. Searched path: " + jarIconPath, marker);
            }
        } catch (IOException e) {
            PL.logE("Exception caught during jar file system creation: " + e, marker);
        }
    }

    /**
     * Copies an icon file from the given path.
     * <br><b>NOTE:</b> the method replaces a possibly already existing icon in the destination.
     * @param iconPath The path of the icon file.
     * @param iconDestinationFolderPath The destination folder where the icon will be copied into.
     * @param newIconFileName The new icon file name. If {@code null} it will remain the old one (last part of the path, the file).
     * @param moreDebug If more debug infos on the icon file you're trying to copy should be printed.
     *                   <br>It's here for testing purposes only, should always be set false when you're sure the copy works.
     */
    @Deprecated(forRemoval = true)
    public static void copyPngIcon(Path iconPath, Path iconDestinationFolderPath, @Nullable String newIconFileName, boolean moreDebug) {
        if (moreDebug) {
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

    @Deprecated
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
}