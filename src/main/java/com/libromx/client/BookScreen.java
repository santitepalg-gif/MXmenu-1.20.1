package com.libromx.client;

import com.libromx.data.Entry;
import com.libromx.net.EditPacket;
import com.libromx.net.Net;
import com.libromx.server.Actions;
import com.libromx.util.TextFmt;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** El libro: tres pestañas, lista a la izquierda, detalle (o editor de operador) a la derecha. */
public class BookScreen extends Screen {
    private static final int BW = 360;
    private static final int BH = 190;
    private static final int ROW_H = 13;
    private static final int ROWS = 11;
    private static final int INK = 0x2E1A09;
    private static final String[] NAMES = {"Misiones", "Notificaciones", "Dificultad"};
    private static final String[] HEADS = {"Diario de misiones", "Avisos del servidor", "Cambios de dificultad"};
    private static final String[] TYPE_KEYS = {"kill", "collect", "craft"};
    private static final String[] TYPE_LABELS = {"Matar entidad", "Obtener objeto", "Craftear objeto"};
    private static final String[] BASE_KEYS = {"easy", "normal", "hard", "peaceful"};
    private static final String[] BASE_LABELS = {"Fácil", "Normal", "Difícil", "Pacífico"};
    private static final int[] PAPEL = {0xFFA5322B, 0xFF3B8A2A, 0xFFE0A020, 0xFFE4007C, 0xFF1D9E9E};
    private static final int[] DOTS = {0xFF3B8A2A, 0xFFE0A020, 0xFFA5322B, 0xFF3A6FD0, 0xFFE4007C};

    private int book = 0;
    private int selected = 0;
    private int scroll = 0;
    private int typeIdx = 0;
    private int baseIdx = 1;
    private boolean editing = false;
    private boolean selectLast = false;
    private String status = "";
    private int bx;
    private int by;
    private int rx;
    private int rw;
    private EditBox titleBox;
    private EditBox descBox;
    private EditBox targetBox;
    private EditBox active;
    private final Map<String, LivingEntity> mobCache = new HashMap<>();

    public BookScreen() {
        super(Component.literal("Libro"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private List<Entry> list() {
        return ClientData.data.list(book);
    }

    private int sel() {
        List<Entry> l = list();
        if (l.isEmpty()) {
            return 0;
        }
        return Math.max(0, Math.min(selected, l.size() - 1));
    }

    /** Llega contenido nuevo del servidor. */
    public void onData() {
        if (selectLast) {
            selected = Math.max(0, list().size() - 1);
            scroll = Math.max(0, selected - ROWS + 1);
            selectLast = false;
        }
        if (!editing) {
            rebuildWidgets();
        }
    }

    @Override
    protected void init() {
        bx = (width - BW) / 2;
        by = Math.max(26, (height - BH) / 2 + 10);
        rx = bx + BW / 2 + 12;
        rw = BW / 2 - 24;
        titleBox = null;
        descBox = null;
        targetBox = null;

        int tw = 100;
        for (int i = 0; i < 3; i++) {
            final int b = i;
            Button t = Button.builder(Component.literal(NAMES[i]), btn -> {
                book = b;
                selected = 0;
                scroll = 0;
                editing = false;
                status = "";
                rebuildWidgets();
            }).bounds(bx + 30 + i * (tw + 6), by - 22, tw, 18).build();
            t.active = book != i;
            addRenderableWidget(t);
        }

        if (editing) {
            buildEditor();
        } else {
            buildDetailButtons();
        }
    }

    private void buildDetailButtons() {
        if (!ClientData.op) {
            return;
        }
        int y = by + BH - 24;
        boolean diff = book == 2;
        int n = diff ? 3 : 2;
        int w = (rw - (n - 1) * 3) / n;
        int x = rx;
        boolean has = !list().isEmpty();

        addRenderableWidget(Button.builder(Component.literal("Nueva"), b -> {
            editing = true;
            status = "";
            rebuildWidgets();
        }).bounds(x, y, w, 16).build());
        x += w + 3;

        Button rem = Button.builder(Component.literal("Quitar"), b ->
                Net.CH.sendToServer(new EditPacket(EditPacket.REMOVE, book, sel(), new Entry()))
        ).bounds(x, y, w, 16).build();
        rem.active = has;
        addRenderableWidget(rem);
        x += w + 3;

        if (diff) {
            Button apply = Button.builder(Component.literal("Aplicar"), b ->
                    Net.CH.sendToServer(new EditPacket(EditPacket.APPLY, 2, sel(), new Entry()))
            ).bounds(x, y, w, 16).build();
            apply.active = has;
            addRenderableWidget(apply);
        }
    }

    private void buildEditor() {
        int y = by + 28;
        titleBox = new EditBox(font, rx, y + 10, rw, 14, Component.literal("Título"));
        titleBox.setMaxLength(Actions.MAX);
        titleBox.setHint(Component.literal("&a&lMi título"));
        addRenderableWidget(titleBox);

        descBox = new EditBox(font, rx, y + 38, rw, 14, Component.literal("Descripción"));
        descBox.setMaxLength(Actions.MAX);
        descBox.setHint(Component.literal("Usa :corazon: para emojis"));
        addRenderableWidget(descBox);
        active = descBox;

        String[] fmtCodes = {"&l", "&o", "&n", "&r"};
        String[] fmtNames = {"N", "K", "S", "R"};
        for (int j = 0; j < 4; j++) {
            final String code = fmtCodes[j];
            addRenderableWidget(Button.builder(Component.literal(fmtNames[j]), b -> insert(code))
                    .bounds(rx + 92 + (j % 2) * 26, y + 56 + (j / 2) * 11, 24, 10).build());
        }

        if (book == 0) {
            Button typeBtn = Button.builder(Component.literal("Tipo: " + TYPE_LABELS[typeIdx]), b -> {
                typeIdx = (typeIdx + 1) % 3;
                b.setMessage(Component.literal("Tipo: " + TYPE_LABELS[typeIdx]));
            }).bounds(rx, y + 96, rw, 14).build();
            addRenderableWidget(typeBtn);

            targetBox = new EditBox(font, rx, y + 112, rw, 14, Component.literal("Objetivo"));
            targetBox.setMaxLength(100);
            targetBox.setHint(Component.literal("minecraft:creeper"));
            addRenderableWidget(targetBox);
        } else if (book == 2) {
            addRenderableWidget(Button.builder(Component.literal("Base: " + BASE_LABELS[baseIdx]), b -> {
                baseIdx = (baseIdx + 1) % 4;
                b.setMessage(Component.literal("Base: " + BASE_LABELS[baseIdx]));
            }).bounds(rx, y + 96, rw, 14).build());
        }

        int w = (rw - 4) / 2;
        addRenderableWidget(Button.builder(Component.literal("Agregar"), b -> submit())
                .bounds(rx, by + BH - 24, w, 16).build());
        addRenderableWidget(Button.builder(Component.literal("Cancelar"), b -> {
            editing = false;
            status = "";
            rebuildWidgets();
        }).bounds(rx + w + 4, by + BH - 24, w, 16).build());
    }

    private void submit() {
        String t = titleBox.getValue().trim();
        if (t.isEmpty()) {
            status = "Escribe un título";
            return;
        }
        String target = targetBox == null ? "" : targetBox.getValue().trim();
        Entry e = new Entry(t, descBox.getValue().trim(), TYPE_KEYS[typeIdx], target, BASE_KEYS[baseIdx]);
        Net.CH.sendToServer(new EditPacket(EditPacket.ADD, book, 0, e));
        selectLast = true;
        editing = false;
        status = "";
        rebuildWidgets();
    }

    private void insert(String s) {
        if (active == null) {
            active = descBox;
        }
        if (active != null) {
            active.insertText(s);
            setFocused(active);
        }
    }

    private boolean typing() {
        return editing && getFocused() instanceof EditBox eb && eb.isFocused();
    }

    private boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // ------------------------------------------------------------------ entrada

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (editing) {
            for (EditBox b : new EditBox[]{titleBox, descBox, targetBox}) {
                if (b != null && b.isMouseOver(mx, my)) {
                    active = b;
                }
            }
            int y = by + 28;
            for (int i = 0; i < 16; i++) {
                int sx = rx + (i % 8) * 11;
                int sy = y + 56 + (i / 8) * 11;
                if (inside(mx, my, sx, sy, 9, 9)) {
                    insert("&" + Integer.toHexString(i));
                    return true;
                }
            }
            int j = 0;
            for (String name : TextFmt.EMOJI.keySet()) {
                if (inside(mx, my, rx + j * 14 - 1, y + 79, 12, 12)) {
                    insert(":" + name + ":");
                    return true;
                }
                j++;
            }
        } else {
            int lx = bx + 14;
            int lw = BW / 2 - 26;
            for (int i = 0; i < ROWS; i++) {
                int idx = scroll + i;
                if (idx >= list().size()) {
                    break;
                }
                if (inside(mx, my, lx, by + 26 + i * ROW_H, lw, 12)) {
                    selected = idx;
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int max = Math.max(0, list().size() - ROWS);
        scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(delta)));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (editing) {
                editing = false;
                status = "";
                rebuildWidgets();
                return true;
            }
            onClose();
            return true;
        }
        if (ClientModEvents.OPEN.matches(keyCode, scanCode) && !typing()) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // ------------------------------------------------------------------ dibujo

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        drawBook(g);
        drawList(g, mx, my);
        if (editing) {
            drawEditor(g);
        } else {
            drawDetail(g, mx, my);
        }
        super.render(g, mx, my, pt);
    }

    private void drawBook(GuiGraphics g) {
        g.fill(bx - 5, by - 5, bx + BW + 5, by + BH + 5, 0xFF5A3A1C);
        g.fill(bx, by, bx + BW, by + BH, 0xFFE9D8A6);
        g.fill(bx + BW / 2 - 3, by, bx + BW / 2 + 3, by + BH, 0xFFA88A57);
        int n = 24;
        int w = BW / n;
        for (int i = 0; i < n; i++) {
            int x0 = bx + i * w;
            for (int r = 0; r < 7; r++) {
                g.fill(x0 + r, by + r, x0 + w - r, by + r + 1, PAPEL[i % PAPEL.length]);
            }
        }
    }

    private void drawList(GuiGraphics g, int mx, int my) {
        int lx = bx + 14;
        int lw = BW / 2 - 26;
        g.drawString(font, HEADS[book], lx, by + 11, INK, false);
        g.fill(lx, by + 22, lx + lw, by + 23, 0xFFA88A57);
        List<Entry> list = list();
        int s = sel();
        for (int i = 0; i < ROWS; i++) {
            int idx = scroll + i;
            if (idx >= list.size()) {
                break;
            }
            int ry = by + 26 + i * ROW_H;
            if (idx == s) {
                g.fill(lx, ry, lx + lw, ry + 12, 0xFFC9AD66);
            } else if (inside(mx, my, lx, ry, lw, 12)) {
                g.fill(lx, ry, lx + lw, ry + 12, 0xFFD8C48C);
            }
            g.fill(lx + 2, ry + 3, lx + 8, ry + 9, DOTS[idx % DOTS.length]);
            String name = TextFmt.strip(list.get(idx).title);
            g.drawString(font, font.plainSubstrByWidth(name, lw - 16), lx + 12, ry + 2, INK, false);
        }
        if (list.isEmpty()) {
            g.drawString(font, "El libro está vacío.", lx, by + 28, INK, false);
        }
    }

    private void drawDetail(GuiGraphics g, int mx, int my) {
        g.drawString(font, "Detalle", rx, by + 11, INK, false);
        g.fill(rx, by + 22, rx + rw, by + 23, 0xFFA88A57);
        List<Entry> list = list();
        if (list.isEmpty()) {
            if (!ClientData.op) {
                g.drawString(font, "Aún no hay nada aquí.", rx, by + 28, INK, false);
            }
            return;
        }
        Entry e = list.get(sel());
        int y = by + 27;
        y = tooltipBox(g, TextFmt.parse(e.title), y, 2);
        y = tooltipBox(g, TextFmt.parse(e.desc), y + 4, 5) + 5;
        if (book == 0) {
            drawQuestExtra(g, e, y, mx, my);
        } else if (book == 2) {
            g.drawString(font, "Actual: " + ClientData.data.currentDifficulty, rx, y, INK, false);
        }
    }

    private int tooltipBox(GuiGraphics g, Component c, int y, int maxLines) {
        List<FormattedCharSequence> lines = font.split(c, rw - 8);
        int n = Math.min(maxLines, Math.max(1, lines.size()));
        int h = n * 10 + 6;
        g.fill(rx, y, rx + rw, y + h, 0xF0100010);
        g.fill(rx, y, rx + rw, y + 1, 0xFF3B1D6E);
        g.fill(rx, y + h - 1, rx + rw, y + h, 0xFF3B1D6E);
        g.fill(rx, y, rx + 1, y + h, 0xFF3B1D6E);
        g.fill(rx + rw - 1, y, rx + rw, y + h, 0xFF3B1D6E);
        for (int i = 0; i < n && i < lines.size(); i++) {
            g.drawString(font, lines.get(i), rx + 4, y + 3 + i * 10, 0xFFFFFF, true);
        }
        return y + h;
    }

    private void drawQuestExtra(GuiGraphics g, Entry e, int y, int mx, int my) {
        int bottom = by + BH - 28;
        int h = bottom - y;
        if (h < 20) {
            return;
        }
        ResourceLocation id = ResourceLocation.tryParse(e.target);
        if ("kill".equals(e.type)) {
            g.fill(rx, y, rx + rw, y + h, 0xFF9CCBF0);
            g.fill(rx, y, rx + rw, y + 1, 0xFF3A3A3A);
            g.fill(rx, y + h - 1, rx + rw, y + h, 0xFFCFCFCF);
            LivingEntity le = mob(e.target);
            if (le != null) {
                float size = Math.max(0.5f, Math.max(le.getBbHeight(), le.getBbWidth()));
                int scale = (int) Math.max(6, Math.min(60, (h - 22) / size));
                int cx = rx + rw / 2;
                int cy = y + h - 4;
                InventoryScreen.renderEntityInInventoryFollowsMouse(g, cx, cy, scale,
                        (float) (cx - mx), (float) (cy - h / 2 - my), le);
            } else {
                g.drawCenteredString(font, "Sin vista previa", rx + rw / 2, y + h / 2 - 4, 0x1A1A1A);
            }
            g.drawString(font, font.plainSubstrByWidth("Matar: " + e.target, rw - 6), rx + 3, y + 3, 0x1A1A1A, false);
        } else {
            Item item = null;
            if (id != null && ForgeRegistries.ITEMS.containsKey(id)) {
                item = ForgeRegistries.ITEMS.getValue(id);
            }
            if (item != null) {
                g.renderItem(new ItemStack(item), rx, y);
            }
            String label = ("collect".equals(e.type) ? "Obtener: " : "Craftear: ") + e.target;
            g.drawString(font, font.plainSubstrByWidth(label, rw - 22), rx + 20, y + 4, INK, false);
        }
    }

    private LivingEntity mob(String id) {
        if (mobCache.containsKey(id)) {
            return mobCache.get(id);
        }
        LivingEntity res = null;
        try {
            ResourceLocation rl = ResourceLocation.tryParse(id);
            if (rl != null && ForgeRegistries.ENTITY_TYPES.containsKey(rl) && minecraft != null && minecraft.level != null) {
                Entity en = ForgeRegistries.ENTITY_TYPES.getValue(rl).create(minecraft.level);
                if (en instanceof LivingEntity le) {
                    res = le;
                }
            }
        } catch (Throwable t) {
            res = null;
        }
        mobCache.put(id, res);
        return res;
    }

    private void drawEditor(GuiGraphics g) {
        g.drawString(font, "Nueva entrada", rx, by + 11, INK, false);
        g.fill(rx, by + 22, rx + rw, by + 23, 0xFFA88A57);
        int y = by + 28;
        g.drawString(font, "Título", rx, y, INK, false);
        g.drawString(font, "Descripción", rx, y + 28, INK, false);
        for (int i = 0; i < 16; i++) {
            int sx = rx + (i % 8) * 11;
            int sy = y + 56 + (i / 8) * 11;
            Integer rgb = ChatFormatting.getById(i).getColor();
            g.fill(sx - 1, sy - 1, sx + 10, sy + 10, 0xFF333333);
            g.fill(sx, sy, sx + 9, sy + 9, 0xFF000000 | (rgb == null ? 0 : rgb));
        }
        int j = 0;
        for (String name : TextFmt.EMOJI.keySet()) {
            int ex = rx + j * 14;
            g.fill(ex - 1, y + 79, ex + 11, y + 91, 0xFF9A9A9A);
            g.drawString(font, TextFmt.glyph(name), ex + 1, y + 81, 0xFFFFFF, false);
            j++;
        }
        if (!status.isEmpty()) {
            g.drawString(font, status, rx, by + BH - 34, 0xFFA5322B, false);
        }
    }
}
