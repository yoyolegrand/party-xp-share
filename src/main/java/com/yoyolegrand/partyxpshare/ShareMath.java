package com.yoyolegrand.partyxpshare;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.BiPredicate;

/**
 * Pure split logic (no Minecraft classes), so it can be unit-tested.
 * Takes the eligible participants of one kill and returns how many XP points each player gets.
 */
public final class ShareMath {

    private ShareMath() {}

    public enum Mode { DAMAGE, EQUAL, HYBRID }

    /** One player who hurt the mob (already filtered: online, alive, in range, contribution not expired). */
    public record Participant(UUID id, double damage, boolean killer) {}

    /** Settings resolved for this kill (global config, or Wave Survivor overrides for horde mobs). */
    public record Settings(Mode mode,
                           double killerBonusPercent,
                           double minContributionPercent,
                           double groupBonusPerPlayerPercent,
                           double groupBonusMaxPercent,
                           double supportSharePercent,
                           double teammateSharePercent) {
        /** Without the FTB Teams "nearby teammates" share. */
        public Settings(Mode mode, double killerBonusPercent, double minContributionPercent,
                        double groupBonusPerPlayerPercent, double groupBonusMaxPercent, double supportSharePercent) {
            this(mode, killerBonusPercent, minContributionPercent, groupBonusPerPlayerPercent, groupBonusMaxPercent,
                    supportSharePercent, 0);
        }
    }

    /** What one player receives. damagePercent is their share of the damage (0 for supporters and teammates). */
    public record Share(UUID id, int xp, double damagePercent, Kind kind) {}

    public enum Kind { ATTACKER, KILLER_BONUS_ONLY, SUPPORT, TEAMMATE }

    /**
     * @return the shares, or {@code null} when nothing should change (vanilla drop is kept):
     *         no damage recorded, or a single recipient.
     */
    public static List<Share> compute(int xp, List<Participant> participants, List<UUID> supporters,
                                      Settings s, Random rng) {
        return compute(xp, participants, supporters, List.of(), s, rng);
    }

    /**
     * @param teammates nearby FTB teammates of the reference player (FTB Teams "includeNearbyTeammates"), hitters included:
     *                  those who qualify by damage stay attackers, the others (no hit, or below minContribution) get
     *                  the teammate share. EQUAL: a full equal share each. DAMAGE / HYBRID: they split teammateSharePercent.
     *                  The minimum contribution never applies to them.
     */
    public static List<Share> compute(int xp, List<Participant> participants, List<UUID> supporters,
                                      List<UUID> teammates, Settings s, Random rng) {
        if (xp <= 0 || participants.isEmpty()) return null;

        double totalDamage = 0;
        for (Participant p : participants) totalDamage += Math.max(0, p.damage());
        if (totalDamage <= 0) return null;

        // 1. Who qualifies (damage share >= minimum). If nobody does, everybody does.
        List<Participant> qualified = new ArrayList<>();
        for (Participant p : participants) {
            if (p.damage() / totalDamage * 100.0 >= s.minContributionPercent()) qualified.add(p);
        }
        if (qualified.isEmpty()) qualified.addAll(participants);

        Participant killer = null;
        for (Participant p : participants) if (p.killer()) killer = p;
        boolean killerBonus = s.mode() == Mode.HYBRID && killer != null && s.killerBonusPercent() > 0;
        boolean killerOutside = killerBonus && !qualified.contains(killer);

        // Teammates who are not already attackers (no hit, or below the minimum contribution).
        List<UUID> mates = new ArrayList<>();
        for (UUID id : teammates) {
            boolean attacker = killerOutside && killer.id().equals(id);
            for (Participant p : qualified) if (p.id().equals(id)) attacker = true;
            if (!attacker && !mates.contains(id)) mates.add(id);
        }
        boolean equal = s.mode() == Mode.EQUAL;
        double matePart = mates.isEmpty() || equal ? 0 : clamp01(s.teammateSharePercent() / 100.0);
        int mateRecipients = equal ? mates.size() : (matePart > 0 ? mates.size() : 0);

        List<UUID> supportIds = new ArrayList<>();
        for (UUID id : supporters) if (!mates.contains(id)) supportIds.add(id);
        double supportPart = supportIds.isEmpty() ? 0 : clamp01(s.supportSharePercent() / 100.0);
        if (supportPart + matePart > 1) supportPart = 1 - matePart;

        int recipients = qualified.size() + (killerOutside ? 1 : 0) + mateRecipients + (supportPart > 0 ? supportIds.size() : 0);
        if (recipients <= 1) return null;   // only one person: keep the vanilla drop

        // 2. Group bonus (based on the number of attackers, teammates who share included).
        int attackers = qualified.size() + (killerOutside ? 1 : 0) + mateRecipients;
        double bonus = Math.min(Math.max(0, s.groupBonusMaxPercent()),
                Math.max(0, s.groupBonusPerPlayerPercent()) * (attackers - 1));
        double total = xp * (1.0 + bonus / 100.0);

        double supportPool = total * supportPart;
        double matePool = total * matePart;
        double attackPool = total - supportPool - matePool;

        // 3. Weights per mode.
        double qualifiedDamage = 0;
        for (Participant p : qualified) qualifiedDamage += Math.max(0, p.damage());

        Map<UUID, Double> raw = new LinkedHashMap<>();
        Map<UUID, Double> pct = new LinkedHashMap<>();
        Map<UUID, Kind> kind = new LinkedHashMap<>();

        double killerPart = killerBonus ? clamp01(s.killerBonusPercent() / 100.0) : 0;
        double restPool = attackPool * (1 - killerPart);

        int equalHeads = qualified.size() + (equal ? mates.size() : 0);
        for (Participant p : qualified) {
            double w = switch (s.mode()) {
                case EQUAL -> 1.0 / equalHeads;
                case DAMAGE, HYBRID -> qualifiedDamage > 0 ? Math.max(0, p.damage()) / qualifiedDamage : 1.0 / qualified.size();
            };
            raw.merge(p.id(), restPool * w, Double::sum);
            pct.put(p.id(), p.damage() / totalDamage * 100.0);
            kind.put(p.id(), Kind.ATTACKER);
        }
        if (killerBonus) {
            raw.merge(killer.id(), attackPool * killerPart, Double::sum);
            pct.putIfAbsent(killer.id(), killer.damage() / totalDamage * 100.0);
            kind.putIfAbsent(killer.id(), Kind.KILLER_BONUS_ONLY);
        }
        for (UUID id : mates) {
            if (raw.containsKey(id)) continue;
            double amount = equal ? restPool / equalHeads : matePool / mates.size();
            if (amount <= 0) continue;
            raw.put(id, amount);
            pct.put(id, 0.0);
            kind.put(id, Kind.TEAMMATE);
        }
        if (supportPool > 0) {
            for (UUID id : supportIds) {
                if (raw.containsKey(id)) continue;
                raw.put(id, supportPool / supportIds.size());
                pct.put(id, 0.0);
                kind.put(id, Kind.SUPPORT);
            }
        }

        // 4. Random rounding: 1.25 XP -> 1 (75 %) or 2 (25 %). Keeps the right average on small mobs.
        List<Share> out = new ArrayList<>();
        for (var e : raw.entrySet()) {
            int amount = randomRound(e.getValue(), rng);
            out.add(new Share(e.getKey(), amount, pct.get(e.getKey()), kind.get(e.getKey())));
        }
        return out;
    }

    /**
     * Team reference for FTB Teams TEAM_ONLY: the killer when it is a player,
     * otherwise the participant who dealt the most damage. Null when nobody qualifies.
     */
    public static UUID teamReference(List<Participant> participants, UUID killer) {
        if (killer != null) return killer;
        Participant best = null;
        for (Participant p : participants) if (best == null || p.damage() > best.damage()) best = p;
        return best == null ? null : best.id();
    }

    /** Nearby players in the same team as the reference player (the reference itself excluded). */
    public static List<UUID> nearbyTeammates(List<UUID> nearby, UUID reference, BiPredicate<UUID, UUID> sameTeam) {
        List<UUID> out = new ArrayList<>();
        if (reference == null) return out;
        for (UUID id : nearby) if (!id.equals(reference) && sameTeam.test(reference, id)) out.add(id);
        return out;
    }

    /** Keeps only the participants in the same team as the reference player (their damage no longer counts). */
    public static List<Participant> sameTeamOnly(List<Participant> participants, UUID reference,
                                                 BiPredicate<UUID, UUID> sameTeam) {
        List<Participant> out = new ArrayList<>();
        for (Participant p : participants) if (sameTeam.test(reference, p.id())) out.add(p);
        return out;
    }

    public static int randomRound(double value, Random rng) {
        if (value <= 0) return 0;
        double floor = Math.floor(value);
        return (int) floor + (rng.nextDouble() < value - floor ? 1 : 0);
    }

    private static double clamp01(double v) {
        return Math.max(0, Math.min(1, v));
    }
}
