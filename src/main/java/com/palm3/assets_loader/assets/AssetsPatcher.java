package com.palm3.assets_loader.assets;

import com.mojang.logging.LogUtils;
import com.palm3.assets_loader.LoaderMain;
import com.palm3.assets_loader.PrettyLogging;
import net.neoforged.fml.loading.FMLPaths;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class AssetsPatcher {

    public static final PrettyLogging PL = new PrettyLogging(LogUtils.getLogger(), LoaderMain.DEF_PL_PARAMS);
    public static final Path GAME_DIR = FMLPaths.GAMEDIR.get();

    @ParametersAreNonnullByDefault
    public static class NamespaceChanger {

        private final @NotNull Path assetsDirectory;
        private final @NotNull List<NamespaceCouple> namespacesCouples;

        public enum ChangeType {
            ITEM_MODELS,
            BLOCK_MODELS,
            ALL_MODELS,
            BLOCK_STATE,
            LANG,
            SOUNDS;

            ChangeType() {}
        }

        private NamespaceChanger(Path packDirectory, List<NamespaceCouple> namespacesCouples) {
            this.assetsDirectory = packDirectory.resolve("assets");
            this.namespacesCouples = namespacesCouples;
        }

        /**
         * Creates a list of multiple {@link NamespaceCouple}.
         * <p>
         * NOTE: Pay attention while using this method.
         * The general order you put the namespaces in the lists is not important; what is important is that you
         * respect the same order and element number you chosen in both lists, otherwise you'll have unmatching namespaces or errors.
         * <p>
         * E.g. {@code oldNamespaces --> oldNamespace_A, oldNamespace_B} & {@code newNamespaces --> newNamespace_A, newNamespace_B}
         * </p>
         * </p>
         * @param oldNamespaces A list of the old namespaces.
         * @param newNamespaces A list of the new namespaces. Remember to match the namespaces in the previous list.
         * @return The {@link List} of {@link NamespaceCouple}.
         * @throws IllegalArgumentException If the lists are different in dimension.
         */
        public static List<NamespaceCouple> createNamespacesList(List<String> oldNamespaces, List<String> newNamespaces) throws IllegalArgumentException {
            // Check lists or throw
            if (oldNamespaces.size() != newNamespaces.size()) {
                String biggerList = oldNamespaces.size() > newNamespaces.size() ? "old_namespaces" : "new_namespaces";
                throw new IllegalArgumentException("The given lists need to be the same size! Bigger list: " + biggerList);
            }

            int newIndex = 0;
            List<NamespaceCouple> namespacesCouples = new ArrayList<>();
            for (String oldNamespace : oldNamespaces) {
                namespacesCouples.add(new NamespaceCouple(oldNamespace, newNamespaces.get(newIndex)));
                newIndex++;
            }
            return namespacesCouples;
        }

        /**
         * Changes multiple namespaces in one pack.
         * <p>
         *     NOTE: Changes the files namespaces, the namespace inside the {@code assets} folder should already be the new one copied by {@link AssetsLoader}.
         *     For example, the pack directory is {@code my_pack_root}. With this you can change the old namespaces occurring in
         *     the files (in the example blockstates) in {@code my_pack_root/assets/new_namespace_A/blockstates <-- some .json files}.
         *     This will change all the old namespaces in the files to the new ones for every given namespace by entering in all the namespaces
         *     folders inside the {@code assets} directory.
         * </p>
         * @param packDirectory The directory containing the pack you want to modify the namespaces (the one with assets folder, the icon, etc.).
         * @param namespacesCouples A {@link List} of {@link NamespaceCouple} with the old and new namespaces. To easily create one you can use
         * {@link NamespaceChanger#createNamespacesList(List, List)}
         * @return A new instance of {@link NamespaceChanger}.
         */
        public static NamespaceChanger multipleNamespaces(Path packDirectory, List<NamespaceCouple> namespacesCouples) {
            return new NamespaceChanger(packDirectory, namespacesCouples);
        }

        /**
         * Changes one namespace in one pack.
         * <p>
         *     NOTE: Changes the files namespaces, the namespace inside the {@code assets} folder should already be the new one copied by {@link AssetsLoader}.
         *     For example, the pack directory is {@code my_pack_root}. With this you can change the old namespaces occurring in
         *     the files (in the example blockstates) in {@code my_pack_root/assets/new_namespace/blockstates <-- some .json files}.
         *     This will change all the old namespaces in the files to the new ones.
         * </p>
         * @param packDirectory The directory containing the pack you want to modify the namespace  (the one with assets folder, the icon, etc.).
         * @param oldNamespace The old namespace to change.
         * @param newNamespace The new namespace that will replace the old one.
         */
        public static NamespaceChanger singleNamespace(Path packDirectory, String oldNamespace, String newNamespace) {
            List<NamespaceCouple> namespacesCouples = new ArrayList<>();
            namespacesCouples.add(new NamespaceCouple(oldNamespace, newNamespace));
            return new NamespaceChanger(packDirectory, namespacesCouples);
        }

        // Replaces all the old namespace occurrences in the given file with the new namespace. targetDir used only for logs.
        private static void processFile(Path targetDirectory, Path file, String oldNamespace, String newNamespace) {
            PL.logI("Found file '" + targetDirectory.relativize(file) + "'.");
            try {
                String fileAsString = Files.readString(file);
                if (fileAsString.contains(oldNamespace)) {
                    fileAsString = fileAsString.replace(oldNamespace, newNamespace);
                    Files.writeString(file, fileAsString);
                    PL.logI("Patched file.");
                } else {
                    PL.logI("File doesn't contain namespace '" + oldNamespace + "', nothing to change.");
                }
            } catch (IOException e) {
                PL.logE("Exception caught during file read/write: " + e);
            }
        }

        // Changes the namespace of all the files in the given dir.
        @SuppressWarnings("all")
        protected static void changeFilesNamespace(Path targetDirectory, String oldNamespace, String newNamespace, @Nullable String fileExtension) {
            try (Stream<Path> stream = Files.walk(targetDirectory)) {
                stream.filter(Files::isRegularFile)
                        .filter(path -> {
                            if (fileExtension == null) return true;
                            return path.toString().endsWith(fileExtension);
                        })
                        .forEach(path -> processFile(targetDirectory, path, oldNamespace, newNamespace));
            } catch (IOException e) {
                PL.logE("Exception caught during files walk: " + e);
            }
        }

        // Repeats given consumer for every present couple of namespaces.
        private void repeatForCouples(Consumer<NamespaceCouple> consumer) {
            for (NamespaceCouple namespaceCouple : namespacesCouples) consumer.accept(namespaceCouple);
        }

        /**
         * Changes the namespace(s) of the specified model types.
         * @param changeType The type of models to change the namespace.
         */
        public void changeModels(ChangeType changeType) {
            repeatForCouples(namespaceCouple -> {
                Path modelsDir = assetsDirectory
                        // The new namespace is used in the directory (changed when copied), only the files have the old one.
                        .resolve(namespaceCouple.newNamespace)
                        .resolve("models");

                // Block models patch
                if (changeType == ChangeType.BLOCK_MODELS || changeType == ChangeType.ALL_MODELS) {
                    changeFilesNamespace(
                            modelsDir.resolve("block"),
                            namespaceCouple.oldNamespace,
                            namespaceCouple.newNamespace,
                            null);
                }
                // Item models patch
                if (changeType == ChangeType.ITEM_MODELS || changeType == ChangeType.ALL_MODELS) {
                    changeFilesNamespace(
                            modelsDir.resolve("item"),
                            namespaceCouple.oldNamespace,
                            namespaceCouple.newNamespace,
                            null);
                }
                // Skipped, invalid
                if (changeType != ChangeType.ALL_MODELS && changeType != ChangeType.BLOCK_MODELS && changeType != ChangeType.ITEM_MODELS) {
                    PL.logW("Tried to change models, but specified change is " + changeType + ", skipping.");
                }
            });
        }

        /**
         * Changes the namespace(s) of the blockstates.
         */
        public void changeBlockStates() {
            repeatForCouples(namespaceCouple -> changeFilesNamespace(
                    // The new namespace is used in the directory (changed when copied), only the files have the old one.
                    assetsDirectory.resolve(namespaceCouple.newNamespace).resolve("blockstates"),
                    namespaceCouple.oldNamespace,
                    namespaceCouple.newNamespace,
                    null));
        }

        /**
         * Changes the namespace(s) of the lang files.
         */
        public void changeLang() {
            repeatForCouples(namespaceCouple -> changeFilesNamespace(
                    // The new namespace is used in the directory (changed when copied), only the files have the old one.                    
                    assetsDirectory.resolve(namespaceCouple.newNamespace).resolve("lang"),
                    namespaceCouple.oldNamespace,
                    namespaceCouple.newNamespace,
                    null));
        }

        /**
         * Changes the {@code sounds.json} file namespace(s).
         */
        public void changeSounds() {
            repeatForCouples(namespaceCouple -> changeFilesNamespace(
                    assetsDirectory.resolve(namespaceCouple.newNamespace),
                    namespaceCouple.oldNamespace,
                    namespaceCouple.newNamespace,
                    null
            ));
        }

        /**
         * Loads custom type of assets.
         * @param changeTypes The types of assets, use commas to separate {@link ChangeType}.
         */
        public void customChanges(ChangeType... changeTypes) {
            for (ChangeType type : changeTypes) {
                if (type == ChangeType.ALL_MODELS || type == ChangeType.BLOCK_MODELS || type == ChangeType.ITEM_MODELS)
                    changeModels(type);
                if (type == ChangeType.SOUNDS)
                    changeSounds();
                if (type == ChangeType.BLOCK_STATE)
                    changeBlockStates();
                if (type == ChangeType.LANG)
                    changeLang();
            }            
        }

        /**
         * Changes the namespace of all files (blockstates, models, sounds.json, lang).
         */
        public void changeAll() {
            changeLang();
            changeBlockStates();
            changeSounds();
            changeModels(ChangeType.ALL_MODELS);
        }


        public static class NamespaceCouple {
            private final String oldNamespace;
            private final String newNamespace;

            /**
             * Used to create a couple of namespaces that need to be changed.
             * @param oldNamespace The old namespace occurring in the resourcepack files.
             * @param newNamespace The new namespace that will replace the old one; also the name of the directory
             *                     inside the {@code assets} folder containing all the files (blockstates, models...).
             */
            public NamespaceCouple(String oldNamespace, String newNamespace) {
                this.oldNamespace = oldNamespace;
                this.newNamespace = newNamespace;
            }

            public String getOld() {
                return oldNamespace;
            }

            public String getNew() {
                return newNamespace;
            }
        }
    }


    //todo fix counter and code rework (rendere guardabile)
    public static class ObjModels {
        public static void patchBlock(Path modelsDirectory) {
            PL.logSpace();
            PL.logCentered("Patching models files", PL.line2, true, true);
            try {
                AtomicInteger totalModels = new AtomicInteger();
                AtomicInteger patchableModels = new AtomicInteger();
                AtomicInteger patched = new AtomicInteger();
                AtomicInteger failed = new AtomicInteger();
                Files.walk(modelsDirectory).forEach(path -> {
                    if (Files.isRegularFile(path)) {
                        totalModels.getAndIncrement();
                        try {
                            String modelAsString = Files.readString(path);
                            if (modelAsString.contains("\"loader\": \"forge:obj\",")) {
                                patchableModels.getAndIncrement();
                                modelAsString = modelAsString.replace("\"loader\": \"forge:obj\",", "\"loader\": \"neoforge:obj\",");
                                Files.writeString(path, modelAsString);
                                if (Files.readString(path).contains("\"loader\": \"neoforge:obj\",")) {
                                    PL.logI("Patched model '" + modelsDirectory.relativize(path) + "'");
                                    patched.getAndIncrement();
                                }
                            }
                        } catch (IOException e) {
                            failed.getAndIncrement();
                            PL.logE("Caught an exception during model file '\" + path + \"' read/write: " + e);
                        }
                    } else {
                        PL.logI("Path '" + modelsDirectory.relativize(path) + "' is a directory, skipping.");
                    }
                });
                PL.logCentered("Patching result", PL.line2, true, true);
                PL.logI("Total totalModels (all): " + totalModels.get());
                PL.logI("Patched: " + patched.get() + "/" + patchableModels.get());
                PL.logI("Failed: " + failed.get() + "/" + patchableModels.get());
                PL.logI(PL.line2);

            } catch (IOException e) {
                PL.logE("Caught an exception during model directory walk: " + e);
            }
        }

        public static void patchBlock(String resourcePackFolderInGameDir, String namespace) {
            ObjModels.patchBlock(GAME_DIR.resolve(resourcePackFolderInGameDir).resolve("assets").resolve(namespace).resolve("models/block"));
        }
    }
}
