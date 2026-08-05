package com.palm3.packs_loader;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber
public class TitleScreenInfos {
    private static boolean toastShown = false;

    @SubscribeEvent
    public static void screenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof TitleScreen && !toastShown) {
            SystemToast.add(
                    Minecraft.getInstance().getToasts(),
                    SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                    Component.translatable("packs_loader.toast.main_menu.title").withColor(0x55FF55),
                    Component.translatable("packs_loader.toast.main_menu.message")
            );
            toastShown = true;
        }
    }
}
