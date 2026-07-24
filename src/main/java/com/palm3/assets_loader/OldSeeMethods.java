package com.palm3.assets_loader;

import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.flag.FeatureFlagSet;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Arrays;

import static com.mojang.text2speech.Narrator.LOGGER;

@Deprecated
public class OldSeeMethods {/*

    // Here the finalized packs get added to the game.
    @SubscribeEvent
    public void addPackFinders(AddPackFindersEvent event) {
        addPacks(event, TEMP_DIR.resolve("dd_assets"));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            deleteTempAssets(manyLogs());
        }));
    }

    //=================================== RESOURCES ===================================
    public static final Path LOAD_DIR = FMLPaths.GAMEDIR.get().resolve("load_assets");
    public static final Path TEMP_DIR = FMLPaths.GAMEDIR.get().resolve("load_assets/.temp_assets");
    /* Only on windows, I know. Also, nothing to hide, only for making it easier for the final user. This will be used in commonSetup() event
       where the resource packs will get created temporary, with the scope of changing their namespace in order to use duplicate textures.
    * /

    private enum JarFiles implements StringRepresentable {
        JAR_1("Create-Dreams-n-Desires-1.19.2-0.2.5b.PREBETA.jar"),
        JAR_2("Create-DnDesire-1.19.2-1.2c.Beta Mid-Dev.jar");

        private final String name;

        JarFiles(String name) {
            this.name = name;
        }

        @Override
        public @NotNull String getSerializedName() {
            return this.name;
        }
    }

    private static String getJar(int fileNumber) {
        if (fileNumber == 0) return JarFiles.JAR_1.name;
        if (fileNumber == 1) return JarFiles.JAR_2.name;
        else throw new IllegalArgumentException("Available jar files are only " + Arrays.toString(JarFiles.values()));
    }

    private static Path getJarP(int fileNumber) {
        if (fileNumber == 0) return LOAD_DIR.resolve(JarFiles.JAR_1.name);
        if (fileNumber == 1) return LOAD_DIR.resolve(JarFiles.JAR_2.name);
        else throw new IllegalArgumentException("Available jar files are only " + Arrays.toString(JarFiles.values()));
    }
    //================================================================================


    // Creates necessary directories for the loading of original assets.
    private static void createDirectories() {
        Path tempDir = FMLPaths.GAMEDIR.get().resolve(TEMP_DIR);

        if (manyLogs()) LOGGER.info("==============================================================================================");
        LOGGER.info("Creating necessary folders to load Dream n' Desires original assets...");
        try {
            if (!Files.exists(LOAD_DIR)) {
                Files.createDirectories(LOAD_DIR);
                if (manyLogs()) LOGGER.info("Created directory for assets jars '{}'", LOAD_DIR);
            } else {
                if (manyLogs()) LOGGER.info("Directory for assets jars '{}' already present, skipping creation.", LOAD_DIR);
            }
        } catch (IOException e) {
            LOGGER.error("Could not create '{}' directory. Exception: {}", LOAD_DIR, e);
        }

        try {
            if (!Files.exists(tempDir)) {
                Files.createDirectories(tempDir);
                if (manyLogs()) LOGGER.info("Created directory for assets extraction '{}'", tempDir);
            } else {
                if (manyLogs()) LOGGER.info("Directory for extracting assets from jars '{}' already present, skipping creation.", tempDir);
            }

            if (System.getProperty("os.name").toLowerCase().contains("win") && Files.getAttribute(tempDir, "dos:hidden") == (Boolean) false) {
                Files.setAttribute(tempDir, "dos:hidden", true);
                if (manyLogs()) LOGGER.info("Windows detected. Applied attributes 'dos:hidden' to directory '.temp_assets'");
            } else if (Files.getAttribute(tempDir, "dos:hidden") == (Boolean) true) {
                if (manyLogs()) LOGGER.info("Windows detected. Attribute 'dos:hidden' of directory '.temp_assets' already 'true'");
            }
        } catch (IOException e) {
            LOGGER.error("Could not create '{}' directory. Exception: {}", tempDir, e);
        }
    }

    // Prepares the assets from the original jars with different namespaces than create_dd.
    private static void prepareJarsForPackCreation() {
        Path game = FMLPaths.GAMEDIR.get();  // Game directory (.minecraft/).
        Path tempDir = game.resolve(TEMP_DIR);  // Temporary directory for the extraction of the assets from the jars, located in '.minecraft/load_assets/.temp_assets'.

        for (int i = 0; i < 2; i++) {  // For two times the assets get loaded from the respective jar.

            // The variables below could be deleted but i left them here in case it's useful.
            //Path tempPackPath = game.resolve(TEMP_DIR + "/dd_assets_" + (i + 1) + "/assets/create_dd_" + (i + 1));  // 2 total packs with 1 namespace
            Path tempPackPath = game.resolve(TEMP_DIR + "/dd_assets/assets/create_dd_" + (i + 1));  // 1 total pack with 2 namespaces
            // The assets (all the content of create_dd/) get copied here so it has a different namespace (create_dd_1 and 2).
            // This is done because some assets have same name but are different in the two jar version used for this mod, and we want to use both.

            // Creates the filesystem in the jar file, no ClassLoader.
            try (FileSystem jarFileSystem = FileSystems.newFileSystem(game.resolve(getJarP(i)), (ClassLoader) null)) {

                Path jarAssets = jarFileSystem.getPath("/assets/create_dd");  // The location of the assets in the jar file.

                // Checks if '.temp_assets' exists (needed to copy assets there) and acts accordingly.
                int tries = 0;  // Number of tries before giving up if the creation fails.
                boolean inWhile = true;
                boolean tempDirExists = Files.exists(tempDir);
                if (!tempDirExists) {
                    if (manyLogs()) LOGGER.warn("Something went wrong before prepareJarsForPackCreation() at com.palm3.dreamdesires.DDMain:136 and the '/load_assets/.temp_assets' directory hasn't been created yet. Trying to create it now.");
                    while (inWhile && tries < 5) {
                        tries++;
                        try {
                            Files.createDirectories(tempDir);
                            if (System.getProperty("os.name").toLowerCase().contains("win") && Files.getAttribute(tempDir, "dos:hidden") == (Boolean) false) {
                                Files.setAttribute(tempDir, "dos:hidden", true);
                                if (manyLogs()) LOGGER.info("Windows detected. Applied attributes 'dos:hidden' to directory '.temp_assets'");
                            } else if (Files.getAttribute(tempDir, "dos:hidden") == (Boolean) true) {
                                if (manyLogs()) LOGGER.info("Windows detected. Attribute 'dos:hidden' of directory '.temp_assets' already 'true'");
                            }
                        } catch (IOException e) {
                            LOGGER.error("Could not create '{}' directory. Exception: {}", tempDir, e.getMessage());
                        }
                        if (Files.exists(tempDir)) {
                            inWhile = false;
                            if (manyLogs()) LOGGER.info("Successfully created directory 'load_assets/.temp_assets'.");
                        } else if (!Files.exists(tempDir) && tries >= 5) {
                            inWhile = false;
                            LOGGER.error("Could not create 'load_assets/.temp_assets' directory, tried 5 times.");
                        }
                    }
                }

                // Copies assets from the jar to '.temp_assets' with a different namespace (copies the content of 'create_dd/', not '/create_dd/*')
                if (Files.exists(tempDir)) {
                    try {
                        Files.walk(jarAssets).forEach(path -> {
                            Path relJarAsset = jarAssets.relativize(path);
                            /* Relativizes the current file in the loop relative to the jar assets dir (.minecraft/load_assets/mod.jar/assets/create_dd)
                               If in the loop we have an item texture file named wand, this is what we have:
                               We're IN -> '.minecraft/load_assets/mod.jar/assets/create_dd/textures/item/wand.png' (path)
                               We need to GO in -> '.minecraft/load_assets/.temp_assets/dd_resources/assets/create_dd_n/textures/item/wand.png'  (n = number of the resource pack, so we can use both version of textures)
                               This RETURNS -> 'textures/item/wand.png', that can be copied in '.temp_assets/dd_resources/assets/create_dd_n/' (tempPackPath variable).
                               Because it relativizes the whole path of file (.minecraft/load_assets/mod.jar/assets/create_dd/textures/item/wand.png) to '.minecraft/load_assets/mod.jar/assets/create_dd/', like a subtraction.
                            * /
                            if (manyLogs()) LOGGER.info("Found asset file/directory in jar, coping {}", relJarAsset);

                            Path copyTarget = tempPackPath.resolve(relJarAsset.toString());  // After the relativization, this is the destination path in '.temp_assets'.

                            // If current path is a directory, create a directory, otherwise copy the file.
                            if (Files.isDirectory(path)) {
                                try {
                                    Files.createDirectories(copyTarget);
                                } catch (IOException e) {
                                    LOGGER.error("Exception caught during creation of directory {}. Exception: ", copyTarget, e);
                                }
                            } else {
                                try (InputStream is = Files.newInputStream(path)) {
                                    Files.copy(is, copyTarget, StandardCopyOption.REPLACE_EXISTING);
                                } catch (IOException e) {
                                    LOGGER.error("Exception caught during copy of file {} inside '.temp_assets/...'. Exception: {}", copyTarget.relativize(tempPackPath), e);
                                }
                            }
                        });
                    } catch (IOException e) {
                        LOGGER.error("Error during jar assets files walk. Exception: {}", String.valueOf(e));
                    }

                    // Copies the icon
                    if (i == 0) try {  // 0 -> old icon (bobert), 1 -> new icon (gear)
                        LOGGER.info("Coping icon file.");
                        Path packIcon = TEMP_DIR.resolve("dd_assets/pack.png");
                        Path jarIcon = jarFileSystem.getPath("/").resolve("pack.png");

                        InputStream is = Files.newInputStream(jarIcon);
                        Files.copy(is, packIcon, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        LOGGER.error("Error during jar icon copy. Exception: {}", String.valueOf(e));
                    }
                } else {
                    LOGGER.error(LogUtils.FATAL_MARKER, "Could not create 'load_assets/.temp_assets' directory, tried 5 (+1) times. Cannot proceed with the Dream n' Desires assets loading since the said directory is required.");
                }
            } catch (IOException e) {
                LOGGER.error("Could not load {} as a file system: {}", game.resolve(getJar(i)), e);
            }
        }
    }

    // Deletes all the temp files used for resource packs in '.temp_assets'.
    private static void deleteTempAssets(boolean listFile) {
        Path game = FMLPaths.GAMEDIR.get();  // Game directory (.minecraft/).
        Path tempDir = game.resolve(TEMP_DIR);  // Temporary directory for the extraction of the assets from the jars, located in '.minecraft/load_assets/.temp_assets'.

        /*LOGGER.info("Deleting temporary assets in '.minecraft/load_assets/.temp_assets'");
        if (Files.exists(tempDir)) {
            try {
                Files.walk(tempDir).sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        if (listFile) LOGGER.info("Deleting {}", tempDir.relativize(path));
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        LOGGER.error("Error during file delete. Exception: {}", String.valueOf(e));
                    }
                });
            } catch (IOException e) {
                LOGGER.error("Error during temporary assets file delete. Exception: {}", String.valueOf(e));
            }
        } else {
            LOGGER.info("Folder '.minecraft/load_assets/.temp_assets' does not exist, nothing to delete.");
        }* /
    }

    // Adds the Dream n' Desires assets to minecraft assets
    private static void addPacks(@NotNull AddPackFindersEvent event, @NotNull Path assetsPath) {
        LOGGER.info("Adding extracted assets to in-game resourcepack named 'create_dd_assets' from files in {}", assetsPath);

        PathPackResources externalPack = new PathPackResources("create_dd_assets", assetsPath, false);
        event.addRepositorySource((infoConsumer) -> {
            Pack pack = Pack.create(
                    "create_dd_assets",
                    Component.literal("Dream 'N' Desires Assets"),
                    true,  // Cannot be disabled?
                    (id) -> externalPack,
                    new Pack.Info(Component.literal("Resources for Dream 'N' Desires: Recrafted"), 15, FeatureFlagSet.of()),
                    PackType.CLIENT_RESOURCES,
                    Pack.Position.TOP,
                    false,  // Cannot change order?
                    PackSource.DEFAULT
            );
            infoConsumer.accept(pack);
        });
    }

    */


}
