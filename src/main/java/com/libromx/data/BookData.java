package com.libromx.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.libromx.LibroMX;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Contenido de los tres libros. El servidor lo guarda en libromx.json dentro del mundo. */
public class BookData {
    public static final int MAX_PER_BOOK = 100;

    public List<Entry> quests = new ArrayList<>();
    public List<Entry> notices = new ArrayList<>();
    public List<Entry> difficulties = new ArrayList<>();
    public String currentDifficulty = "Normal";

    private static final Gson FILE_GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Gson NET_GSON = new Gson();
    private static BookData server = defaults();

    public static BookData server() {
        return server;
    }

    /** 0 = misiones, 1 = notificaciones, 2 = dificultad. */
    public List<Entry> list(int book) {
        return book == 0 ? quests : book == 1 ? notices : difficulties;
    }

    private BookData fixNulls() {
        if (quests == null) quests = new ArrayList<>();
        if (notices == null) notices = new ArrayList<>();
        if (difficulties == null) difficulties = new ArrayList<>();
        if (currentDifficulty == null) currentDifficulty = "Normal";
        return this;
    }

    public static BookData defaults() {
        BookData d = new BookData();
        d.quests.add(new Entry("&a&lCaza de creepers", "Elimina 5 creepers :calavera: y gana :diamante:", "kill", "minecraft:creeper", "normal"));
        d.quests.add(new Entry("&6Prepara tamales", "Consigue pan para los tamales de mole. :chile:", "collect", "minecraft:bread", "normal"));
        d.quests.add(new Entry("&cOfrenda de Día de Muertos", "Craftea un pastel para el altar. :cempasuchil: :corazon:", "craft", "minecraft:cake", "normal"));
        d.notices.add(new Entry("&b&lBienvenido", "¡Órale! Abre el libro con la tecla G. :estrella:", "kill", "", "normal"));
        d.notices.add(new Entry("&e&lLotería esta noche", "A las 8 pm en la plaza. &dHabrá premios :chile:", "kill", "", "normal"));
        d.difficulties.add(new Entry("&aFácil: Domingo en Coyoacán", "Mobs tranquilos y comida de sobra.", "kill", "", "easy"));
        d.difficulties.add(new Entry("&6Normal: Tianguis con prisa", "Equilibrio entre aventura y peligro.", "kill", "", "normal"));
        d.difficulties.add(new Entry("&c&lPesadilla: Noche de Chupacabras", "Mobs más fuertes. :calavera:", "kill", "", "hard"));
        return d;
    }

    private static Path path(MinecraftServer s) {
        return s.getWorldPath(LevelResource.ROOT).resolve("libromx.json");
    }

    public static void load(MinecraftServer s) {
        Path p = path(s);
        try {
            if (Files.exists(p)) {
                try (Reader r = Files.newBufferedReader(p)) {
                    BookData d = FILE_GSON.fromJson(r, BookData.class);
                    server = d != null ? d.fixNulls() : defaults();
                }
            } else {
                server = defaults();
                save(s);
            }
        } catch (Exception e) {
            LibroMX.LOGGER.error("No se pudo leer libromx.json, se usan los valores por defecto", e);
            server = defaults();
        }
    }

    public static void save(MinecraftServer s) {
        try {
            Files.writeString(path(s), FILE_GSON.toJson(server));
        } catch (IOException e) {
            LibroMX.LOGGER.error("No se pudo guardar libromx.json", e);
        }
    }

    public String toJson() {
        return NET_GSON.toJson(this);
    }

    public static BookData fromJson(String json) {
        try {
            BookData d = NET_GSON.fromJson(json, BookData.class);
            return d == null ? new BookData() : d.fixNulls();
        } catch (Exception e) {
            return new BookData();
        }
    }
}
