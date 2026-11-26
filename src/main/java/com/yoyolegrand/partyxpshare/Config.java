package com.yoyolegrand.partyxpshare;

import com.yoyolegrand.partyxpshare.ShareMath.Mode;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

/**
 * config/partyxpshare-common.toml — reloaded automatically when the file is saved,
 * and editable in game with /partyxp set ...
 */
public final class Config {

    public enum Delivery { ORBS, DIRECT }
    public enum OrbMode { SPLIT, BONUS }
    public enum ModeOverride { GLOBAL, DAMAGE, EQUAL, HYBRID }
    public enum TeamMode { OFF, TEAM_ONLY }

    public static final ForgeConfigSpec SPEC;

    // general
    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.ConfigValue<String> LANGUAGE;
    public static final ForgeConfigSpec.BooleanValue SHOW_MESSAGES;

    // kill sharing
    public static final ForgeConfigSpec.BooleanValue KILL_ENABLED;
    public static final ForgeConfigSpec.EnumValue<Mode> MODE;
    public static final ForgeConfigSpec.DoubleValue KILLER_BONUS;
    public static final ForgeConfigSpec.DoubleValue MIN_CONTRIBUTION;
    public static final ForgeConfigSpec.IntValue SHARE_RADIUS;
    public static final ForgeConfigSpec.IntValue CONTRIBUTION_TIMEOUT;
    public static final ForgeConfigSpec.DoubleValue GROUP_BONUS_PER_PLAYER;
    public static final ForgeConfigSpec.DoubleValue GROUP_BONUS_MAX;
    public static final ForgeConfigSpec.BooleanValue PETS_COUNT;
    public static final ForgeConfigSpec.BooleanValue SUMMON_LOOKUP;
    public static final ForgeConfigSpec.BooleanValue CREDIT_INDIRECT;
    public static final ForgeConfigSpec.EnumValue<Delivery> DELIVERY;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BLACKLIST;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> WHITELIST;

    // orb sharing
    public static final ForgeConfigSpec.BooleanValue ORB_ENABLED;
    public static final ForgeConfigSpec.EnumValue<OrbMode> ORB_MODE;
    public static final ForgeConfigSpec.DoubleValue ORB_BONUS;
    public static final ForgeConfigSpec.IntValue ORB_RADIUS;

    // teams (FTB Teams)
    public static final ForgeConfigSpec.BooleanValue TEAMS_ENABLED;
    public static final ForgeConfigSpec.EnumValue<TeamMode> TEAM_MODE;

    // wave survivor
    public static final ForgeConfigSpec.BooleanValue WS_ENABLED;
    public static final ForgeConfigSpec.EnumValue<ModeOverride> WS_MODE;
    public static final ForgeConfigSpec.IntValue WS_RADIUS;
    public static final ForgeConfigSpec.DoubleValue WS_MIN_CONTRIBUTION;
    public static final ForgeConfigSpec.DoubleValue WS_SUPPORT_SHARE;
    public static final ForgeConfigSpec.DoubleValue WS_GROUP_BONUS_PER_PLAYER;
    public static final ForgeConfigSpec.BooleanValue WS_HIDE_MESSAGES;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.comment("Party XP Share — every option can also be changed in game: /partyxp set <option> <value>",
                "The file is reloaded automatically when you save it.").push("general");
        ENABLED = b.comment("Master switch for the whole mod.").define("enabled", true);
        LANGUAGE = b.comment("Language of the in-game messages: en or fr.")
                .define("language", "en", o -> o instanceof String s && (s.equals("en") || s.equals("fr")));
        SHOW_MESSAGES = b.comment("Show '+12 XP (35% damage)' in the action bar when a kill is shared.")
                .define("showMessages", true);
        b.pop();

        b.comment("Kill sharing: when a mob dies, its XP is split between the players who hurt it.").push("killSharing");
        KILL_ENABLED = b.define("enabled", true);
        MODE = b.comment("DAMAGE = split by share of damage dealt (20% damage -> 20% XP)",
                        "EQUAL  = same share for every participant (4 players -> 25% each)",
                        "HYBRID = the killer gets killerBonusPercent, the rest is split by damage")
                .defineEnum("mode", Mode.DAMAGE);
        KILLER_BONUS = b.comment("HYBRID only: % of the XP reserved for the player who lands the killing blow.")
                .defineInRange("killerBonusPercent", 30.0, 0.0, 100.0);
        MIN_CONTRIBUTION = b.comment("Players below this % of the total damage get nothing (stops 'one hit and wait' leeching).")
                .defineInRange("minContributionPercent", 5.0, 0.0, 100.0);
        SHARE_RADIUS = b.comment("Max distance (blocks) between a participant and the mob when it dies. 0 = no limit (same dimension).")
                .defineInRange("shareRadius", 32, 0, 1024);
        CONTRIBUTION_TIMEOUT = b.comment("A player's damage is forgotten if they have not hit the mob for this many seconds.")
                .defineInRange("contributionTimeoutSeconds", 30, 1, 3600);
        GROUP_BONUS_PER_PLAYER = b.comment("Extra XP (in %) per additional participant, to reward teamwork. 0 = total XP is never increased.")
                .defineInRange("groupBonusPerPlayerPercent", 0.0, 0.0, 100.0);
        GROUP_BONUS_MAX = b.comment("Cap of the group bonus (in %).")
                .defineInRange("groupBonusMaxPercent", 50.0, 0.0, 1000.0);
        PETS_COUNT = b.comment("Damage dealt by tamed pets / owned summons counts for their owner.")
                .define("petsCountForOwner", true);
        SUMMON_LOOKUP = b.comment("Summons from mods that are not 'ownable' (e.g. Iron's Spells): look for a public getSummoner() or getOwner()",
                        "method and credit the player it returns. Only used when petsCountForOwner is true.")
                .define("summonOwnerLookup", true);
        CREDIT_INDIRECT = b.comment("Damage with no attacker (fire, poison, wither...) counts for the last player who hit the mob.")
                .define("creditIndirectDamage", true);
        DELIVERY = b.comment("ORBS   = XP orbs appear on each player (Mending works as usual)",
                        "DIRECT = XP is added straight to the player (Mending is still applied)")
                .defineEnum("delivery", Config.Delivery.ORBS);
        BLACKLIST = b.comment("Mobs never shared. Entity ids or tags, e.g. [\"minecraft:wither\", \"#forge:bosses\"]")
                .defineListAllowEmpty("entityBlacklist", List.of(), o -> o instanceof String);
        WHITELIST = b.comment("If not empty, ONLY these mobs are shared. Entity ids or tags.")
                .defineListAllowEmpty("entityWhitelist", List.of(), o -> o instanceof String);
        b.pop();

        b.comment("Orb sharing: when a player picks up an XP orb, nearby players get part of it.",
                "Works for every XP source (mining, smelting, fishing...). Orbs from shared kills are never shared twice.").push("orbSharing");
        ORB_ENABLED = b.define("enabled", false);
        ORB_MODE = b.comment("SPLIT = the orb is split evenly between the picker and nearby players (no extra XP)",
                        "BONUS = the picker keeps everything, nearby players each get bonusPercent of it on top")
                .defineEnum("mode", OrbMode.SPLIT);
        ORB_BONUS = b.comment("BONUS only: % of the orb given to each nearby player.")
                .defineInRange("bonusPercent", 25.0, 0.0, 100.0);
        ORB_RADIUS = b.comment("Radius (blocks) around the picker.")
                .defineInRange("radius", 16, 1, 256);
        b.pop();

        b.comment("FTB Teams compatibility (only used when FTB Teams is installed).",
                "Applies to kill sharing, orb sharing and the Wave Survivor support share.").push("teams");
        TEAMS_ENABLED = b.define("enabled", true);
        TEAM_MODE = b.comment("OFF       = everybody who helped shares the XP (no team check)",
                        "TEAM_ONLY = only players in the same team as the killer share the XP; other players are ignored.",
                        "            A player without a team shares with nobody. If the killer is not a player (fire, arrow, pet...),",
                        "            the player who dealt the most damage is used as the reference.")
                .defineEnum("mode", TeamMode.TEAM_ONLY);
        b.pop();

        b.comment("Wave Survivor compatibility (only used when Wave Survivor is installed).",
                "These settings apply to horde mobs, bosses and Kingdom units.").push("waveSurvivor");
        WS_ENABLED = b.define("enabled", true);
        WS_MODE = b.comment("Mode for horde mobs. GLOBAL = same as killSharing.mode.")
                .defineEnum("mode", ModeOverride.GLOBAL);
        WS_RADIUS = b.comment("Share radius for horde mobs (arenas are big). 0 = no limit.")
                .defineInRange("shareRadius", 64, 0, 1024);
        WS_MIN_CONTRIBUTION = b.comment("Minimum contribution for horde mobs. -1 = same as killSharing.")
                .defineInRange("minContributionPercent", -1.0, -1.0, 100.0);
        WS_SUPPORT_SHARE = b.comment("% of a horde mob's XP split between nearby players who did NOT hit it",
                        "(builders, healers, players defending elsewhere in the arena). 0 = off.")
                .defineInRange("supportSharePercent", 10.0, 0.0, 100.0);
        WS_GROUP_BONUS_PER_PLAYER = b.comment("Group bonus per extra participant for horde mobs. -1 = same as killSharing.")
                .defineInRange("groupBonusPerPlayerPercent", -1.0, -1.0, 100.0);
        WS_HIDE_MESSAGES = b.comment("Hide the action bar messages for horde mobs (Wave Survivor already uses the action bar).")
                .define("hideMessagesDuringHorde", true);
        b.pop();

        SPEC = b.build();
    }

    private Config() {}

    public static boolean fr() {
        return "fr".equals(LANGUAGE.get());
    }
}
