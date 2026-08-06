package com.palm3.packs_loader.common;

import com.palm3.packs_loader.logging.PrettyLogging;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import static com.palm3.packs_loader.PacksLoaderMain.*;

/**
 * Provides common methods for directory, pack creation and others. The class has been instantiated due to
 * the need of knowing which class is calling these methods, otherwise always shown as [co.pa.pa.co.CommonMethods/]:
 */
@Deprecated(forRemoval = true)
public class CommonMethods {
    /**
     * Contains various static utility methods that don't log anything, and thus can be used without logger instance. All method throw {@link RuntimeException}
     * if something fails, since there's no logger to log the exceptions.
     */
    @Deprecated(forRemoval = true)
    public static class Unlogged {
        /**
         * Creates a directory inside the game folder {@code .minecraft}.
         * @param directory The directory you want to create, can also be a path.
         *                  E.g. {@code assets/temp}. Don't insert points in the path/folder name, they will get deleted.
         * @param hidden If the directory should be hidden. If you're using a path, only the first (highest) directory of the path will be hidden.
         *               <br>E.g. in {@code assets/temp} only the {@code assets} directory will be hidden (and also renamed {@code .assets}).
         * @throws RuntimeException If the directory creation fails.
         */
        @Deprecated(forRemoval = true)
        public static Path createDirectoryAndGetPath(String directory, boolean hidden) {
            if (directory.contains(".")) directory = directory.replace(".", "");
            Path dir = hidden ? GAME_DIR.resolve("." + directory) : GAME_DIR.resolve(directory);

            if (!Files.exists(dir)) {
                try {
                    Files.createDirectories(dir);
                    if (hidden && System.getProperty("os.name").toLowerCase().contains("win") && Files.getAttribute(dir, "dos:hidden") == (Boolean) false) {
                        Files.setAttribute(dir, "dos:hidden", true);
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Exception caught during directory creation: " + e);
                }
            }
            return dir;
        }

        /**
         * Deletes a directory inside the game folder {@code .minecraft}.
         * @param directory The directory you want to delete, can also be a path.
         *                  E.g. {@code assets/temp}. Don't insert points in the path/folder name, they will get deleted.
         * @param deleteItself If you want to also delete the directory itself instead of the content only.
         * @param hidden If the directory is hidden (the name should also begin with '.').
         * @throws RuntimeException If the directory/file deletion fails or files walk fails.
         */
        @Deprecated(forRemoval = true)
        public static void deleteDirectory(String directory, boolean deleteItself, boolean hidden) {
            if (directory.contains(".")) directory = directory.replace(".", "");
            Path dir = hidden ? GAME_DIR.resolve("." + directory) : GAME_DIR.resolve(directory);

            if (Files.exists(dir)) {
                try (Stream<Path> pathStream = Files.walk(dir)) {
                    pathStream.sorted(Comparator.reverseOrder()).forEach(path -> {
                        try {
                            if (deleteItself) Files.deleteIfExists(path);
                            else if (!path.equals(dir)) Files.deleteIfExists(path);
                        } catch (IOException e) {
                            throw new RuntimeException("Exception caught during file/directory delete. Path: " + dir + " Exception: " + e);
                        }
                    });
                } catch (IOException e) {
                    throw new RuntimeException("Exception caught during directory files walk: " + e);
                }
            }
        }
    }


    /**
     * Contains various utility methods. The class is instantiated to let it have a {@link PrettyLogging} instance used for all methods.
     * Unless specified, methods don't throw exceptions, they log them as errors.
     */
    @Deprecated(forRemoval = true)
    public static class Logged {
        private final PrettyLogging pl;

        /**
         * Construct an instance of this class.
         * @param prettyLogging The pretty logging instance of the class you're using these methods in.
         */
        @Deprecated(forRemoval = true)
        public Logged(PrettyLogging prettyLogging) {
            pl = prettyLogging;
        }

        /**
         * Creates a directory inside the game folder {@code .minecraft}.
         * @param directory The directory you want to create, can also be a path.
         *                  E.g. {@code assets/temp}. Don't insert points in the path/folder name, they will get deleted.
         * @param hidden If the directory should be hidden. If you're using a path, only the first (highest) directory of the path will be hidden.
         *               <br>E.g. in {@code assets/temp} only the {@code assets} directory will be hidden (and also renamed {@code .assets}).
         */
        @Deprecated(forRemoval = true)
        public Path createDirectoryAndGetPath(String directory, boolean hidden) {
            if (directory.contains(".")) directory = directory.replace(".", "");
            Path dir = hidden ? GAME_DIR.resolve("." + directory) : GAME_DIR.resolve(directory);

            if (!Files.exists(dir)) {
                pl.logI("Creating directory '" + directory + "' inside game folder.");
                try {
                    Files.createDirectories(dir);
                    if (hidden) {
                        if (System.getProperty("os.name").toLowerCase().contains("win") && Files.getAttribute(dir, "dos:hidden") == (Boolean) false) {
                            Files.setAttribute(dir, "dos:hidden", true);
                            pl.logI("Windows detected. Applied attributes 'dos:hidden' to directory '" + GAME_DIR.relativize(dir) + "', value: 'true'");
                        } else if (Files.getAttribute(dir, "dos:hidden") == (Boolean) false) {
                            pl.logI("Windows detected. Attribute 'dos:hidden' of directory '" + GAME_DIR.relativize(dir) + "' already 'true'");
                        }
                    }
                } catch (IOException e) {
                    pl.logE("Exception caught during directory creation: " + e);
                }
            } else {
                pl.logI("Directory '" + directory + "' already exist, skipping creation.");
            }
            return dir;
        }

        /**
         * Deletes a directory inside the game folder {@code .minecraft}.
         * @param directory The directory you want to delete, can also be a path.
         *                  E.g. {@code assets/temp}. Don't insert points in the path/folder name, they will get deleted.
         * @param deleteItself If you want to also delete the directory itself instead of the content only.
         * @param hidden If the directory is hidden (the name should also begin with '.').
         */
        @Deprecated(forRemoval = true)
        public void deleteDirectory(String directory, boolean deleteItself, boolean hidden) {
            if (directory.contains(".")) directory = directory.replace(".", "");
            Path dir = hidden ? GAME_DIR.resolve("." + directory) : GAME_DIR.resolve(directory);

            pl.logI("Deleting directory '" + directory, 1, PrettyLogging.LogPos.BEFORE);

            if (Files.exists(dir)) {
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
                    pl.logE("Exception caught during directory files walk: " + e);
                }
            } else {
                pl.logI("Folder '" + GAME_DIR.getParent().relativize(dir) + "' does not exist, nothing to delete.");
            }
        }
    }
}
