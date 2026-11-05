package com.libromx.net;

import com.libromx.client.ClientHooks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> cliente: contenido de los libros, si es operador y si debe abrir el libro. */
public class SyncPacket {
    public final String json;
    public final boolean op;
    public final boolean open;

    public SyncPacket(String json, boolean op, boolean open) {
        this.json = json;
        this.op = op;
        this.open = open;
    }

    public static void encode(SyncPacket m, FriendlyByteBuf b) {
        b.writeUtf(m.json, 1 << 20);
        b.writeBoolean(m.op);
        b.writeBoolean(m.open);
    }

    public static SyncPacket decode(FriendlyByteBuf b) {
        return new SyncPacket(b.readUtf(1 << 20), b.readBoolean(), b.readBoolean());
    }

    public static void handle(SyncPacket m, Supplier<NetworkEvent.Context> sup) {
        NetworkEvent.Context ctx = sup.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.onSync(m)));
        ctx.setPacketHandled(true);
    }
}
