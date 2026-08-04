package com.palm3.packs_loader.base;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.function.Supplier;

public class CacheFile {
/*
    private final Path cacheFile;

    public CacheFile(Path cacheFile) {
        this.cacheFile = cacheFile;
    }

    public void write() {

    }

    public String readString() {

    }*/


}

/*
* public class CreateRecipesGenerator {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static String RECIPES_DIR;
    private static String ITEM_APPLICATION_DIR;

    /**
     * Constructor of the recipe generation class.
     * @param namespace The namespace used for all the recipes generated with the class instance.
     * @param genRecipesInMainFolder If true, recipes get put in src/main/resources/data/namespace/recipes/..., if false in src/generated/resources/data/namespace/recipes/...
     * /
public CreateRecipesGenerator(String namespace, boolean genRecipesInMainFolder) {
    if (genRecipesInMainFolder) RECIPES_DIR = "src/main/resources/data/" + namespace + "/recipes/";
    else RECIPES_DIR = "src/generated/resources/data/" + namespace + "/recipes/";

    ITEM_APPLICATION_DIR = RECIPES_DIR + "item_application/";
}

// Class that represents a recipe entry, either an item or a tag.
public static class RecipeEntry {
    private static String I_NAME;
    private static String I_NAMESPACE;
    private static RecipeEntryTypes TYPE;

    /**
     * Constructor designed to be used with ResourceLocations
     * @param namespace The namespace of the entry.
     * @param name The entry itself.
     * @param type The type of the entry, either tag or item.
     * /
    public RecipeEntry(String namespace, String name, RecipeEntryTypes type) {
        I_NAMESPACE = namespace;
        I_NAME = name;
        TYPE = type;
    }

    /**
     * Constructor designed to be used with Items
     * @param item Supplier of the item used for the instance.
     * /
    public RecipeEntry(Supplier<Item> item) {
        I_NAMESPACE = Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(item.get())).getNamespace();
        I_NAME = Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(item.get())).getPath();
        TYPE = RecipeEntryTypes.TAG;
    }

    /**
     * Generates a recipe entry (an HashSet) with given item(s).
     * @param items The items to put in the HashSet.
     * @return HashSet of RecipeEntry ready to be used in recipe generation methods.
     * /
    public static HashSet<RecipeEntry> recipeEntry(Item... items) {
        HashSet<RecipeEntry> itemsSet = new HashSet<>();
        Arrays.stream(items).forEach(item -> itemsSet.add(new RecipeEntry(() -> item)));
        return itemsSet;
    }

    /**
     * Generates a recipe entry (an HashSet) with given tag key(s).
     * @param itemTags The tag keys to put in the HashSet.
     * @return HashSet of RecipeEntry ready to be used in recipe generation methods.
     * /
    @SafeVarargs
    public static HashSet<RecipeEntry> recipeEntry(TagKey<Item>... itemTags) {
        HashSet<RecipeEntry> itemTagsSet = new HashSet<>();
        Arrays.stream(itemTags).forEach(tag -> itemTagsSet.add(new RecipeEntry(tag.location().getNamespace(), tag.location().getPath(), RecipeEntryTypes.TAG)));
        return itemTagsSet;
    }

    private boolean isItem() {
        return TYPE.equals(RecipeEntryTypes.ITEM);
    }

    private boolean isTag() {
        return TYPE.equals(RecipeEntryTypes.TAG);
    }

    private String getName() {
        return I_NAME;
    }

    private String getNamespace() {
        return I_NAMESPACE;
    }

    private RecipeEntryTypes getType() {
        return TYPE;
    }

    private String getEntire() {
        return I_NAMESPACE + ":" + I_NAME;
    }
}

/**
 * Represents the types of entries for the recipes.
 * /
public enum RecipeEntryTypes {
    TAG,
    ITEM;
    RecipeEntryTypes() {}
}

private static JsonObject createJItem(String item) {
    JsonObject jItem = new JsonObject();
    jItem.addProperty("item", item);
    return jItem;
}

private static JsonObject createJTag(String tag) {
    JsonObject jTag = new JsonObject();
    jTag.addProperty("tag", tag);
    return jTag;
}

private static TagKey<Item>[] itemTags(String namespace, String... paths) {
    TagKey<Item>[] tags = new TagKey[paths.length];
    for (int i = 0; i < paths.length; i++) {
        tags[i] = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(namespace, paths[i]));
    }
    return tags;
}

/**
 * @param recipeJson The JsonObject of the recipe.
 * @param recipeDirectory The directory where to save the recipe.
 * @param saveName The name of the final recipe json file.
 * /
private static void saveRecipe(JsonObject recipeJson, String recipeDirectory, String saveName) {

    String effectiveDir = null;
    if (new File("").getAbsoluteFile().getName().equals("run-data")) {
        effectiveDir = "../" + recipeDirectory;  // Go up in main project directory and then in recipe directory from there.
    } else {
        LOGGER.info("Datagen not running, skipping recipe(s) saving.");
        //throw new IllegalStateException("Method saveRecipe() at CreateRecipesGenerator.java:38 has been called outside its working directory 'run-data'! Called in: " + new File("").getAbsoluteFile());
    }

    if (effectiveDir != null) {
        File effectiveDirAsFile = new File(effectiveDir);  // Final recipe file directory.

        // Creates directories for the recipe file if not existing.
        try {
            if (!Files.exists(effectiveDirAsFile.toPath())) {
                Files.createDirectories(effectiveDirAsFile.toPath());
                LOGGER.info("Final recipe directory '{}' doesn't exist, creating it.", effectiveDirAsFile);
            }
        } catch (IOException e) {
            LOGGER.error("An error occurred while generating recipe directory '{}'. Exception: {}", effectiveDirAsFile, String.valueOf(e));
        }

        // Saves recipe json file
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter(new File(effectiveDirAsFile, saveName + ".json"))) {
            gson.toJson(recipeJson, writer);  // Writes json
            LOGGER.info("Created casing item application recipe in file: " + saveName + ".json" + " located in " + new File(effectiveDir, saveName + ".json").getAbsolutePath());
        } catch (IOException e) {
            LOGGER.error("Error while writing recipe recipeJson file: {}", String.valueOf(e));
        }
    }
}


//====================================== CASING RECIPE ==========================================
/**
 * Creates a finished custom JSON recipe for a casing.
 * @param itemsToApply The item(s) to apply on the blocks in order to obtain the casing.
 * @param blocksOnWhichToApply An HashSet of blocks(s) (taken as items(s)) on which the items to apply can be applied to obtain the casing.
 * @param result The result of the recipe as ResourceLocation.
 * @return Finished JsonObject recipe, ready to be written.
 * /
@ParametersAreNonnullByDefault
protected static JsonObject genCasingJsonObjRecipe(HashSet<RecipeEntry> itemsToApply, HashSet<RecipeEntry> blocksOnWhichToApply, ResourceLocation result) {

    if (new File("").getAbsoluteFile().getName().equals("run-data")) {
        //----------------------------- Json file -------------------------------
        JsonObject recipe = new JsonObject();

        //-------------------- RECIPE TYPE----------------------
        recipe.addProperty("type", "create:item_application");
        //-------------------------------------------------------

        //-------------------- INGREDIENTS - 2 mains ----------------------
        JsonArray ingredients = new JsonArray();

        //----- Block(s) on which to apply the item(s) ------
        if (blocksOnWhichToApply.size() > 1) {
            JsonArray blocksOnWhichToApplyJ = new JsonArray();
            blocksOnWhichToApply.forEach(itemOfBlock -> {
                JsonObject itemOfBlockJ = new JsonObject();
                itemOfBlockJ.addProperty(itemOfBlock.getType().equals(RecipeEntryTypes.ITEM) ? "item" : "tag", itemOfBlock.getEntire());
                blocksOnWhichToApplyJ.add(itemOfBlockJ);
            });
            ingredients.add(blocksOnWhichToApplyJ);
        } else {
            JsonObject blockOnWhichToApplyJ = new JsonObject();
            blocksOnWhichToApply.forEach(itemOfBlock ->
                    blockOnWhichToApplyJ.addProperty(itemOfBlock.getType().equals(RecipeEntryTypes.ITEM) ? "item" : "tag", itemOfBlock.getEntire())
            );
            ingredients.add(blockOnWhichToApplyJ);
        }
        //---------------------------------------------------

        //----------- Required item(s) to apply -----------
        if (itemsToApply.size() > 1) {
            JsonArray itemsToApplyJ = new JsonArray();
            itemsToApply.forEach(item -> {
                JsonObject itemJ = new JsonObject();
                itemJ.addProperty(item.getType().equals(RecipeEntryTypes.ITEM) ? "item" : "tag", item.getEntire());
                itemsToApplyJ.add(itemJ);
            });
            ingredients.add(itemsToApplyJ);
        } else {
            JsonObject itemToApply = new JsonObject();
            itemsToApply.forEach(item ->
                    itemToApply.addProperty(item.getType().equals(RecipeEntryTypes.ITEM) ? "item" : "tag", item.getEntire())
            );
            ingredients.add(itemToApply);
        }
        //-----------------------------------------------

        recipe.add("ingredients", ingredients);
        //---------------------------------------------------------------

        //----------------------- RESULT ------------------------
        JsonArray results = new JsonArray();
        JsonObject resultOfRecipe = new JsonObject();
        resultOfRecipe.addProperty("item", result.toString());
        results.add(resultOfRecipe);
        recipe.add("results", results);
        //---------------------------------------------------------
        //-----------------------------------------------------------------------

        return recipe;
    } else {
        LOGGER.info("Datagen not running, skipping recipe(s) creation.");
        return null;
    }


}

/**
 * Generates a casing recipe.
 * @param itemsToApply HashSet of item(s) that can be applied (right-click) on the block(s) to obtain the casing.
 * @param blocksOnWhichToApply HashSet of block(s) (taken as item) on which the item(s) can be applied on.
 * @param result The result of the recipe as ResourceLocation.
 * /
@ParametersAreNonnullByDefault
public void customCasing(HashSet<RecipeEntry> itemsToApply, HashSet<RecipeEntry> blocksOnWhichToApply, ResourceLocation result) {
    JsonObject recipe = genCasingJsonObjRecipe(itemsToApply, blocksOnWhichToApply, result);
    saveRecipe(recipe, ITEM_APPLICATION_DIR, result.getPath());
}

/**
 * Generates a casing recipe.
 * <p>
 * You can use this in the onRegister() method of Registrate with the CasingBlock by creating a new CreateRecipesGenerator instance.
 * @param itemsToApply HashSet of item(s) that can be applied (right-click) on the block(s) to obtain the casing.
 * @param blocksOnWhichToApply HashSet of block(s) (taken as item) on which the item(s) can be applied on.
 * @param result The result of the recipe as an item.
 * /
@ParametersAreNonnullByDefault
public void customCasing(HashSet<RecipeEntry> itemsToApply, HashSet<RecipeEntry> blocksOnWhichToApply, Item result) {
    var resultItem = Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(result));
    customCasing(itemsToApply, blocksOnWhichToApply, ResourceLocation.fromNamespaceAndPath(resultItem.getNamespace(), resultItem.getPath()));
}

/**
 * Simplified method that generates a casing-from-wood recipe.
 * @param itemsToApply The item(s) to apply on the wood.
 * @param result The result item.
 * /
@ParametersAreNonnullByDefault
public void casingFromWood(HashSet<RecipeEntry> itemsToApply, Item result) {
    customCasing(itemsToApply, RecipeEntry.recipeEntry(itemTags("forge", "stripped_wood", "stripped_logs")), result);
}

/**
 * Simplified method that generates a casing-from-stone recipe.
 * @param itemsToApply The item(s) to apply on the stone.
 * @param result The result item.
 * /
@ParametersAreNonnullByDefault
public void casingFromStone(HashSet<RecipeEntry> itemsToApply, Item result) {
    HashSet<RecipeEntry> blocksOnWhichToApply = new HashSet<>();
    blocksOnWhichToApply.add(new RecipeEntry("forge", "stone", RecipeEntryTypes.TAG));
    blocksOnWhichToApply.add(new RecipeEntry("create", "cut_deepslate", RecipeEntryTypes.ITEM));
    blocksOnWhichToApply.add(new RecipeEntry("create", "polished_cut_deepslate", RecipeEntryTypes.ITEM));
    blocksOnWhichToApply.add(new RecipeEntry("create", "cut_deepslate_bricks", RecipeEntryTypes.ITEM));
    blocksOnWhichToApply.add(new RecipeEntry("create", "small_deepslate_bricks", RecipeEntryTypes.ITEM));
    blocksOnWhichToApply.add(new RecipeEntry("create", "layered_deepslate", RecipeEntryTypes.ITEM));
    blocksOnWhichToApply.add(new RecipeEntry("create", "deepslate_pillar", RecipeEntryTypes.ITEM));
    customCasing(itemsToApply, blocksOnWhichToApply, result);
}
}*/
