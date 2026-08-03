package com.palm3.assets_loader.assets.patchers;

import com.mojang.logging.LogUtils;
import com.palm3.assets_loader.LoaderMain;
import com.palm3.assets_loader.ModLoader;
import com.palm3.assets_loader.PrettyLogging;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * This class is used to patch minecraft models coming from different loaders or versions.
 * <br>It offers an obj model changer method for the loader difference.
 * <br>It also offers a static method {@link ModelsChanger#changeJsonModelString(Path, String, String, QuotationMarkPos)}
 * that can be used to make custom changes to specific models not covered here.
 */
public class ModelsChanger {

    private static final PrettyLogging PL = new PrettyLogging(LogUtils.getLogger(), LoaderMain.DEF_PL_PARAMS);
    private final Path modelsDirectory;
    private final ModLoader fromLoader;
    private final ModLoader toLoader;

    protected ModelsChanger(Path packDirectory, String namespace, ModLoader fromLoader, ModLoader toLoader) {
        modelsDirectory = packDirectory.resolve("assets").resolve(namespace).resolve("models");
        this.fromLoader = fromLoader;
        this.toLoader = toLoader;
    }

    /**
     * Create an instance of {@link ModelsChanger}, can specify the final loader.
     * @param packDirectory The root directory of the resourcepack (containing the assets folder, icon, etc.).
     * @param namespace The namespace (the folder) containing the model files. E.g. {@code pack_root/assets/my_namespace <-- this}.
     * @param fromLoader The loader that the models currently have, will be changed.
     * @param toLoader The new loader to change the models loader to.
     */
    public static ModelsChanger createNew(Path packDirectory, String namespace, ModLoader fromLoader, ModLoader toLoader) {
        return new ModelsChanger(packDirectory, namespace, fromLoader, toLoader);
    }

    /**
     * Create an instance of {@link ModelsChanger}. The final loader is the one of this mod.
     * @param packDirectory The root directory of the resourcepack (containing the assets folder, icon, etc.).
     * @param namespace The namespace (the folder) containing the model files. E.g. {@code pack_root/assets/my_namespace <-- this}.
     * @param fromLoader The loader that the models currently have, will be changed.
     */
    public static ModelsChanger createNew(Path packDirectory, String namespace, ModLoader fromLoader) {
        return new ModelsChanger(packDirectory, namespace, fromLoader, LoaderMain.MOD_LOADER);
    }

    /// @return The {@link Path} of the item models directory.
    public Path getItemModelsDir() {
        return modelsDirectory.resolve("item");
    }

    /// @return The {@link Path} of the block models directory.
    public Path getBlockModelsDir() {
        return modelsDirectory.resolve("block");
    }

    /// Indicates the position of the quotation marks in a json string.
    public enum QuotationMarkPos {
        BEFORE,
        AFTER,
        BOTH,
        NONE;

        QuotationMarkPos() {}

        public boolean isBefore() {
            return EnumSet.of(BEFORE, BOTH).contains(this);
        }

        public boolean isAfter() {
            return EnumSet.of(AFTER, BOTH).contains(this);
        }
    }

    /**
     * Changes the given string in a json file to the new one. Filters to only json files.
     * @param modelFile The model file you want to change.
     * @param oldString The old string.
     * @param newString The new string that will replace the old one.
     * @param quotationMarksPos The position of the quotation marks in the strings, can be disabled with {@link QuotationMarkPos#NONE}.
     */
    public static void changeJsonModelString(Path modelFile, String oldString, String newString, QuotationMarkPos quotationMarksPos) {
        if (Files.isRegularFile(modelFile) && modelFile.toString().endsWith(".json")) {
            try {
                String oldJsonStr = oldString;
                String newJsonStr = newString;
                if (quotationMarksPos != QuotationMarkPos.NONE && quotationMarksPos.isBefore()) {
                    oldJsonStr = "\"" + oldJsonStr;
                    newJsonStr = "\"" + newJsonStr;
                }
                if (quotationMarksPos != QuotationMarkPos.NONE && quotationMarksPos.isAfter()) {
                    oldJsonStr = oldJsonStr + "\"";
                    newJsonStr = newJsonStr + "\"";
                }

                String jsonAsString = Files.readString(modelFile);
                if (jsonAsString.contains(oldJsonStr)) {
                    PL.logI("Changing json value " + oldJsonStr + " to " + newJsonStr + ", model file: " + modelFile.getFileName());
                    jsonAsString = jsonAsString.replace(oldJsonStr, newJsonStr);
                    Files.writeString(modelFile, jsonAsString);
                }
            } catch (IOException e) {
                PL.logE("Exception caught during json file " + modelFile.getFileName() + " read/write: " + e);
            }
        } else {
            PL.logI("Tried to change a json string of a non-json file: '" + modelFile.getFileName() + "'");
        }
    }

    /**
     * Used to patch {@code .obj} block models. If the {@code .json} model, referencing an {@code .obj}
     * model comes from a different loader, it has the json key {@code "loader"} with value {@code <loader>:obj}.
     * The wrong loader causes the model to fail the loading in-game. This is used to fix that.
     * @param assetType The type of model (all, item, block).
     */
    public void changeModelsObjLoader(AssetType assetType) {
        if (!assetType.isModel()) {
            PL.logW("Tried to change obj models, but specified change is " + assetType + ", skipping.");
            return;
        }

        PL.logSpaceI();
        PL.logCenteredI("Patching " + fromLoader.name + " obj models loader", PL.line2, true, true);

        AtomicInteger itemModels = new AtomicInteger();
        AtomicInteger blockModels = new AtomicInteger();

        if (assetType.isBlockModel()) {
            try (Stream<Path> modelsStream = Files.walk(getBlockModelsDir())) {
                modelsStream
                        .filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".json"))
                        .forEach(modelFile -> {
                            changeJsonModelString(modelFile, fromLoader.name + ":obj", toLoader.name + ":obj", QuotationMarkPos.BOTH);
                            blockModels.incrementAndGet();
                        });
            } catch (IOException e) {
                PL.logE("Exception caught during block models files walk: " + e);
            }
        }

        if (assetType.isItemModel()) {
            try (Stream<Path> modelsStream = Files.walk(getItemModelsDir())) {
                modelsStream
                        .filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".json"))
                        .forEach(modelFile -> {
                            changeJsonModelString(modelFile, fromLoader.name + ":obj", toLoader.name + ":obj", QuotationMarkPos.BOTH);
                            itemModels.incrementAndGet();
                        });
            } catch (IOException e) {
                PL.logE("Exception caught during item models files walk: " + e);
            }
        }

        PL.logCenteredI("Loader patching result", PL.line2, true, true);
        PL.logI("Total models (json): " + (blockModels.get() + itemModels.get()));
        PL.logI("Block models: " + blockModels.get());
        PL.logI("Item models: " + itemModels.get());
        PL.logI(PL.line2);
    }
}
