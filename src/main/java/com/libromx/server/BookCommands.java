package com.libromx.server;

import com.libromx.data.BookData;
import com.libromx.data.Entry;
import com.libromx.util.TextFmt;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Comandos /libro ... solo para operadores (nivel 2 o más). */
public final class BookCommands {
    private static final List<String> TYPES = List.of("kill", "collect", "craft");
    private static final List<String> BASES = List.of("peaceful", "easy", "normal", "hard");

    private BookCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("libro")
                .requires(src -> src.hasPermission(2));

        root.then(Commands.literal("mision")
                .then(Commands.literal("agregar")
                        .then(Commands.argument("tipo", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(TYPES, b))
                                .then(Commands.argument("objetivo", ResourceLocationArgument.id())
                                        .then(Commands.argument("titulo", StringArgumentType.string())
                                                .then(Commands.argument("descripcion", StringArgumentType.greedyString())
                                                        .executes(BookCommands::addQuest))))))
                .then(removeNode(0)));

        root.then(Commands.literal("notificacion")
                .then(Commands.literal("enviar")
                        .then(Commands.argument("titulo", StringArgumentType.string())
                                .then(Commands.argument("texto", StringArgumentType.greedyString())
                                        .executes(BookCommands::addNotice))))
                .then(removeNode(1)));

        root.then(Commands.literal("dificultad")
                .then(Commands.literal("agregar")
                        .then(Commands.argument("base", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(BASES, b))
                                .then(Commands.argument("titulo", StringArgumentType.string())
                                        .then(Commands.argument("descripcion", StringArgumentType.greedyString())
                                                .executes(BookCommands::addDifficulty)))))
                .then(Commands.literal("establecer")
                        .then(Commands.argument("indice", IntegerArgumentType.integer(1))
                                .executes(c -> {
                                    int idx = IntegerArgumentType.getInteger(c, "indice") - 1;
                                    if (!Actions.setDifficulty(c.getSource().getServer(), idx)) {
                                        c.getSource().sendFailure(Component.literal("Ese número no existe en el libro de dificultad"));
                                        return 0;
                                    }
                                    return 1;
                                })))
                .then(removeNode(2)));

        root.then(Commands.literal("lista")
                .then(listNode("mision", 0))
                .then(listNode("notificacion", 1))
                .then(listNode("dificultad", 2)));

        d.register(root);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> removeNode(int book) {
        return Commands.literal("quitar")
                .then(Commands.argument("indice", IntegerArgumentType.integer(1))
                        .executes(c -> {
                            int idx = IntegerArgumentType.getInteger(c, "indice") - 1;
                            if (!Actions.remove(c.getSource().getServer(), book, idx)) {
                                c.getSource().sendFailure(Component.literal("Ese número no existe en el libro"));
                                return 0;
                            }
                            c.getSource().sendSuccess(() -> Component.literal("Entrada quitada"), true);
                            return 1;
                        }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> listNode(String name, int book) {
        return Commands.literal(name).executes(c -> {
            List<Entry> list = BookData.server().list(book);
            if (list.isEmpty()) {
                c.getSource().sendSuccess(() -> Component.literal("El libro está vacío"), false);
            }
            for (int i = 0; i < list.size(); i++) {
                final int n = i + 1;
                final Entry e = list.get(i);
                c.getSource().sendSuccess(() -> Component.literal(n + ". ").append(TextFmt.parse(e.title)), false);
            }
            return list.size();
        });
    }

    private static int addQuest(CommandContext<CommandSourceStack> c) {
        String tipo = StringArgumentType.getString(c, "tipo");
        if (!TYPES.contains(tipo)) {
            c.getSource().sendFailure(Component.literal("Tipo inválido. Usa kill, collect o craft"));
            return 0;
        }
        ResourceLocation id = ResourceLocationArgument.getId(c, "objetivo");
        Entry e = new Entry(StringArgumentType.getString(c, "titulo"),
                StringArgumentType.getString(c, "descripcion"), tipo, id.toString(), "normal");
        return finish(c, Actions.add(c.getSource().getServer(), 0, e), "Misión agregada");
    }

    private static int addNotice(CommandContext<CommandSourceStack> c) {
        Entry e = new Entry(StringArgumentType.getString(c, "titulo"),
                StringArgumentType.getString(c, "texto"), "kill", "", "normal");
        return finish(c, Actions.add(c.getSource().getServer(), 1, e), "Notificación enviada");
    }

    private static int addDifficulty(CommandContext<CommandSourceStack> c) {
        String base = StringArgumentType.getString(c, "base");
        if (!BASES.contains(base)) {
            c.getSource().sendFailure(Component.literal("Base inválida. Usa peaceful, easy, normal o hard"));
            return 0;
        }
        Entry e = new Entry(StringArgumentType.getString(c, "titulo"),
                StringArgumentType.getString(c, "descripcion"), "kill", "", base);
        return finish(c, Actions.add(c.getSource().getServer(), 2, e), "Dificultad agregada");
    }

    private static int finish(CommandContext<CommandSourceStack> c, boolean ok, String msg) {
        if (!ok) {
            c.getSource().sendFailure(Component.literal("No se pudo agregar (título vacío o libro lleno)"));
            return 0;
        }
        c.getSource().sendSuccess(() -> Component.literal(msg), true);
        return 1;
    }
}
