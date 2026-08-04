package com.palm3.packs_loader;

import com.mojang.logging.LogUtils;
import com.palm3.packs_loader.assets.*;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import org.slf4j.Logger;

import java.util.List;

import static com.palm3.packs_loader.PrettyLogging.DEF_EMPTY_LINE;

@Mod(PacksLoaderMain.MOD_ID)
public class PacksLoaderMain {
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final PrettyLogging.DefaultPrettyLoggingParams DEF_PL_PARAMS = new PrettyLogging.DefaultPrettyLoggingParams(LOGGER, "-", 83, "-", 41);
    public static final PrettyLogging MAIN_PL = new PrettyLogging(DEF_PL_PARAMS);

    // Mod infos
    public static final String MOD_ID = "assets_loader";
    public static final String MOD_VERSION = "1.0.0";
    public static final String DISCORD_LINK = "https://discord.com/invite/BuMv2f8epp";
    public static final ModLoader MOD_LOADER = ModLoader.NEOFORGE;
    private static boolean initialPackLoad = true;

    /**
     * This method is used to distinguish the {@link AddPackFindersEvent} firing time.
     * <p>
     *     <h3>Event firing times:</h3>
     *     The first time the event gets called is in the mod lifecycle (when all the mods get loaded).
     *     This is the moment where the loaders <b>need</b> to copy and create the resourcepacks.
     *     The event gets also called outside the {@link net.neoforged.fml.event.IModBusEvent} (and the mod lifecycle) every time
     *     some changes to the resourcepacks are made in-game. To avoid logging unwanted stuff and most importantly avoid
     *     coping all the resourcepack files every time changes are made in-game (completely not necessary, and you also get several disc writes)
     *     this method is used in various other methods.
     *     <h3>Usage and value changes:</h3>
     *     The value returned by this method is always {@code true} during the mod lifecycle (mod loading phase).
     *     The returned value changes one time only in the mod lifecycle, when the {@link FMLCommonSetupEvent} gets fired.
     *     Here the value ({@code true} by default) gets changed to {@code false} for the entire time the game is loaded.
     *     You can then easly understand the usage of this method:
     *     <pre>
     *         {@code
     *         if (LoaderMain.isInitialPackLoad()) {
     *             // Do here the coping stuff for your mod methods.
     *         }
     *         }
     *     </pre>
     * </p>
     */
    public static boolean isInitialPackLoad() {
        return initialPackLoad;
    }

    public PacksLoaderMain(IEventBus modEventBus) {
        // Logs mod info
        MAIN_PL.logLineI(false);
        MAIN_PL.logCenteredI("External pack loader by Palm3", DEF_EMPTY_LINE);
        MAIN_PL.logCenteredI("Loads assets and data in-game directly from a mod jar", DEF_EMPTY_LINE);
        MAIN_PL.logCenteredI("Mod version: " + MOD_VERSION + "    Discord: " + DISCORD_LINK, DEF_EMPTY_LINE);
        MAIN_PL.logLineI(false);

        // Do actual stuff
        modEventBus.addListener(PacksLoaderMain::packFindersEvent);
    }

    @SubscribeEvent
    public static void commonSetup(FMLCommonSetupEvent event) {
        initialPackLoad = false;
    }









    //todo remove jar files that don't load after first startup
    //todo enable/disable assets patching
    //todo individual obj loader
    //todo re-add patchers


    public static final AssetsLoader ASSET_LOADER = new AssetsLoader(".temp_assets", true, MOD_ID + "_mod_id");

    @SubscribeEvent
    public static void packFindersEvent(AddPackFindersEvent event) {
        //testing, praying it works, fingers crossed, even more than the ones i have by default
        // works, idk how i did it - 15:33
        // 19:38 - doesn't work anymore -> god, fu**, dammit (holy cit here)
        // 19:39 - k just a little mistake, initialPacksLoad shall be set after the loops
        JarLoadingInfos jarLoadingInfos = new JarLoadingInfos(
                JarLoadingInfos.createJarNamespacesMap(
                        List.of("Create-Dreams-n-Desires-1.19.2-0.2.5b.PREBETA.jar", "design_decor-0.4.0b-1.20.1.jar"),
                        List.of(
                                List.of(new NamespaceCouple("create_dd", "create_dream_des_prebeta"), new NamespaceCouple("create", "create_space_from_dream_des_prebeta")),
                                List.of(new NamespaceCouple("design_decor", "new_design_decor"))
                        )
                ),
                new JarLoadingInfos.JarIconFile("Create-Dreams-n-Desires-1.19.2-0.2.5b.PREBETA.jar", "pack"),
                false,
                AssetsLoader.LogCopyOption.ALWAYS_LOG
        );

        ResourcePackInfos packInfos = new ResourcePackInfos(
                "ultimate_shitpack",
                Component.literal("ulti-title"),
                Component.literal("ulti-description"),
                true,
                false
        );

        PackLoadingContext context = new PackLoadingContext(ASSET_LOADER, jarLoadingInfos, packInfos, true);

        AssetsLoader.loadPack(event, context);


        /*assetsLoader.single.loadAssetsCustomFolderName(
                event,
                "design_decor-0.4.0b-1.20.1.jar",
                "design_decor",
                "create_dd",
                "logo",
                "design_decor_mod_extracted_assets",
                Component.literal("title"),
                Component.literal("description"),
                false,
                true,
                true
        );

        /*assetsLoader.multiple.loadAssets(
                event,
                List.of("Create-Dreams-n-Desires-1.19.2-0.2.5b.PREBETA.jar", "design_decor-0.4.0b-1.20.1.jar"),
                List.of("create_dd", "design_decor"),
                List.of("dream_des", "design_and_d"),
                "logo",
                "des_dec_and_dream_des_pack",
                Component.literal("multiple"),
                Component.literal("descrpt mult"),
                false,
                false,
                false

        );*/
    }
}
