package com.libromx.net;

import com.libromx.data.Entry;
import com.libromx.server.Actions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Cliente -> servidor: agregar, quitar o aplicar una entrada. El servidor revisa que sea operador. */
public class EditPacket {
    public static final int ADD = 0;
    public static final int REMOVE = 1;
    public static final int APPLY = 2;

    private final int action;
    private final int book;
    private final int index;
    private final Entry entry;

    public EditPacket(int action, int book, int index, Entry entry) {
        this.action = action;
        this.book = book;
        this.index = index;
        this.entry = entry;
    }

    public static void encode(EditPacket m, FriendlyByteBuf b) {
        b.writeByte(m.action);
        b.writeByte(m.book);
        b.writeVarInt(m.index);
        b.writeUtf(m.entry.title);
        b.writeUtf(m.entry.desc);
        b.writeUtf(m.entry.type);
        b.writeUtf(m.entry.target);
        b.writeUtf(m.entry.base);
    }

    public static EditPacket decode(FriendlyByteBuf b) {
        int action = b.readByte();
        int book = b.readByte();
        int index = b.readVarInt();
        Entry e = new Entry(b.readUtf(400), b.readUtf(400), b.readUtf(40), b.readUtf(200), b.readUtf(20));
        return new EditPacket(action, book, index, e);
    }

    public static void handle(EditPacket m, Supplier<NetworkEvent.Context> sup) {
        NetworkEvent.Context ctx = sup.get();
        ctx.enqueueWork(() -> {
            ServerPlayer p = ctx.getSender();
            if (p == null || !p.hasPermissions(2)) {
                return;
            }
            switch (m.action) {
                case ADD -> Actions.add(p.server, m.book, m.entry);
                case REMOVE -> Actions.remove(p.server, m.book, m.index);
                case APPLY -> {
                    if (m.book == 2) {
                        Actions.setDifficulty(p.server, m.index);
                    }
                }
                default -> {
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
