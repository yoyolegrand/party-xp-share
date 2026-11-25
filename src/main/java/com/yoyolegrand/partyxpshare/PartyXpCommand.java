package com.yoyolegrand.partyxpshare;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.yoyolegrand.partyxpshare.compat.WaveSurvivorCompat;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * /partyxp                         status (everyone)
 * /partyxp mode damage|equal|hybrid
 * /partyxp toggle                  master switch
 * /partyxp set <option> <value>    any option of the config file
 * /partyxp blacklist|whitelist add|remove|list <entity id or #tag>
 * Changes are saved to config/partyxpshare-common.toml right away.
 */
public final class PartyXpCommand {

    /** Short option names for /partyxp set. */
    private static final Map<String, ForgeConfigSpec.ConfigValue<?>> OPTIONS = new LinkedHashMap<>();

    static {
        OPTIONS.put("enabled", Config.ENABLED);
        OPTIONS.put("language", Config.LANGUAGE);
        OPTIONS.put("messages", Config.SHOW_MESSAGES);
        OPTIONS.put("killSharing", Config.KILL_ENABLED);
        OPTIONS.put("mode", Config.MODE);
        OPTIONS.put("killerBonus", Config.KILLER_BONUS);
        OPTIONS.put("minContribution", Config.MIN_CONTRIBUTION);
        OPTIONS.put("radius", Config.SHARE_RADIUS);
        OPTIONS.put("timeout", Config.CONTRIBUTION_TIMEOUT);
        OPTIONS.put("groupBonus", Config.GROUP_BONUS_PER_PLAYER);
        OPTIONS.put("groupBonusMax", Config.GROUP_BONUS_MAX);
        OPTIONS.put("pets", Config.PETS_COUNT);
        OPTIONS.put("indirectDamage", Config.CREDIT_INDIRECT);
        OPTIONS.put("delivery", Config.DELIVERY);
        OPTIONS.put("orbSharing", Config.ORB_ENABLED);
        OPTIONS.put("orbMode", Config.ORB_MODE);
        OPTIONS.put("orbBonus", Config.ORB_BONUS);
        OPTIONS.put("orbRadius", Config.ORB_RADIUS);
        OPTIONS.put("ws", Config.WS_ENABLED);
        OPTIONS.put("wsMode", Config.WS_MODE);
        OPTIONS.put("wsRadius", Config.WS_RADIUS);
        OPTIONS.put("wsMinContribution", Config.WS_MIN_CONTRIBUTION);
        OPTIONS.put("wsSupport", Config.WS_SUPPORT_SHARE);
        OPTIONS.put("wsGroupBonus", Config.WS_GROUP_BONUS_PER_PLAYER);
        OPTIONS.put("wsHideMessages", Config.WS_HIDE_MESSAGES);
    }

    private PartyXpCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        SuggestionProvider<CommandSourceStack> optionNames = (c, b) -> SharedSuggestionProvider.suggest(OPTIONS.keySet(), b);
        SuggestionProvider<CommandSourceStack> optionValues = (c, b) -> {
            var v = OPTIONS.get(StringArgumentType.getString(c, "option"));
            return SharedSuggestionProvider.suggest(v == null ? List.of() : suggestions(v), b);
        };

        d.register(Commands.literal("partyxp")
                .executes(PartyXpCommand::status)
                .then(Commands.literal("toggle").requires(s -> s.hasPermission(2))
                        .executes(c -> setAndReport(c, "enabled", Boolean.toString(!Config.ENABLED.get()))))
                .then(Commands.literal("mode").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("value", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(List.of("damage", "equal", "hybrid"), b))
                                .executes(c -> setAndReport(c, "mode", StringArgumentType.getString(c, "value")))))
                .then(Commands.literal("set").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("option", StringArgumentType.word()).suggests(optionNames)
                                .then(Commands.argument("value", StringArgumentType.word()).suggests(optionValues)
                                        .executes(c -> setAndReport(c, StringArgumentType.getString(c, "option"),
                                                StringArgumentType.getString(c, "value"))))))
                .then(listCommand("blacklist", Config.BLACKLIST))
                .then(listCommand("whitelist", Config.WHITELIST)));
    }

    // ─── status ───

    private static int status(CommandContext<CommandSourceStack> c) {
        boolean fr = Config.fr();
        CommandSourceStack s = c.getSource();
        s.sendSuccess(() -> Component.literal("§6§lParty XP Share §r§7— " + onOff(Config.ENABLED.get(), fr)), false);
        String mode = switch (Config.MODE.get()) {
            case DAMAGE -> fr ? "selon les dégâts" : "by damage";
            case EQUAL -> fr ? "part égale" : "equal shares";
            case HYBRID -> (fr ? "hybride (tueur " : "hybrid (killer ") + fmt(Config.KILLER_BONUS.get()) + "%)";
        };
        line(s, fr ? "Partage des kills" : "Kill sharing", onOff(Config.KILL_ENABLED.get(), fr) + " §7· §f" + mode);
        line(s, fr ? "Contribution mini" : "Min contribution", fmt(Config.MIN_CONTRIBUTION.get()) + "%");
        line(s, fr ? "Rayon / oubli" : "Radius / timeout", radius(Config.SHARE_RADIUS.get(), fr) + " §7· §f" + Config.CONTRIBUTION_TIMEOUT.get() + " s");
        line(s, fr ? "Bonus de groupe" : "Group bonus", "+" + fmt(Config.GROUP_BONUS_PER_PLAYER.get()) + "% / "
                + (fr ? "joueur (max " : "player (max ") + fmt(Config.GROUP_BONUS_MAX.get()) + "%)");
        line(s, fr ? "Livraison" : "Delivery", Config.DELIVERY.get().name());
        line(s, fr ? "Partage des orbes" : "Orb sharing", onOff(Config.ORB_ENABLED.get(), fr) + " §7· §f" + Config.ORB_MODE.get()
                + " §7· §f" + Config.ORB_RADIUS.get() + (fr ? " blocs" : " blocks"));
        if (WaveSurvivorCompat.isLoaded()) {
            String wsMode = Config.WS_MODE.get() == Config.ModeOverride.GLOBAL ? (fr ? "comme global" : "same as global") : Config.WS_MODE.get().name();
            line(s, "Wave Survivor", onOff(Config.WS_ENABLED.get(), fr) + " §7· §f" + wsMode + " §7· §f"
                    + radius(Config.WS_RADIUS.get(), fr) + " §7· §f" + (fr ? "soutien " : "support ") + fmt(Config.WS_SUPPORT_SHARE.get()) + "%");
        }
        if (s.hasPermission(2)) s.sendSuccess(() -> Component.literal(fr
                ? "§8/partyxp set <option> <valeur> · /partyxp mode <damage|equal|hybrid>"
                : "§8/partyxp set <option> <value> · /partyxp mode <damage|equal|hybrid>"), false);
        return 1;
    }

    private static void line(CommandSourceStack s, String label, String value) {
        s.sendSuccess(() -> Component.literal(" §7" + label + ": §f" + value), false);
    }

    private static String onOff(boolean b, boolean fr) {
        return b ? (fr ? "§aactivé" : "§aon") : (fr ? "§cdésactivé" : "§coff");
    }

    private static String radius(int r, boolean fr) {
        return r <= 0 ? (fr ? "illimité" : "unlimited") : r + (fr ? " blocs" : " blocks");
    }

    private static String fmt(double d) {
        return d == Math.rint(d) ? Long.toString((long) d) : Double.toString(d);
    }

    // ─── set ───

    private static int setAndReport(CommandContext<CommandSourceStack> c, String option, String raw) {
        boolean fr = Config.fr();
        ForgeConfigSpec.ConfigValue<?> value = OPTIONS.get(option);
        if (value == null) {
            c.getSource().sendFailure(Component.literal((fr ? "Option inconnue : " : "Unknown option: ") + option));
            return 0;
        }
        Object parsed = parse(value, raw);
        if (parsed == null || !valid(value, parsed)) {
            c.getSource().sendFailure(Component.literal((fr ? "Valeur invalide pour " : "Invalid value for ") + option + ": " + raw
                    + (suggestions(value).isEmpty() ? "" : " §7(" + String.join(", ", suggestions(value)) + ")")));
            return 0;
        }
        setRaw(value, parsed);
        boolean frNow = Config.fr();
        c.getSource().sendSuccess(() -> Component.literal("§6[Party XP] §f" + option + " §7= §a" + parsed
                + (frNow ? " §7(enregistré)" : " §7(saved)")), true);
        return 1;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void setRaw(ForgeConfigSpec.ConfigValue value, Object parsed) {
        value.set(parsed);
        value.save();
    }

    private static Object parse(ForgeConfigSpec.ConfigValue<?> value, String raw) {
        Object def = value.getDefault();
        try {
            if (def instanceof Boolean) {
                String r = raw.toLowerCase(Locale.ROOT);
                if (List.of("true", "on", "yes", "oui", "1").contains(r)) return true;
                if (List.of("false", "off", "no", "non", "0").contains(r)) return false;
                return null;
            }
            if (def instanceof Integer) return Integer.parseInt(raw);
            if (def instanceof Double) return Double.parseDouble(raw.replace(',', '.'));
            if (def instanceof Enum<?> e) {
                for (Object constant : e.getDeclaringClass().getEnumConstants()) {
                    if (((Enum<?>) constant).name().equalsIgnoreCase(raw)) return constant;
                }
                return null;
            }
            if (def instanceof String) return raw.toLowerCase(Locale.ROOT);
        } catch (NumberFormatException ignored) {
        }
        return null;
    }

    private static boolean valid(ForgeConfigSpec.ConfigValue<?> value, Object parsed) {
        Object spec = Config.SPEC.getSpec().get(value.getPath());
        return !(spec instanceof ForgeConfigSpec.ValueSpec vs) || vs.test(parsed);
    }

    private static List<String> suggestions(ForgeConfigSpec.ConfigValue<?> value) {
        Object def = value.getDefault();
        if (def instanceof Boolean) return List.of("true", "false");
        if (def instanceof Enum<?> e) {
            return Arrays.stream(e.getDeclaringClass().getEnumConstants()).map(x -> ((Enum<?>) x).name().toLowerCase(Locale.ROOT)).toList();
        }
        if (value == Config.LANGUAGE) return List.of("en", "fr");
        return List.of();
    }

    // ─── blacklist / whitelist ───

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> listCommand(
            String name, ForgeConfigSpec.ConfigValue<List<? extends String>> list) {
        return Commands.literal(name).requires(s -> s.hasPermission(2))
                .then(Commands.literal("list").executes(c -> {
                    List<? extends String> l = list.get();
                    c.getSource().sendSuccess(() -> Component.literal("§6[Party XP] §f" + name + ": §7"
                            + (l.isEmpty() ? (Config.fr() ? "(vide)" : "(empty)") : String.join(", ", l))), false);
                    return 1;
                }))
                .then(Commands.literal("add").then(Commands.argument("entry", StringArgumentType.greedyString())
                        .executes(c -> editList(c, name, list, StringArgumentType.getString(c, "entry").trim(), true))))
                .then(Commands.literal("remove").then(Commands.argument("entry", StringArgumentType.greedyString())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(list.get().stream().map(String::valueOf), b))
                        .executes(c -> editList(c, name, list, StringArgumentType.getString(c, "entry").trim(), false))));
    }

    private static int editList(CommandContext<CommandSourceStack> c, String name,
                                ForgeConfigSpec.ConfigValue<List<? extends String>> list, String entry, boolean add) {
        List<String> l = new ArrayList<>();
        for (String s : list.get()) l.add(s);
        boolean changed = add ? (!l.contains(entry) && l.add(entry)) : l.remove(entry);
        if (changed) {
            list.set(l);
            list.save();
        }
        boolean fr = Config.fr();
        String verb = add ? (fr ? "ajouté à" : "added to") : (fr ? "retiré de" : "removed from");
        c.getSource().sendSuccess(() -> Component.literal("§6[Party XP] §f" + entry + " §7"
                + (changed ? verb + " " + name : (fr ? "(aucun changement)" : "(no change)"))), true);
        return changed ? 1 : 0;
    }
}
