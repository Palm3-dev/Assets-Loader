package com.palm3.assets_loader;

import com.mojang.logging.LogUtils;
import com.palm3.assets_loader.assets.*;
import com.palm3.assets_loader.assets.patchers.ModelsChanger;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import org.slf4j.Logger;

import java.util.List;

//todo remove jar files that don't load after first startup
//todo enable/disable assets patching
//todo move in main the introdutcion/start in asset loader
//todo add mod name for the one it's loading the assets to know who's using the classes
//todo individual obj loader

@Mod(LoaderMain.MOD_ID)
public class LoaderMain {
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final PrettyLogging.DefaultPrettyLoggingParams DEF_PL_PARAMS = new PrettyLogging.DefaultPrettyLoggingParams(LOGGER, "-", 83, "-", 41);

    // Mod infos
    public static final String MOD_ID = "assets_loader";
    public static final String MOD_VERSION = "1.0.0";
    public static final String DISCORD_LINK = "https://discord.com/invite/BuMv2f8epp";
    public static final ModelsChanger.Loader MOD_LOADER = ModelsChanger.Loader.NEOFORGE;

    public LoaderMain(IEventBus modEventBus) {
        modEventBus.addListener(LoaderMain::packFindersEvent);
        AssetsInGameTest.BLOCKS.register(modEventBus);
    }

    public static final AssetsLoader ASSET_LOADER = new AssetsLoader(".temp_assets", true, MOD_ID + "_mod_id");



    @SubscribeEvent
    public static void packFindersEvent(AddPackFindersEvent event) {
        //testing, praying it works, fingers crossed, even more than the ones i have by default
        // works, idk how i did it
        JarLoadingInfos jarLoadingInfos = new JarLoadingInfos(
                JarLoadingInfos.createCouplesList(
                        List.of("Create-Dreams-n-Desires-1.19.2-0.2.5b.PREBETA.jar", "design_decor-0.4.0b-1.20.1.jar"),
                        List.of(
                                List.of(new NamespaceCouple("create_dd", "create_dream_des_PREBETA"), new NamespaceCouple("create", "create_space_from_dream_des_PREBETA")),
                                List.of(new NamespaceCouple("design_decor", "new_design_decor"))
                        )

                ),
                new JarLoadingInfos.JarIconFile("Create-Dreams-n-Desires-1.19.2-0.2.5b.PREBETA.jar", "pack"),
                false,
                ModelsChanger.Loader.FORGE,
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

        AssetsLoader.Loaders.loadSinglePackMultiJarMultiNamespace(event, context);


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
