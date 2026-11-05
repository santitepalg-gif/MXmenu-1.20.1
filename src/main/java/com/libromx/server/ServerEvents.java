package com.libromx.server;

import com.libromx.LibroMX;
import com.libromx.data.BookData;
import com.libromx.net.Net;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LibroMX.MODID)
public final class ServerEvents {
    private ServerEvents() {
    }

    @SubscribeEvent
    public static void onStarting(ServerStartingEvent e) {
        BookData.load(e.getServer());
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent e) {
        BookData.save(e.getServer());
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent e) {
        BookCommands.register(e.getDispatcher());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            Net.sync(p, false);
        }
    }
}
