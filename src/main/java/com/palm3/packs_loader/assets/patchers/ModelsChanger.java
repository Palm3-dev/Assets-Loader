package com.palm3.packs_loader.assets.patchers;

import com.mojang.logging.LogUtils;
import com.palm3.packs_loader.PacksLoaderMain;
import com.palm3.packs_loader.ModLoader;
import com.palm3.packs_loader.logging.PrettyLogging;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * This utility class offers various statics to patch minecraft models coming from different loaders or versions.
 * <br>It offers an obj model changer method for the loader difference.
 * <br>It also offers the method {@link ModelsChanger#changeJsonModelString(Path, String, String, QuotationMarkPos)}
 * that can be used to make custom changes to specific models not covered here.
 */
public class ModelsChanger {

    private static final PrettyLogging PL = new PrettyLogging(LogUtils.getLogger(), PacksLoaderMain.DEF_PL_PARAMS);

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
     * @return {@code true} if the model has been changed successfully, {@code false} if the model hasn't been changed, exceptions happened or others.
     *         You can ignore the return value most of the time.
     */
    public static boolean changeJsonModelString(Path modelFile, String oldString, String newString, QuotationMarkPos quotationMarksPos) {
        boolean modelsChanged = false;
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
                    modelsChanged = true;
                }
            } catch (IOException e) {
                PL.logE("Exception caught during json file " + modelFile.getFileName() + " read/write: " + e);
            }
        } else {
            PL.logI("Tried to change a json string of a non-json file: '" + modelFile.getFileName() + "'");
        }
        return modelsChanged;
    }

    /**
     * Record holds a couple of loaders used to define from which loader the models come from and to which loader change them to.
     * @param fromLoader The old models loader.
     * @param toLoader The new models loader.
     */
    public record LoaderCouple(ModLoader fromLoader, ModLoader toLoader) {}

    /**
     * Used to patch {@code .obj} block models. If the {@code .json} model, referencing an {@code .obj}
     * model comes from a different loader, it has the json key {@code "loader"} with value {@code <loader>:obj}.
     * The wrong loader causes the model to fail the loading in-game. This is used to fix that.
     * @param assetType The type of model you want to change (see {@link AssetType}).
     *                  If the type is not a model you'll get a WARN with an {@link IllegalArgumentException} (without crashes).
     * @param packRoot The root of the resourcepack (folder containing the icon, the assets folder, etc...).
     * @param loaders The loaders of the pack, old and new one. See {@link LoaderCouple}.
     * @param namespaces A list of namespaces where to patch the models.
     */
    public static void changeModelsObjLoader(AssetType assetType, Path packRoot, LoaderCouple loaders, List<String> namespaces) {
        if (!assetType.isModel()) {
            PL.logW(new IllegalArgumentException("Tried to change obj models, but specified change is " + assetType + ", skipping.").toString());
            return;
        }

        PL.logCenteredI("Patching " + loaders.fromLoader + " obj models loader", PL.line1);

        namespaces.forEach(namespace -> {
            PL.logI("Models namespace: '" + namespace + "'");

            AtomicInteger totalItemModels = new AtomicInteger();
            AtomicInteger totalBlockModels = new AtomicInteger();
            AtomicInteger changedItemModels = new AtomicInteger();
            AtomicInteger changedBlockModels = new AtomicInteger();

            Path modelsPath = getModelsDir(packRoot, namespace);

            if (assetType.isBlockModel()) {
                try (Stream<Path> modelsStream = Files.walk(getBlockModelsDir(modelsPath))) {
                    modelsStream
                            .filter(Files::isRegularFile)
                            .filter(path -> path.toString().endsWith(".json"))
                            .forEach(modelFile -> {
                                if (changeJsonModelString(modelFile, loaders.fromLoader.name + ":obj", loaders.toLoader.name + ":obj", QuotationMarkPos.BOTH))
                                    changedBlockModels.incrementAndGet();
                                totalBlockModels.incrementAndGet();
                            });
                } catch (IOException e) {
                    PL.logE("Exception caught during block models files walk: " + e);
                }
            }

            if (assetType.isItemModel()) {
                try (Stream<Path> modelsStream = Files.walk(getItemModelsDir(modelsPath))) {
                    modelsStream
                            .filter(Files::isRegularFile)
                            .filter(path -> path.toString().endsWith(".json"))
                            .forEach(modelFile -> {
                                if (changeJsonModelString(modelFile, loaders.fromLoader.name + ":obj", loaders.toLoader.name + ":obj", QuotationMarkPos.BOTH))
                                    changedItemModels.incrementAndGet();
                                totalItemModels.incrementAndGet();
                            });
                } catch (IOException e) {
                    PL.logE("Exception caught during item models files walk: " + e);
                }
            }

            PL.logCenteredI("Loader patching result", PL.line2);
            PL.logI("Total models: " + (totalItemModels.get() + totalBlockModels.get()));
            PL.logI("Total block models: " + totalBlockModels.get());
            PL.logI("Total item models: " + totalItemModels.get());
            PL.logI("Patched block models: " + changedBlockModels.get());
            PL.logI("Patched item models: " + changedItemModels.get());
            PL.logI(PL.line2);
        });
    }

    /**
     * Gets you the models directory.
     * @param packRoot The root of your resourcepack.
     * @param namespace The namespace of the models inside the resourcepack.
     * @return The {@link Path} of the models directory.
     */
    public static Path getModelsDir(Path packRoot, String namespace) {
        return packRoot.resolve("assets").resolve(namespace).resolve("models");
    }

    /**
     * Gets you the item models directory.
     * @param modelsPath The models directory in your pack namespace.
     * @return The {@link Path} of the item models directory.
     */
    public static Path getItemModelsDir(Path modelsPath) {
        return modelsPath.resolve("item");
    }

    /**
     * Gets you the block models directory.
     * @param modelsPath The models directory in your pack namespace.
     * @return The {@link Path} of the block models directory.
     */
    public static Path getBlockModelsDir(Path modelsPath) {
        return modelsPath.resolve("block");
    }
}
