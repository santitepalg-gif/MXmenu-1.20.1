package com.libromx.net;

import com.libromx.LibroMX;
import com.libromx.data.BookData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class Net {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CH = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(LibroMX.MODID, "main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private Net() {
    }

    public static void init() {
        int id = 0;
        CH.registerMessage(id++, RequestPacket.class, RequestPacket::encode, RequestPacket::decode, RequestPacket::handle);
        CH.registerMessage(id++, SyncPacket.class, SyncPacket::encode, SyncPacket::decode, SyncPacket::handle);
        CH.registerMessage(id++, EditPacket.class, EditPacket::encode, EditPacket::decode, EditPacket::handle);
    }

    /** Envía el contenido a un jugador. Con open=true, además le abre el libro. */
    public static void sync(ServerPlayer p, boolean open) {
        CH.send(PacketDistributor.PLAYER.with(() -> p),
                new SyncPacket(BookData.server().toJson(), p.hasPermissions(2), open));
    }

    public static void syncAll(MinecraftServer s) {
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            sync(p, false);
        }
    }
}
