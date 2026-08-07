package com.palm3.packs_loader.common;

import com.palm3.packs_loader.logging.PrettyLogging;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.*;
import net.minecraft.world.flag.FeatureFlagSet;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.DosFileAttributeView;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static com.palm3.packs_loader.PacksLoaderMain.*;

@ParametersAreNonnullByDefault
public class FilesCopier {
    private final PrettyLogging pl;

    public FilesCopier(PrettyLogging prettyLogging) {
        pl = prettyLogging;
    }

    @ParametersAreNonnullByDefault
    public static record JarFilesCopyContext(Path jarFilePath, Path filesDestinationPath, String namespaceToCopy, PackType packType, boolean forceCopy, LogCopyOption logCopyOption) {
    }

    @ParametersAreNonnullByDefault
    public static record JarIconCopyContext(Path jarFilePath, String iconFileName, Path iconDestinationPath, @Nullable String newIconFileName) {
    }

    /**
     * @return {@code true} if the Minecraft (and the JVM) are running on a Windows OS machine.
     */
    @SuppressWarnings("all")  // Shut up, i know it's inverted.
    public static boolean isOnWindows() {
        return Util.getPlatform() == Util.OS.WINDOWS;
    }

    /**
     * Represents an option for copy logs.
     */
    public enum LogCopyOption {
        ALWAYS_LOG,
        LOG_NONEXISTENT,
        NEVER_LOG;

        LogCopyOption() {}

        /**
         * If the current enum value is LOG_NONEXISTENT, returns true only if the given path doesn't exist.
         */
        private boolean canLog(Path copyTarget) {
            if (this == ALWAYS_LOG) return true;
            return this == LOG_NONEXISTENT && !Files.exists(copyTarget);

        }
    }

    /**
     * If on Windows OS, applies attribute {@code dos:hidden} to the given file or directory.
     * If the file/directory name doesn't start with {@code .} it does nothing.
     */
    protected void applyDosHidden(Path fileOrDirectory) {
        if (!fileOrDirectory.getFileName().toString().startsWith(".") || !isOnWindows()) return;
        try {
            DosFileAttributeView dosView = Files.getFileAttributeView(fileOrDirectory, DosFileAttributeView.class);
            if (dosView == null) return;
            if (!dosView.readAttributes().isHidden()) {
                pl.logI("Setting directory/file '" + fileOrDirectory.getFileName() + "' hidden.");
                dosView.setHidden(true);
            } else {
                pl.logI("Directory/file " + fileOrDirectory.getFileName() + "' already is hidden.");
            }
        } catch (IOException e) {
            pl.logExceptionE("path attributes setting", e);
        }
    }


    /**
     * If on Windows OS, it finds and hides all the directories and files in the path starting from the game directory that should be hidden
     * (this means that the directory/file name needs to start with {@code .}).
     * @param path The path you want to hide (itself and content).
     * @param hideFiles If files should be made hidden.
     * @param hideContentIfDirectory If the given path is a directory, defines if the subdirectories and
     *                               files (only made hidden if hideFiles is {@code true}) of that path should be also set hidden.
     *                               If the given path is not a directory, this value is ignored.
     */
    public void setHiddenFromGameDir(Path path, boolean hideFiles, boolean hideContentIfDirectory) {
        if (!isOnWindows()) {
            pl.logI("Non-windows machine, skipping 'dos:hidden' attribute setting.");
            return;
        }

        Path absolutePath = path.isAbsolute() ? path : GAME_DIR.resolve(path);
        Path gameRelativePath = GAME_DIR.relativize(absolutePath);

        // Hides directories (and file if present and with permission 'hideFiles = true') of the given path.
        Path target = GAME_DIR;
        for (int i = 0; i < gameRelativePath.getNameCount(); i++) {
            target = target.resolve(gameRelativePath.getName(i));
            if ((hideFiles && (Files.isDirectory(target) || Files.isRegularFile(target))) || Files.isDirectory(target))
                applyDosHidden(target);
        }

        // Hides all subdirectories (and subfiles if with permission 'hideFiles = true') of the path.
        if (Files.isDirectory(absolutePath) && hideContentIfDirectory) {
            pl.logI("Hiding directory '" + GAME_DIR.relativize(absolutePath) + "' content.");
            try (Stream<Path> pathStream = Files.walk(absolutePath)) {
                pathStream.forEach(p -> {
                    if ((hideFiles && (Files.isDirectory(p) || Files.isRegularFile(p))) || Files.isDirectory(p))
                        applyDosHidden(p);
                });
            } catch (IOException e) {
                pl.logExceptionE("path '" + absolutePath + "' files walk to make them hidden", e);
            }
        }
    }

    /**
     * Creates a directory inside the game folder.
     * @param directory The directory path <b>inside the game folder.</b> You should pass a non-absolute {@link Path}, so that it
     *                  gets resolved against the game dir {@link net.neoforged.fml.loading.FMLPaths#GAMEDIR}.
     *                  <b>If it is absolute,</b> it should already be a path that ends inside the game folder.
     * @param hideContent If {@code null} the given directory will be set hidden only if it doesn't already exist
     *                    (so usually only the first time the method gets called unless the directory gets deleted or others).
     *                    On the other hand, if it's not null, the directory will be set hidden every time the method gets called.
     *                    When not null, the value of this {@link Boolean} is used to determine if the content of the given directory
     *                    should be also set hidden; this every time the method gets called, remember.
     *
     */
    public void createDirectory(Path directory, @Nullable Boolean hideContent) {
        Path absoluteDirectory = directory.isAbsolute() ? directory : GAME_DIR.resolve(directory);
        if (!Files.exists(absoluteDirectory)) {
            pl.logI("Creating directory '" + GAME_DIR.relativize(absoluteDirectory) + "' inside game folder.");
            try {
                Files.createDirectories(absoluteDirectory);
                setHiddenFromGameDir(absoluteDirectory, false, false);
            } catch (IOException e) {
                pl.logE("Exception caught during directory creation: " + e);
            }
        } else {
            pl.logI("Directory '" + GAME_DIR.relativize(absoluteDirectory) + "' already exists in game folder, skipping creation.");
        }
        if (hideContent != null) setHiddenFromGameDir(absoluteDirectory, true, hideContent);
    }

    /**
     * Deletes a directory.
     * @param directory The directory you want to delete.
     * @param deleteItself If the directory itself should be deleted.
     */
    public void deleteDirectory(Path directory, boolean deleteItself) {
        Path absoluteDirectory = directory.isAbsolute() ? directory : GAME_DIR.resolve(directory);
        if (Files.exists(absoluteDirectory)) {
            pl.logAdditionalI("Deleting directory '" + absoluteDirectory + "'", !deleteItself, " content");
            try (Stream<Path> pathStream = Files.walk(absoluteDirectory)) {
                pathStream.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        if (deleteItself) Files.deleteIfExists(path);
                        else if (!path.equals(absoluteDirectory)) Files.deleteIfExists(path);
                    } catch (IOException e) {
                        pl.logE("Exception caught during file/directory delete. Path: " + path + " Exception: " + e);
                    }
                });
            } catch (IOException e) {
                pl.logExceptionE("directory files walk", e);
            }
        } else {
            pl.logI("Folder '" + absoluteDirectory + "' does not exist in game folder, nothing to delete.");
        }
    }

    /**
     * Copies all the directories and files inside the given jar under the given namespace.
     * @param context A record holding the context of the jar copy. See {@link JarFilesCopyContext}.
     */
    // Holy nested methods, warning provided.
    public void copyFilesFromJar(JarFilesCopyContext context) {
        try (FileSystem jarFileSystem = FileSystems.newFileSystem(context.jarFilePath())) {
            Path jarFilesPath = jarFileSystem.getPath(context.packType().getDirectory());

            pl.logI("Walking jar files and coping...");
            pl.logI("| --> Coping files from directory: '" + jarFilesPath + "'. Jar file system root: '" + context.jarFilePath() + File.separator + "'");
            pl.logI("--> | Coping files inside directory: '" + context.filesDestinationPath() + "'");
            pl.conditionalI(context.forceCopy(), "Force copy enabled, files will be replaced!");

            try (Stream<Path> jarFilesStream = Files.walk(jarFilesPath)) {
                // All used for logging
                AtomicInteger totalPaths = new AtomicInteger();
                AtomicInteger totalFiles = new AtomicInteger();
                AtomicInteger totalDirectories = new AtomicInteger();
                AtomicInteger copiedFiles = new AtomicInteger();
                AtomicInteger copiedDirectories = new AtomicInteger();
                AtomicBoolean allAlreadyExist = new AtomicBoolean(true);
                AtomicInteger replacedFiles = new AtomicInteger();

                jarFilesStream.forEach(fileOrDir -> {
                    Path relativeJarFileOrDir = jarFilesPath.relativize(fileOrDir);
                    Path copyTarget = relativeJarFileOrDir.resolve(relativeJarFileOrDir.toString());

                    // Logging purpose
                    totalPaths.incrementAndGet();
                    if (Files.isRegularFile(fileOrDir)) totalFiles.incrementAndGet();
                    if (Files.isDirectory(fileOrDir)) totalDirectories.incrementAndGet();
                    if (allAlreadyExist.get() && !Files.exists(copyTarget))
                        allAlreadyExist.set(false);

                    if ((Files.exists(copyTarget) && context.forceCopy()) || !Files.exists(copyTarget)) {
                        if (Files.isRegularFile(fileOrDir)) {
                            try (InputStream is = Files.newInputStream(fileOrDir)) {
                                if (!Files.exists(copyTarget)) copiedFiles.incrementAndGet();
                                else replacedFiles.incrementAndGet();
                                pl.conditionalI(context.logCopyOption().canLog(copyTarget), "Coping file '" + relativeJarFileOrDir + "'");
                                Files.copy(is, copyTarget, StandardCopyOption.REPLACE_EXISTING);
                            } catch (IOException e) {
                                pl.logExceptionE("copy of file '" + fileOrDir + "' inside destination directory '" + GAME_DIR.relativize(context.filesDestinationPath()) + "'", e);
                            }

                        } else if (Files.isDirectory(fileOrDir)) {
                            try {
                                pl.conditionalI(context.logCopyOption().canLog(copyTarget), "Coping directory '" + relativeJarFileOrDir + "'");
                                Files.createDirectories(copyTarget);
                                copiedDirectories.incrementAndGet();
                            } catch (IOException e) {
                                pl.logE("Exception caught during creation of directory " + copyTarget + ". Exception: " + e);
                            }

                        } else {
                            pl.logW("Path " + fileOrDir + " is neither a file nor a directory.");
                        }
                    }
                });

                // Log results
                pl.logCenteredI("Copy results", pl.line2, true, true);
                pl.logI("Total elements: " + totalPaths.get());
                pl.logI("Total directories: " + totalDirectories.get());
                pl.logI("Total files: " + totalFiles.get());
                pl.logI("Copied directories: " + copiedDirectories.get());
                pl.logI("Copied files: " + copiedFiles.get());
                pl.logI("Replaced files: " + replacedFiles.get());
                pl.conditionalI(allAlreadyExist.get() && !context.forceCopy(), "-> No copy done, already existing files.");
                pl.logI(pl.line2);

            } catch (IOException e) {
                pl.logExceptionE("jar files walk", e);
            }
        } catch (IOException e) {
            pl.logExceptionE("jar file system creation", e);
        }
    }

    /**
     * Copies an icon from a jar file.
     * @param context A record holding the context of the jar icon copy. See {@link JarIconCopyContext}.
     */
    public void copyJarIcon(JarIconCopyContext context) {
        try (FileSystem jarFileSystem = FileSystems.newFileSystem(context.jarFilePath())) {
            String fullIconFileName = context.iconFileName().endsWith(".png") ? context.iconFileName() : context.iconFileName() + ".png";
            Path jarIconPath = jarFileSystem.getPath(fullIconFileName);
            if (Files.exists(jarIconPath) && Files.isRegularFile(jarIconPath)) {
                try (InputStream is = Files.newInputStream(jarIconPath)) {
                    pl.logI("Coping icon file '" + jarIconPath + "' from jar file");
                    String newIconFileName = context.newIconFileName() == null ? fullIconFileName : context.newIconFileName();
                    Files.copy(is, context.iconDestinationPath().resolve(newIconFileName.endsWith(".png") ? newIconFileName : newIconFileName + ".png"));
                } catch (IOException e) {
                    pl.logExceptionE("copy of jar icon '" + jarIconPath + "'", e);
                }
            } else {
                pl.logW("No icon file found in jar file. Searched path: " + jarIconPath);
            }
        } catch (IOException e) {
            pl.logExceptionE("jar file system creation", e);
        }
    }

    /**
     * Copies an icon from a general path inside the game directory.
     * @param iconPath The path of the icon file.
     * @param iconDestinationPath The destination <b>folder</b> of the icon file.
     * @param newIconFileName The new icon file name. If null it will remain the same as the old one.
     * @param debugInfos If more debug infos about the icon file you're trying to copy should be printed, designed to be used only during testing.
     */
    public void copyPngIcon(Path iconPath, Path iconDestinationPath, @Nullable String newIconFileName, boolean debugInfos) {
        if (debugInfos) {
            pl.logI("Debug infos about the icon file you're trying to copy:");
            pl.logI("Given icon path: " + iconPath);
            pl.logI("Is readable: " + Files.isReadable(iconPath));
            pl.logI("File exists: " + Files.exists(iconPath));
            pl.logI("Is regular file: " + Files.isRegularFile(iconPath));
            pl.logI("Path ends with '.png': " + iconPath.getFileName().toString().endsWith(".png"));
        }

        String iconFileName = iconPath.getFileName().toString();
        if (Files.exists(iconPath) && Files.isRegularFile(iconPath) && iconFileName.endsWith(".png")) {
            try (InputStream is = Files.newInputStream(iconPath)) {
                pl.logI("Coping PNG icon file in '" + iconPath + "'");

                String newFullIconFileName = newIconFileName == null ? iconFileName : newIconFileName;
                Files.copy(is, iconDestinationPath.resolve(newFullIconFileName.endsWith(".png") ? newFullIconFileName : newFullIconFileName + ".png"), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                pl.logExceptionE("copy of PNG icon file " + iconPath, e);
            }
        } else {
            pl.logW("No PNG icon file found (or error happened, use copy debug for more infos). Searched path: " + iconPath);
        }
    }


    //shall work, i guess?
    public static RepositorySource createPackRepositorySource(String internalPackId, Component packTitle, Component packDescription, Path packPath, boolean required, boolean hidden) throws RuntimeException {
        PackLocationInfo packLocationInfo = new PackLocationInfo(
                internalPackId,
                packTitle,
                PackSource.DEFAULT,
                Optional.empty()
        );

        PackSelectionConfig selectionConfig = new PackSelectionConfig(required, Pack.Position.BOTTOM, false);
        Pack.Metadata metadata = new Pack.Metadata(packDescription, PackCompatibility.COMPATIBLE, FeatureFlagSet.of(), List.of(), hidden);
        Pack.ResourcesSupplier resourcesSupplier = new PathPackResources.PathResourcesSupplier(packPath);

        Pack pack = new Pack(
                packLocationInfo,
                resourcesSupplier,
                metadata,
                selectionConfig
        );

        return packConsumer -> packConsumer.accept(pack);
    }
}
