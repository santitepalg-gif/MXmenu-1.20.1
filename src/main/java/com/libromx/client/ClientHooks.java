package com.libromx.client;

import com.libromx.data.BookData;
import com.libromx.net.SyncPacket;
import net.minecraft.client.Minecraft;

public final class ClientHooks {
    private ClientHooks() {
    }

    public static void onSync(SyncPacket m) {
        ClientData.data = BookData.fromJson(m.json);
        ClientData.op = m.op;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof BookScreen bs) {
            bs.onData();
        } else if (m.open && mc.screen == null) {
            mc.setScreen(new BookScreen());
        }
    }
}
