package com.palm3.assets_loader;

import com.mojang.logging.LogUtils;
import com.palm3.assets_loader.assets.AssetsLoader;
import com.palm3.assets_loader.assets.patchers.ModelsChanger;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.ModLifecycleEvent;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import org.slf4j.Logger;

//todo remove jar files that don't load after first startup
//todo enable/disable assets patching
//todo move in main the introdutcion/start in asset loader
//todo add mod name for the one it's loading the assets to know who's using the classes

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

    public static final AssetsLoader assetsLoader = new AssetsLoader(".temp_assets");

    @SubscribeEvent
    public static void packFindersEvent(AddPackFindersEvent event) {


        assetsLoader.single.loadAssetsCustomFolderName(
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
