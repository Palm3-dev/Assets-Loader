package com.palm3.assets_loader.assets.patchers;

import com.mojang.logging.LogUtils;
import com.palm3.assets_loader.LoaderMain;
import com.palm3.assets_loader.PrettyLogging;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * This class is used to patch {@code .obj} block models. If the {@code .json} model, referencing an {@code .obj}
 * model comes from a different loader, it has the json key {@code "loader"} with value {@code <loader>:obj}.
 * The wrong loader causes the model to fail the loading in-game.
 * <br>
 * Currently works only FORGE <--> NEOFORGE.
 */
public class ObjModelsChanger {

    private static final PrettyLogging PL = new PrettyLogging(LogUtils.getLogger(), LoaderMain.DEF_PL_PARAMS);
    private final Path modelsDirectory;
    private final Path blockModelsDirectory;
    private final Path itemModelsDirectory;
    private final Loader fromLoader;
    private final Loader toLoader;
    private final boolean doLog;

    protected ObjModelsChanger(Path packDirectory, String namespace, Loader fromLoader, Loader toLoader, boolean doLog) {
        modelsDirectory = packDirectory.resolve("assets").resolve(namespace).resolve("models");
        blockModelsDirectory = modelsDirectory.resolve("block");
        itemModelsDirectory = modelsDirectory.resolve("item");
        this.fromLoader = fromLoader;
        this.toLoader = toLoader;
        this.doLog = doLog;
    }

    private ObjModelsChanger(Path packDirectory, String namespace, Loader fromLoader, boolean doLog) {
        modelsDirectory = packDirectory.resolve("assets").resolve(namespace).resolve("models");
        blockModelsDirectory = modelsDirectory.resolve("block");
        itemModelsDirectory = modelsDirectory.resolve("item");
        this.fromLoader = fromLoader;
        this.toLoader = LoaderMain.MOD_LOADER;
        this.doLog = doLog;
    }

    /**
     * Create an instance of {@link ObjModelsChanger}, can specify the final loader.
     * @param packDirectory The root directory of the resourcepack (containing the assets folder, icon, etc.).
     * @param namespace The namespace (the folder) containing the model files. E.g. {@code pack_root/assets/my_namespace <-- this}.
     * @param fromLoader The loader that the models currently have, will be changed.
     * @param toLoader The new loader to change the models loader to.
     * @param doLog If the models being patched should be logged.
     */
    public static ObjModelsChanger createNew(Path packDirectory, String namespace, Loader fromLoader, Loader toLoader, boolean doLog) {
        return new ObjModelsChanger(packDirectory, namespace, fromLoader, toLoader, doLog);
    }

    /**
     * Create an instance of {@link ObjModelsChanger}. The final loader is the one of this mod.
     * @param packDirectory The root directory of the resourcepack (containing the assets folder, icon, etc.).
     * @param namespace The namespace (the folder) containing the model files. E.g. {@code pack_root/assets/my_namespace <-- this}.
     * @param fromLoader The loader that the models currently have, will be changed.
     * @param doLog If the models being patched should be logged.
     */
    public static ObjModelsChanger createNew(Path packDirectory, String namespace, Loader fromLoader, boolean doLog) {
        return new ObjModelsChanger(packDirectory, namespace, fromLoader, doLog);
    }

    // Self-explanatory tbh
    public enum Loader {
        FORGE("forge"),
        NEOFORGE("neoforge");

        final String name;

        Loader(String name) {
            this.name = name;
        }
    }

    protected static void changeObjModelLoader(Path modelsDirectory, Path modelFile, Loader fromLoader, Loader toLoader, boolean log) {
        if (Files.isRegularFile(modelFile)) {  // Stay safe
            try {
                if (log) PL.logI("Patching obj loader, model: '" + modelsDirectory.relativize(modelFile) + "'");
                String modelAsString = Files.readString(modelFile);
                if (modelAsString.contains("\"" + fromLoader.name + ":obj\"")) {
                    modelAsString = modelAsString.replace("\"" + fromLoader.name + ":obj\"", "\"" + toLoader.name + ":obj\"");
                    Files.writeString(modelFile, modelAsString);
                }
            } catch (IOException e) {
                PL.logE("Exception caught during model file " + modelsDirectory.relativize(modelFile) + " read/write: " + e);
            }
        }
    }

    public void changeModelsObjLoader(AssetType assetType) {
        if (!assetType.isModel()) {
            PL.logW("Tried to change obj models, but specified change is " + assetType + ", skipping.");
            return;
        } else {
            PL.logSpace();
            PL.logCentered("Patching " + fromLoader.name + " obj models loader", PL.line2, true, true);
        }

        AtomicInteger changedItemModels = new AtomicInteger();
        AtomicInteger changedBlockModels = new AtomicInteger();

        if (assetType.isBlockModel()) {
            try (Stream<Path> modelsStream = Files.walk(blockModelsDirectory)) {
                modelsStream
                        .filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".json"))
                        .forEach(modelFile -> {
                            changeObjModelLoader(modelsDirectory, modelFile, fromLoader, toLoader, doLog);
                            changedBlockModels.incrementAndGet();
                        });
            } catch (IOException e) {
                PL.logE("Exception caught during block models files walk: " + e);
            }
        }

        if (assetType.isItemModel()) {
            try (Stream<Path> modelsStream = Files.walk(itemModelsDirectory)) {
                modelsStream
                        .filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".json"))
                        .forEach(modelFile -> {
                            changeObjModelLoader(modelsDirectory, modelFile, fromLoader, toLoader, doLog);
                            changedItemModels.incrementAndGet();
                        });
            } catch (IOException e) {
                PL.logE("Exception caught during item models files walk: " + e);
            }
        }

        if (assetType.isModel()) {
            if (doLog) PL.logCentered("Loader patching result", PL.line2, true, true);
            PL.logI("Total models (json): " + (changedBlockModels.get() + changedItemModels.get()));
            PL.logI("Block models: " + changedBlockModels.get());
            PL.logI("Item models: " + changedItemModels.get());
            PL.logI(PL.line2);
        }
    }
}
