package com.palm3.packs_loader.common;

import com.palm3.packs_loader.logging.PrettyLogging;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.DosFileAttributeView;
import java.util.Comparator;
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
     * Sets the given file or directory to hidden (on Windows).
     * @param fileOrDirectory The directory or file path.
     */
    public void setHidden(Path fileOrDirectory) {
        try {
            pl.logI("Setting directory/file '" + fileOrDirectory.getFileName() + "' hidden.");
            DosFileAttributeView dosView = Files.getFileAttributeView(fileOrDirectory, DosFileAttributeView.class);
            dosView.setHidden(true);
        } catch (IOException e) {
            pl.logExceptionE("path attributes setting", e);
        }
    }

    /**
     * Used to assemble a directory path.
     * @param first The first directory of the path.
     * @param others Other optional directories.
     * @return a directory path as {@link String} with correct file separators.
     */
    public static String makeDirectory(String first, String... others) {
        StringBuilder actualDir = new StringBuilder(first);
        for (String other : others) {
            actualDir.append(File.separator).append(other);
        }
        return actualDir.toString();
    }

    /**
     * Creates a directory inside the game folder.
     * @param directory The directory or path of directories you want to create. To create a path correctly see {@link FilesCopier#makeDirectory(String, String...)}.
     * @return The path of the created directory.
     */
    public Path createDirectoryAndGetPath(String directory) {
        Path dir = GAME_DIR.resolve(directory);
        if (!Files.exists(dir)) {
            pl.logI("Creating directory '" + GAME_DIR.relativize(dir) + "' inside game folder.");
            try {
                Files.createDirectories(dir);
                if (directory.startsWith(".")) setHidden(dir);
            } catch (IOException e) {
                pl.logE("Exception caught during directory creation: " + e);
            }
        } else {
            pl.logI("Directory '" + GAME_DIR.relativize(dir) + "' already exists in game folder, skipping creation.");
        }
        return dir;
    }

    /**
     * Deletes a directory inside the game folder.
     * @param directory The directory or path of directories you want to delete.
     * @param deleteItself If the directory itself should be deleted.
     */
    public void deleteDirectory(String directory, boolean deleteItself) {
        Path dir = GAME_DIR.resolve(directory);
        if (Files.exists(dir)) {
            pl.logAdditionalI("Deleting directory '" + directory + "'", !deleteItself, " content");
            try (Stream<Path> pathStream = Files.walk(dir)) {
                pathStream.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        if (deleteItself) Files.deleteIfExists(path);
                        else if (!path.equals(dir)) Files.deleteIfExists(path);
                    } catch (IOException e) {
                        pl.logE("Exception caught during file/directory delete. Path: " + dir + " Exception: " + e);
                    }
                });
            } catch (IOException e) {
                pl.logExceptionE("directory files walk", e);
            }
        } else {
            pl.logI("Folder '" + directory + "' does not exist in game folder, nothing to delete.");
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
}
