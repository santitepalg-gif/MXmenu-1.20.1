package com.libromx.server;

import com.libromx.data.BookData;
import com.libromx.data.Entry;
import com.libromx.net.Net;
import com.libromx.util.TextFmt;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;

import java.util.List;
import java.util.Locale;

/** Operaciones del servidor sobre los libros. Las usan los comandos y los paquetes del panel. */
public final class Actions {
    public static final int MAX = 300;
    private static final List<String> TYPES = List.of("kill", "collect", "craft");
    private static final List<String> BASES = List.of("peaceful", "easy", "normal", "hard");

    private Actions() {
    }

    public static String clip(String s) {
        if (s == null) return "";
        s = s.trim();
        return s.length() > MAX ? s.substring(0, MAX) : s;
    }

    private static Entry clean(Entry in) {
        Entry e = new Entry();
        e.title = clip(in.title);
        e.desc = clip(in.desc);
        e.type = TYPES.contains(in.type) ? in.type : "kill";
        ResourceLocation rl = ResourceLocation.tryParse(clip(in.target).toLowerCase(Locale.ROOT));
        e.target = rl != null ? rl.toString() : "minecraft:zombie";
        e.base = BASES.contains(in.base) ? in.base : "normal";
        return e;
    }

    public static boolean add(MinecraftServer s, int book, Entry in) {
        if (book < 0 || book > 2 || in == null) return false;
        Entry e = clean(in);
        List<Entry> list = BookData.server().list(book);
        if (e.title.isEmpty() || list.size() >= BookData.MAX_PER_BOOK) return false;
        list.add(e);
        if (book == 1) {
            s.getPlayerList().broadcastSystemMessage(
                    Component.literal("[Aviso] ").withStyle(ChatFormatting.GOLD)
                            .append(TextFmt.parse(e.title)).append(Component.literal(" "))
                            .append(TextFmt.parse(e.desc)), false);
        }
        done(s);
        return true;
    }

    public static boolean remove(MinecraftServer s, int book, int idx) {
        if (book < 0 || book > 2) return false;
        List<Entry> list = BookData.server().list(book);
        if (idx < 0 || idx >= list.size()) return false;
        list.remove(idx);
        done(s);
        return true;
    }

    public static boolean setDifficulty(MinecraftServer s, int idx) {
        List<Entry> list = BookData.server().difficulties;
        if (idx < 0 || idx >= list.size()) return false;
        Entry e = list.get(idx);
        Difficulty d;
        switch (e.base) {
            case "peaceful" -> d = Difficulty.PEACEFUL;
            case "easy" -> d = Difficulty.EASY;
            case "hard" -> d = Difficulty.HARD;
            default -> d = Difficulty.NORMAL;
        }
        s.setDifficulty(d, true);
        BookData.server().currentDifficulty = TextFmt.strip(e.title);
        s.getPlayerList().broadcastSystemMessage(
                Component.literal("Dificultad: ").withStyle(ChatFormatting.GOLD).append(TextFmt.parse(e.title)), false);
        done(s);
        return true;
    }

    private static void done(MinecraftServer s) {
        BookData.save(s);
        Net.syncAll(s);
    }
}
