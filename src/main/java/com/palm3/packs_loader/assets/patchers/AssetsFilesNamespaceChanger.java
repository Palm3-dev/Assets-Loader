package com.palm3.packs_loader.assets.patchers;

import com.mojang.logging.LogUtils;
import com.palm3.packs_loader.PacksLoaderMain;
import com.palm3.packs_loader.PrettyLogging;
import com.palm3.packs_loader.assets.AssetsLoader;
import com.palm3.packs_loader.assets.NamespaceCouple;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * This class is used to patch the files inside a resourcepack that have been copied with {@link AssetsLoader}
 * by changing the assets folders namespace. The problem that results from this is that now the resourcepack has
 * the correct folder namespace inside the {@code assets} folder, but inside the files (blockstates, models, etc.) there's still the old namespace.
 * This class fixes this problem.
 *
 * <h3><a id=exampleUseScenario>Example use scenario:</a></h3>
 *     <p>
 *         We copied a pack from mod 'MinecraftMod' that had old namespace (original of the mod) {@code mc_mod} and
 *         we changed it to {@code new_mc}. The pack root folder is named {@code mc_mod_assets}.
 *         <br>
 *         <br>Now, the pack is structured like this:
 *
 *         <br>{@code mc_mod_assets/pack.png} if present
 *         <br>{@code mc_mod_assets/assets/new_mc} that before was {@code mc_mod}
 *         <br>
 *         <br>Inside this folder we found:
 *         <br>{@code blockstates}
 *         <br>  {@code lang}
 *         <br>  {@code models}
 *         <br>  {@code sounds}
 *         <br>  {@code textures}
 *         <br>  {@code sounds.json}
 *         <br>
 *         <br>
 *         We use this class to change every old namespace ({@code mc_mod}) occurrence in the files to the new {@code new_mc}.
 *         Therefore, this class needs to be used when the assets folder namespace is the new one ({@code mc_mod_assets/assets/new_mc})
 *         but the files of that namespace still have the old one, it doesn't change the namespace of an existing pack!
 *     </p>
 */
@ParametersAreNonnullByDefault
public class AssetsFilesNamespaceChanger {

    private static final PrettyLogging PL = new PrettyLogging(LogUtils.getLogger(), PacksLoaderMain.DEF_PL_PARAMS);
    private final @NotNull Path assetsDirectory;
    private final @NotNull List<NamespaceCouple> namespacesCouples;

    protected AssetsFilesNamespaceChanger(Path packDirectory, List<NamespaceCouple> namespacesCouples) {
        this.assetsDirectory = packDirectory.resolve("assets");
        this.namespacesCouples = namespacesCouples;
    }

    /**
     * Changes multiple namespaces in <b>one</b> pack.
     * <p>
     *     <b>NOTE:</b> Changes the files namespaces, the namespace folders inside the {@code assets} folder should already be the new ones.
     *     <br>
     *     This will change all the old namespaces in the <b>files</b> to the new ones for every given new namespace folder.
     *     So if whe have {@code my_pack_root/assets/newNamespace_A} and {@code my_pack_root/assets/newNamespace_B}
     *     this will change all the files old namespaces ({@code oldNamespace_A} and {@code oldNamespace_B})
     *     in {@code newNamespace_A} and {@code newNamespace_B} respectively.
     * </p>
     * @param packDirectory The directory containing the pack you want to modify the namespaces (the one with assets folder, the icon, etc.).
     * @param namespacesCouples A {@link List} of {@link NamespaceCouple} with the old and new namespaces couples.
     *                          To easily create one you can use {@link NamespaceCouple#createMultipleCouplesList(List, List)}
     * @return A new instance of {@link AssetsFilesNamespaceChanger}.
     */
    public static AssetsFilesNamespaceChanger multipleNamespaces(Path packDirectory, List<NamespaceCouple> namespacesCouples) {
        return new AssetsFilesNamespaceChanger(packDirectory, namespacesCouples);
    }

    /**
     * Changes one namespace in <b>one</b> pack.
     * <p>
     *     <b>NOTE:</b> Changes the files namespaces, the namespace folder inside the {@code assets} folder should already be the new one.
     *     <br>
     *     This will change the old namespace in the <b>files</b> to the new one.
     *     So if whe have {@code my_pack_root/assets/newNamespace}
     *     this will change all the files old namespaces ({@code oldNamespace}) in {@code newNamespace}.
     * </p>
     * @param packDirectory The directory containing the pack you want to modify the namespace  (the one with assets folder, the icon, etc.).
     * @param oldNamespace The old namespace to change.
     * @param newNamespace The new namespace that will replace the old one.
     * @return A new instance of {@link AssetsFilesNamespaceChanger}.
     */
    public static AssetsFilesNamespaceChanger singleNamespace(Path packDirectory, String oldNamespace, String newNamespace) {
        List<NamespaceCouple> namespacesCouples = new ArrayList<>();
        namespacesCouples.add(new NamespaceCouple(oldNamespace, newNamespace));
        return new AssetsFilesNamespaceChanger(packDirectory, namespacesCouples);
    }

    // Replaces all the old namespace occurrences in the given file with the new namespace.
    protected static void processFile(Path filePath, String oldNamespace, String newNamespace) {
        try {
            String fileAsString = Files.readString(filePath);
            if ((!oldNamespace.equals("forge") || (newNamespace.equals("neoforge") && !fileAsString.contains("neoforge"))) && fileAsString.contains(oldNamespace)) {
                PL.logI("Found file with old namespace: '" + filePath.getFileName() + "'.");
                fileAsString = fileAsString.replace(oldNamespace, newNamespace);
                Files.writeString(filePath, fileAsString);
            }
        } catch (IOException e) {
            PL.logE("Exception caught during file read/write: " + e);
        }
    }

    // Changes the namespace of all the files in the given dir. Extension can be a file or path e.g. my_cool/path/file.json and whole searched path will have to end with that.
    @SuppressWarnings("all")
    protected static void changeFilesNamespace(Path targetFilesDirectory, String oldNamespace, String newNamespace, String... fileExtensions) {
        try (Stream<Path> stream = Files.walk(targetFilesDirectory)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> {
                        if (fileExtensions.length == 0) return true;  // Any extension
                        for (String fileExtension : fileExtensions) {
                            if (path.toString().endsWith(fileExtension)) return true;  // Extension of current file is in filters array
                            //return false;  // Doesn't match  // I have the feeling this explodes things a bit. Doesn't check all if first doesn't match, right? Yhyh, 90% sure
                        }
                        return false;
                    })
                    .forEach(path -> processFile(path, oldNamespace, newNamespace));
        } catch (IOException e) {
            PL.logE("Exception caught during files walk: " + e);
        }
    }

    // Repeats given consumer for every existing couple of namespaces. Logs shit.
    private void repeatForCouples(Consumer<NamespaceCouple> consumer, AssetType fileTypeForLogs) {
        PL.logCenteredI("Changing " + fileTypeForLogs + " namespace", PL.line2);

        for (NamespaceCouple namespaceCouple : namespacesCouples) {
            consumer.accept(namespaceCouple);
        }

        PL.logCenteredI("Done", PL.line2);
        PL.logSpaceI();
    }

    /**
     * Changes the namespace(s) of the specified model types.
     * @param assetType The type of models to change the namespace.
     */
    public void changeModels(AssetType assetType) {
        repeatForCouples(namespaceCouple -> {
            Path modelsDir = assetsDirectory
                    // The new namespace is used in the directory (changed when copied), only the files have the old one.
                    .resolve(namespaceCouple.newOrSameNamespace())
                    .resolve("models");

            // Block models patch
            if (assetType.isBlockModel()) {
                changeFilesNamespace(
                        ModelsChanger.getBlockModelsDir(modelsDir),
                        namespaceCouple.oldNamespace(),
                        namespaceCouple.newOrSameNamespace(),
                        ".json"
                        );
            }
            // Item models patch
            if (assetType.isItemModel()) {
                changeFilesNamespace(
                        ModelsChanger.getItemModelsDir(modelsDir),
                        namespaceCouple.oldNamespace(),
                        namespaceCouple.newOrSameNamespace(),
                        ".json");
            }
            // Skipped, invalid
            if (!assetType.isModel()) {
                PL.logW("Tried to change models, but specified change is " + assetType + ", skipping.");
            }
        }, assetType);
    }

    /**
     * Changes the namespace(s) of the blockstates.
     */
    public void changeBlockStates() {
        repeatForCouples(namespaceCouple -> changeFilesNamespace(
                // The new namespace is used in the directory (changed when copied), only the files have the old one.
                assetsDirectory.resolve(namespaceCouple.newOrSameNamespace()).resolve("blockstates"),
                        namespaceCouple.oldNamespace(),
                        namespaceCouple.newOrSameNamespace(),
                ".json"),
                AssetType.BLOCK_STATE
        );
    }

    /**
     * Changes the namespace(s) of the lang files.
     */
    public void changeLang() {
        repeatForCouples(namespaceCouple -> changeFilesNamespace(
                // The new namespace is used in the directory (changed when copied), only the files have the old one.                    
                assetsDirectory.resolve(namespaceCouple.newOrSameNamespace()).resolve("lang"),
                        namespaceCouple.oldNamespace(),
                        namespaceCouple.newOrSameNamespace(),
                ".json"),
                AssetType.LANG
        );
    }

    /**
     * Changes the {@code sounds.json} file namespace(s).
     */
    public void changeSounds() {
        repeatForCouples(namespaceCouple -> changeFilesNamespace(
                assetsDirectory.resolve(namespaceCouple.newOrSameNamespace()),
                        namespaceCouple.oldNamespace(),
                        namespaceCouple.newOrSameNamespace(),
                        File.separator + "sounds.json"),
                AssetType.SOUNDS
        );
    }

    /**
     * Loads custom type of assets.
     * @param assetTypes The types of assets, use commas to separate {@link AssetType}.
     */
    public void customChanges(AssetType... assetTypes) {
        for (AssetType type : assetTypes) {
            if (type == AssetType.ALL_MODELS || type == AssetType.BLOCK_MODELS || type == AssetType.ITEM_MODELS)
                changeModels(type);
            if (type == AssetType.SOUNDS)
                changeSounds();
            if (type == AssetType.BLOCK_STATE)
                changeBlockStates();
            if (type == AssetType.LANG)
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
        changeModels(AssetType.ALL_MODELS);
    }
}
