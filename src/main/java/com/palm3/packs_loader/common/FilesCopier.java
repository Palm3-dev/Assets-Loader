package com.palm3.packs_loader.common;

import com.mojang.logging.LogUtils;
import com.palm3.packs_loader.logging.PrettyLogging;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.*;
import net.minecraft.world.flag.FeatureFlagSet;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.DosFileAttributeView;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static com.palm3.packs_loader.PacksLoaderMain.*;

/**
 * Provides various methods to perform a jar file assets or data copy.
 * The class has been instantiated to make it use the caller class logger.
 * All the {@link IOException}s have been 'muted', thus they will only get logged <b>without stopping the game</b> (unless otherwise specified).
 */
@ParametersAreNonnullByDefault
public class FilesCopier {
    private final PrettyLogging pl;
    private static final PrettyLogging PL = new PrettyLogging(LogUtils.getLogger(), DEF_PL_PARAMS);

    public FilesCopier(PrettyLogging prettyLogging) {
        pl = prettyLogging;
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
        /// Logs all copied files.
        ALWAYS_LOG,
        /// Logs only files that don't already exist.
        LOG_NONEXISTENT,
        /// Doesn't log.
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
     * @param fileOrDirectory The file or directory to apply the attribute to.
     */
    protected void applyDosHidden(Path fileOrDirectory) {
        if (!fileOrDirectory.getFileName().toString().startsWith(".") || !isOnWindows()) {
            pl.conditionalW(!isOnWindows(), "Tried to apply Windows attribute (dos:hidden) on non-Windows machine, skipped.");
            return;
        }
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
     * Checks if the given path (that should be absolute) ends inside the game directory {@link net.neoforged.fml.loading.FMLPaths#GAMEDIR}.
     * This means that a path like {@code C:\Users\<user_name>\AppData\Roaming\.minecraft\mods} is valid, on the other hand a path like
     * {@code C:\Users\<user_name>\images\mods} is not valid (even though it ends in mods directory).
     * This is an example, the game directory could also not have a point at the start (like with the third parties launchers)
     * or could also be a completely different name (e.g. {@code run} if you're in a dev environment).
     * @param absolutePath The path (absolute) that needs to be checked.
     * @return {@code true} if the path ends inside the game dir, {@code false} if it doesn't.
     * @throws IllegalArgumentException If the given path is not an absolute path.
     */
    protected static boolean absoluteEndsInGameDir(Path absolutePath) {
        if (!absolutePath.isAbsolute())
            throw new IllegalArgumentException("Checked if absolute path '" + absolutePath + "' is in game directory, but given path it's not absolute!");
        return absolutePath.normalize().startsWith(GAME_DIR);
    }

    // IllegalArgument if path doesn't end in game dir or not absolute.
    /**
     * IllegalArgumentException msg: "The given path (absolute) '-path-' is not in the game directory!"
     */
    private static void absoluteEndsInGameDirOrThrow(Path absolutePath) {
        if (!absoluteEndsInGameDir(absolutePath))
            throw new IllegalArgumentException("The given path (absolute) '" + absolutePath + "' is not in the game directory!");
    }

    /**
     * If on Windows OS, it finds and hides all the directories and files in the path - starting from the game directory - that should be hidden
     * (this means that the directory/file name needs to start with {@code .} to be made hidden).
     * @param path The path you want to hide (itself and content).
     *             If it's not an absolute path (e.g. {@code folder_A/folder_B}) it will get resolved against the game dir {@link net.neoforged.fml.loading.FMLPaths#GAMEDIR}.
     *             If it is absolute, and it ends in the game dir, the method will apply the attributes. On the other hand, if the path doesn't end in the game dir
     *             an {@link IllegalArgumentException} will be thrown.
     * @param hideFiles If files should be made hidden, filter defined by {@link Files#isRegularFile(Path, LinkOption...)}.
     * @param hideContentIfDirectory If the given path is a directory, defines if the subdirectories and
     *                               files of that path should be also set hidden (files will be only made hidden if {@code hideFiles} is {@code true}).
     *                               If the given path is not a directory, this value is <b>ignored</b>.
     * @throws IllegalArgumentException If the given path <b>is an absolute path</b> and doesn't end somewhere inside the game dir.
     */
    public void setHiddenFromGameDir(Path path, boolean hideFiles, boolean hideContentIfDirectory) {
        if (!isOnWindows()) {
            pl.logI("Non-windows machine, skipping 'dos:hidden' attribute setting.");
            return;
        }

        Path absolutePath = path.isAbsolute() ? path : GAME_DIR.resolve(path);
        absoluteEndsInGameDirOrThrow(absolutePath);
        Path gameRelativePath = GAME_DIR.relativize(absolutePath);

        // Hides directories (and file if present and with permission 'hideFiles = true') of the given path.
        Path target = GAME_DIR;
        for (int i = 0; i < gameRelativePath.getNameCount(); i++) {
            target = target.resolve(gameRelativePath.getName(i));
            if ((hideFiles && (Files.isDirectory(target) || Files.isRegularFile(target))) || Files.isDirectory(target))
                applyDosHidden(target);
        }

        // Hides all subdirectories (and subfiles if with permission 'hideFiles = true') of the path, only if absolute path is a directory (file cannot be walked).
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
     * Creates a directory inside the game folder, unless it already exists.
     * Sets the directory path (starting from the game directory) and files hidden, see {@code hideContent} parameter for details.
     * @param directory The directory path inside the game folder. You should pass a non-absolute {@link Path}, so that it
     *                  gets resolved against the game dir {@link net.neoforged.fml.loading.FMLPaths#GAMEDIR}.
     *                  <b>If you pass an absolute path,</b> it should already be a path that ends inside the game dir.
     *                  <br><b>IMPORTANT:</b> all the given path will be treated as a directory, even if you give {@code my/path/file.txt} a directory
     *                  {@code file.txt} will be created!
     * @param hideContent If {@code null} the given directory will be set hidden only if it doesn't already exist
     *                    (so usually only the first time the method gets called unless the directory gets deleted or others).
     *                    On the other hand, if it's not {@code null}, the directory will be set hidden every time the method gets called.
     *                    When not null, the value of this {@link Boolean} is used to determine if the content of the given directory
     *                    should be also set hidden; this every time the method gets called, remember.
     * @return The absolute path inside the game dir of the created directory.
     * @throws IllegalArgumentException If the given directory <b>is an absolute path</b> and doesn't end somewhere inside the game dir.
     * @throws RuntimeException If an {@link IOException} occurs during the directory creation.
     */
    public Path createDirectory(Path directory, @Nullable Boolean hideContent) {
        Path absoluteDirectory = directory.isAbsolute() ? directory : GAME_DIR.resolve(directory);
        absoluteEndsInGameDirOrThrow(absoluteDirectory);

        if (!Files.exists(absoluteDirectory)) {
            pl.logI("Creating directory '" + GAME_DIR.relativize(absoluteDirectory) + "' inside game folder.");
            try {
                Files.createDirectories(absoluteDirectory);
                setHiddenFromGameDir(absoluteDirectory, false, false);
            } catch (IOException e) {
                throw new RuntimeException("IOException caught during directory creation: " + e);
            }
        } else {
            pl.logI("Directory '" + GAME_DIR.relativize(absoluteDirectory) + "' already exists in game folder, skipping creation.");
        }
        if (hideContent != null) setHiddenFromGameDir(absoluteDirectory, true, hideContent);
        return absoluteDirectory;
    }

    /**
     * Deletes a directory inside the game folder and its content.
     * @param directory The directory you want to delete.
     * @param deleteItself If the directory itself should be deleted.
     * @throws IllegalArgumentException If the given directory path doesn't represent a directory OR
     * if it <b>is an absolute path</b> and doesn't end somewhere inside the game dir.
     */
    public void deleteDirectory(Path directory, boolean deleteItself) {
        Path absoluteDirectory = directory.isAbsolute() ? directory : GAME_DIR.resolve(directory);
        absoluteEndsInGameDirOrThrow(absoluteDirectory);
        if (Files.exists(absoluteDirectory) && Files.isDirectory(absoluteDirectory)) {
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
            PrettyLogging.conditionalThrow(
                    !Files.isDirectory(absoluteDirectory),
                    new IllegalArgumentException("Given directory path to delete '" + absoluteDirectory + "' doesn't point to a directory!")
            );
            pl.conditionalI(!Files.exists(absoluteDirectory), "Folder '" + absoluteDirectory + "' does not exist in game folder, nothing to delete.");
        }
    }

    /**
     * Copies all the directories and files inside the given jar under the given namespace for the given pack type (file type, data or assets).
     * <br><b>NOTE:</b> the method doesn't throw an {@link IOException} if something fails, it logs it instead so that the game doesn't stop!
     * @param context A record holding the context of the jar copy. See {@link JarFilesCopyContext}.
     * @throws IllegalArgumentException In the following cases:
     * <ul>
     *     <li>The method gets called and the given {@link PackType} from the {@code context} is {@link PackType#BOTH}
     *         (cannot copy more than one file type, can be either assets or data).
 *         </li>
     *     <li>The file represented by the given jar file path in the {@code context} doesn't exist.</li>
     *     <li>If the destination directory doesn't exist (thus method tries to create one)
     *         and its path is an absolute path and doesn't end somewhere inside the game directory,
     *         as mentioned in method {@link #createDirectory(Path, Boolean)}.
 *         </li>
     *     <li>If the destination directory path doesn't point to a directory, as mentioned in method {@link #createDirectory(Path, Boolean)}.</li>
     *     <li>If the jar file path from the {@code context} is absolute and doesn't end in the game directory.</li>
     *     <li>If the actual jar file search via method {@link IncompatibleModsRemover#getModJarPath(Path)} fails in some way with a {@link FileNotFoundException}
     *         as defined in such method.
     *     </li>
     *     <li>The given files destination path from the {@code context} doesn't point to a directory.</li>
     * </ul>
     * <br>
     * @throws RuntimeException If an {@link IOException} occurs during the destination directory creation as mentioned in {@link FilesCopier#createDirectory(Path, Boolean)}.
     */
    // Holy nested methods, warning provided.
    public void copyFilesFromJar(JarFilesCopyContext context) {
        // Pack type is valid (assets or data)?
        PrettyLogging.conditionalThrow(
                context.packType().absoluteFolderName() == null,
                new IllegalArgumentException("Method FilesCopier.copyFilesFromJar(JarFilesCopyContext context) loads one pack type at the time, given PackType value (from context) is PackType.BOTH!")
        );

        // Jar file path exists?
        PrettyLogging.conditionalThrow(
                !Files.exists(context.jarFilePath()),
                new IllegalArgumentException("Unable to copy jar files, jar file of path '" + context.jarFilePath() + "' doesn't exist!")
        );

        // Jar path is absolute?
        Path modJarFilePath;
        boolean isAbsolutePath = context.jarFilePath().isAbsolute();
        try {
            if (isAbsolutePath) {
                absoluteEndsInGameDirOrThrow(context.jarFilePath());
            }
            modJarFilePath = IncompatibleModsRemover.getModJarPath(context.jarFilePath());
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("Tried to get the actual absolute jar path with IncompatibleModsRemover.getModJarPath() but failed with exception: " + e);
        }

        // Destination is absolute?
        Path absoluteFilesDestinationPath = context.filesDestinationPath().isAbsolute() ? context.filesDestinationPath() : context.filesDestinationPath().toAbsolutePath();

        // Path is dir? --> throw
        PrettyLogging.conditionalThrow(
                !Files.isDirectory(absoluteFilesDestinationPath),
                new IllegalArgumentException("The given destination path '" + absoluteFilesDestinationPath + "' for files copy is not a directory!")
        );

        // Destination path exists? --> create
        if (!Files.exists(absoluteFilesDestinationPath)) {
            pl.logW("Destination directory '" + absoluteFilesDestinationPath + "' for jar files copy doesn't already exist, creating now.");
            createDirectory(absoluteFilesDestinationPath, null);  // Possible RuntimeException throw just to remind
        }

        try (FileSystem jarFileSystem = FileSystems.newFileSystem(modJarFilePath)) {
            Path jarFilesPath = jarFileSystem.getPath(context.packType().absoluteFolderName());  // Assets or data

            pl.logI("Walking jar files and coping...");
            pl.logI("| --> Coping files from directory: '" + jarFilesPath + "'. Jar file system root: '" + modJarFilePath + File.separator + "'");
            pl.logI("--> | Coping files inside directory: '" + absoluteFilesDestinationPath + "'");
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
                    Path copyTarget = absoluteFilesDestinationPath.resolve(relativeJarFileOrDir.toString());

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
                                pl.logExceptionE("copy of file '" + fileOrDir + "' inside destination directory '" + GAME_DIR.relativize(absoluteFilesDestinationPath) + "'", e);
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
            pl.logExceptionE("jar file system creation for files copy", e);
        }
    }

    /**
     * Copies an icon from a jar file.
     * @param context A record holding the context of the jar icon copy. See {@link JarIconCopyContext}.
     * @throws IllegalArgumentException In the following cases:
     * <ul>
     *     <li>If the given mod jar file from {@code context} <b>is absolute</b> and it doesn't end in the game directory.</li>
     *     <li>If the actual jar file search via method {@link IncompatibleModsRemover#getModJarPath(Path)}
     *         fails in some way with a {@link FileNotFoundException} as defined in such method.
     *     </li>
     *     <li>If the destination directory doesn't exist (thus method tries to create one)
     *         and its path is an absolute path and doesn't end somewhere inside the game directory,
     *         as mentioned in method {@link #createDirectory(Path, Boolean)}.
     *     </li>
     *     <li>If the destination directory path doesn't point to a directory, as mentioned in method {@link #createDirectory(Path, Boolean)}.</li>
     * </ul>
     * <br>
     * @throws RuntimeException If an {@link IOException} occurs during the destination directory creation as mentioned in {@link #createDirectory(Path, Boolean)}.
     */
    public void copyJarIcon(JarIconCopyContext context) {
        Path modJarFilePath;
        boolean isAbsolutePath = context.jarFilePath().isAbsolute();
        try {
            if (isAbsolutePath) {
                absoluteEndsInGameDirOrThrow(context.jarFilePath());
            }
            modJarFilePath = IncompatibleModsRemover.getModJarPath(context.jarFilePath());
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("Tried to get the actual absolute jar path with IncompatibleModsRemover.getModJarPath() but failed with exception: " + e);
        }

        if (!Files.exists(modJarFilePath) || !Files.isRegularFile(modJarFilePath)) {
            pl.conditionalE(!Files.isRegularFile(modJarFilePath), "Unable to copy jar icon, file of path '" + modJarFilePath + "' isn't a file!");
            pl.conditionalE(!Files.exists(modJarFilePath), "Unable to copy jar icon, file of path '" + modJarFilePath + "' doesn't exist!");
            return;
        }

        Path absoluteIconDestinationPath = context.iconDestinationPath().isAbsolute() ? context.iconDestinationPath() : context.iconDestinationPath().toAbsolutePath();

        if (!Files.exists(absoluteIconDestinationPath)) {
            pl.logW("Destination directory '" + absoluteIconDestinationPath + "' for jar files copy doesn't already exist, creating now.");
            createDirectory(absoluteIconDestinationPath, null);  // Possible RuntimeException throw just to remind
        }

        try (FileSystem jarFileSystem = FileSystems.newFileSystem(modJarFilePath)) {
            String fullIconFileName = context.iconFileName().endsWith(".png") ? context.iconFileName() : context.iconFileName() + ".png";
            Path jarIconPath = jarFileSystem.getPath(fullIconFileName);
            if (Files.exists(jarIconPath) && Files.isRegularFile(jarIconPath)) {
                try (InputStream is = Files.newInputStream(jarIconPath)) {
                    pl.logI("Coping icon file '" + jarIconPath + "' from jar file");
                    String newIconFileName = context.newIconFileName() == null ? fullIconFileName : context.newIconFileName();
                    Files.copy(is, absoluteIconDestinationPath.resolve(newIconFileName.endsWith(".png") ? newIconFileName : newIconFileName + ".png"), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    pl.logExceptionE("copy of jar icon '" + jarIconPath + "'", e);
                }
            } else {
                pl.logW("No icon file found in jar file. Searched path: " + jarIconPath);
            }
        } catch (IOException e) {
            pl.logExceptionE("jar file system creation for jar icon copy", e);
        }
    }

    /**
     * Creates a list of all the namespaces under the assets or data directory (based on given pack type) in the given jar file.
     * @param jarFilePath The jar file you want to search. Will be searched in the {@code mods} dir.
     * @param packType The {@link PackType} to determine to search assets or data.
     * @return The {@link List} of namespaces as {@link String}. An <b>empty</b> list if the given jar file doesn't exist.
     * @throws IllegalArgumentException In the following conditions:
     * <ul>
     *     <li>The method gets called and the given {@link PackType} is {@link PackType#BOTH}
     *         (cannot copy more than one file type, can be either assets or data).
     *     </li>
     *     <li>If the jar file path is absolute and doesn't end in the game directory.</li>
     *     <li>If the actual jar file search via method {@link IncompatibleModsRemover#getModJarPath(Path)} fails in some way with a {@link FileNotFoundException}
     *         as defined in such method.
     *     </li>
     * </ul>
     */
    protected static List<String> discoverJarNamespaces(Path jarFilePath, PackType packType) {
        PrettyLogging.conditionalThrow(
                packType.absoluteFolderName() == null,
                new IllegalArgumentException("Method FilesCopier.discoverNamespaces(Path, PackType) only accepts one pack type at the time, given PackType value is PackType.BOTH!")
        );

        Path modJarFilePath;
        boolean isAbsolutePath = jarFilePath.isAbsolute();
        try {
            if (isAbsolutePath) {
                absoluteEndsInGameDirOrThrow(jarFilePath);
            }
            modJarFilePath = IncompatibleModsRemover.getModJarPath(jarFilePath);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("Tried to get the actual absolute jar path with IncompatibleModsRemover.getModJarPath() but failed with exception: " + e);
        }

        if (!Files.exists(modJarFilePath) || !Files.isRegularFile(modJarFilePath)) {
            PL.conditionalE(!Files.isRegularFile(modJarFilePath), "Unable to search namespaces (" + packType.absoluteFolderName() + "), file of path '" + modJarFilePath + "' isn't a file!");
            PL.conditionalE(!Files.exists(modJarFilePath), "Unable to search namespaces (" + packType.absoluteFolderName() + "), file of path '" + modJarFilePath + "' doesn't exist!");
            return List.of();
        }

        List<String> namespaces = new ArrayList<>();
        try (FileSystem jarFileSystem = FileSystems.newFileSystem(modJarFilePath)) {
            Path targetNamespacesPath = jarFileSystem.getPath(packType.absoluteFolderName());
            PL.logI("Searching jar namespaces in: " + targetNamespacesPath + ", jar file: " + modJarFilePath.getFileName());
            try (Stream<Path> pathStream = Files.walk(targetNamespacesPath)) {
                pathStream
                        .filter(path -> {
                            if (path.toAbsolutePath().getParent().equals(targetNamespacesPath)) {
                                PL.logI("Found namespace " + path);
                                return true;
                            }
                            return false;
                        })
                        .forEach(path -> namespaces.add(path.getFileName().toString()));
            } catch (IOException e) {
                PL.logExceptionE("files/directories walk", e);
            }
        } catch (IOException e) {
            PL.logExceptionE("jar file system creation", e);
        }
        return namespaces;
    }

    /**
     * Copies a jar file as a pack, 1:1.
     * @param context The instance of {@link LiteralJarCopyContext}.
     * @throws RuntimeException Various types, as defined in {@link #copyFilesFromJar(JarFilesCopyContext)}, {@link #discoverJarNamespaces(Path, PackType)}
     *                                         and {@link #copyJarIcon(JarIconCopyContext)}.
     */
    public void literalJarCopy(LiteralJarCopyContext context) {
        pl.logCenteredI("Coping full jar file pack for jar: " + context.modJarFile.getFileName().toString(), PrettyLogging.DEF_LINE);

        List<PackType> packTypes = List.of(PackType.ASSETS, PackType.DATAPACK);
        for (PackType packType : packTypes) {
            pl.logI("first for");
            for (String namespace : discoverJarNamespaces(context.modJarFile, packType)) {
                pl.logI("2 for");
                copyFilesFromJar(new JarFilesCopyContext(
                        packType,
                        context.modJarFile,
                        context.destinationPackRoot,
                        namespace,
                        context.forceCopy,
                        context.logCopyOption
                ));
            }
        }

        copyJarIcon(new JarIconCopyContext(
                context.modJarFile,
                context.iconFileName,
                context.destinationPackRoot,
                "pack"
        ));

        pl.logCenteredI("Literal copy of jar '" + context.modJarFile.getFileName().toString() + "' done", PrettyLogging.DEF_LINE);
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


    //================= RECORDS ==================
    /**
     * Provides the context to copy files (data or assets) from a mod jar.
     * @param packType The type of files you're coping (assets or data).
     * @param jarFilePath The path of the mod jar file.
     * @param filesDestinationPath The destination <b>folder</b> of the files.
     * @param namespaceToCopy The namespace to copy inside the assets or data folder.
     * @param forceCopy If the copy should be forced, replacing already existing files (if any).
     * @param logCopyOption Option that defines which files should be logged.
     */
    @ParametersAreNonnullByDefault
    public record JarFilesCopyContext(PackType packType, Path jarFilePath, Path filesDestinationPath, String namespaceToCopy, boolean forceCopy, LogCopyOption logCopyOption) {
    }

    /**
     * Context used to copy an icon from a jar file.
     * @param jarFilePath The path of the jar file.
     * @param iconFileName The name of the icon file.
     * @param iconDestinationPath The destination <b>folder</b> of the icon.
     * @param newIconFileName The new name of the icon file. Leave {@code null} to keep the old one.
     */
    @ParametersAreNonnullByDefault
    public record JarIconCopyContext(Path jarFilePath, String iconFileName, Path iconDestinationPath, @Nullable String newIconFileName) {
    }

    /**
     * Represents an icon file for pack creation.
     * @param jarFile The name of the jar file that contains the icon.
     * @param iconFileName The name of the icon file.
     */
    @ParametersAreNonnullByDefault
    public record JarIcon(String jarFile, String iconFileName) {
    }

    /**
     * Holds the parameters to do a full jar copy, 1:1. You get a pack that's the same as the jar file.
     * @param modJarFile The mod jar file.
     * @param destinationPackRoot The destination of the files, the pack root, containing the assets folder, data folder and icon.
     * @param iconFileName The name of the icon file in the jar.
     * @param forceCopy If the copy should be forced, replacing already existing files (if any)
     * @param logCopyOption Option that defines which files should be logged.
     */
    @ParametersAreNonnullByDefault
    public record LiteralJarCopyContext(Path modJarFile, Path destinationPackRoot, String iconFileName, boolean forceCopy, LogCopyOption logCopyOption) {
    }
}
