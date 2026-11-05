package com.libromx.util;

import com.libromx.LibroMX;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Convierte texto con códigos a componentes de Minecraft.
 * Colores: &0-&f, hex &#RRGGBB. Formatos: &l negrita, &o cursiva, &n subrayado, &m tachado, &r limpiar.
 * Emojis: :corazon: :estrella: :calavera: :diamante: :chile: :cempasuchil:
 */
public final class TextFmt {
    public static final ResourceLocation EMOJI_FONT = new ResourceLocation(LibroMX.MODID, "emoji");
    /** Para agregar emojis: añade aquí el nombre y su carácter, y el dibujo en emoji.png / emoji.json. */
    public static final Map<String, Character> EMOJI = new LinkedHashMap<>();

    static {
        EMOJI.put("corazon", '\uE000');
        EMOJI.put("estrella", '\uE001');
        EMOJI.put("calavera", '\uE002');
        EMOJI.put("diamante", '\uE003');
        EMOJI.put("chile", '\uE004');
        EMOJI.put("cempasuchil", '\uE005');
    }

    private TextFmt() {
    }

    public static Component glyph(String name) {
        return Component.literal(String.valueOf(EMOJI.get(name))).withStyle(Style.EMPTY.withFont(EMOJI_FONT));
    }

    public static MutableComponent parse(String s) {
        MutableComponent root = Component.empty();
        if (s == null) return root;
        StringBuilder buf = new StringBuilder();
        Style st = Style.EMPTY;
        int i = 0;
        int n = s.length();
        while (i < n) {
            char c = s.charAt(i);
            if (c == '&' && i + 1 < n) {
                char k = Character.toLowerCase(s.charAt(i + 1));
                if (k == '#' && i + 8 <= n && s.substring(i + 2, i + 8).matches("[0-9a-fA-F]{6}")) {
                    flush(root, buf, st);
                    st = Style.EMPTY.withColor(TextColor.fromRgb(Integer.parseInt(s.substring(i + 2, i + 8), 16)));
                    i += 8;
                    continue;
                }
                ChatFormatting f = ChatFormatting.getByCode(k);
                if (f != null) {
                    flush(root, buf, st);
                    if (f == ChatFormatting.RESET) {
                        st = Style.EMPTY;
                    } else if (f.isFormat()) {
                        st = st.applyFormat(f);
                    } else {
                        st = Style.EMPTY.withColor(f);
                    }
                    i += 2;
                    continue;
                }
            } else if (c == ':') {
                int j = s.indexOf(':', i + 1);
                if (j > i + 1) {
                    String name = s.substring(i + 1, j);
                    if (EMOJI.containsKey(name)) {
                        flush(root, buf, st);
                        root.append(glyph(name));
                        i = j + 1;
                        continue;
                    }
                }
            }
            buf.append(c);
            i++;
        }
        flush(root, buf, st);
        return root;
    }

    private static void flush(MutableComponent root, StringBuilder buf, Style st) {
        if (buf.length() > 0) {
            root.append(Component.literal(buf.toString()).withStyle(st));
            buf.setLength(0);
        }
    }

    /** Texto sin códigos de color ni emojis. */
    public static String strip(String s) {
        if (s == null) return "";
        String r = s.replaceAll("&#[0-9a-fA-F]{6}", "").replaceAll("&[0-9a-fk-orA-FK-OR]", "");
        for (String k : EMOJI.keySet()) {
            r = r.replace(":" + k + ":", "");
        }
        return r.trim();
    }
}
