package com.palm3.assets_loader;

import com.mojang.logging.LogUtils;
import com.palm3.assets_loader.assets.AssetsLoader;
import com.palm3.assets_loader.assets.AssetsPatcher;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import org.slf4j.Logger;

import java.util.List;

//todo fix Files.walk() not using try, remove concatenation of / in paths, use resolve
//todo remove jar files that don't load after first startup
//todo log decently
//todo enable/disable assets patching

@Mod(LoaderMain.MOD_ID)
public class LoaderMain {
    public static final String MOD_ID = "assets_loader";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final PrettyLogging.DefaultPrettyLoggingParams DEF_PL_PARAMS = new PrettyLogging.DefaultPrettyLoggingParams(LOGGER, "-", 83, "-", 41);
    public static final PrettyLogging PL = new PrettyLogging(LOGGER, DEF_PL_PARAMS);

    public static final String MOD_VERSION = "1.0.0";
    public static final String DISCORD_LINK = "https://discord.com/invite/BuMv2f8epp";

    public LoaderMain(IEventBus modEventBus) {
        modEventBus.addListener(LoaderMain::packFindersEvent);
        AssetsInGameTest.BLOCKS.register(modEventBus);
    }

    @SubscribeEvent
    public static void packFindersEvent(AddPackFindersEvent event) {
        AssetsLoader assetsLoader = new AssetsLoader(".temp_assets");

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
                false
        );

        assetsLoader.multiple.loadAssets(
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

        );
    }
}
