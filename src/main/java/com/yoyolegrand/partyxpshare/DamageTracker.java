package com.yoyolegrand.partyxpshare;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Remembers who hurt which mob, and how much.
 * Cost: one map lookup + one addition per hit. Nothing runs every tick except a cleanup every 30 s.
 */
public final class DamageTracker {

    /** Damage of one player on one mob. */
    public static final class Contribution {
        public double damage;
        public long lastHitTick;
    }

    private static final Map<UUID, Map<UUID, Contribution>> RECORDS = new HashMap<>();
    private static long tick;

    public DamageTracker() {}

    public static long now() { return tick; }

    /** Removes and returns the record of a mob (called when it drops its XP). */
    public static Map<UUID, Contribution> take(LivingEntity mob) {
        return RECORDS.remove(mob.getUUID());
    }

    public static int trackedCount() { return RECORDS.size(); }

    public static void clear() {
        RECORDS.clear();
        SummonOwners.clear();
    }

    // LOWEST so we read the final amount after every other mod (armor, buffs, Wave Survivor blessings...).
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDamage(LivingDamageEvent event) {
        if (!Config.ENABLED.get() || !Config.KILL_ENABLED.get()) return;
        LivingEntity mob = event.getEntity();
        if (mob.level().isClientSide || mob instanceof Player) return;

        ServerPlayer player = attacker(mob, event.getSource());
        if (player == null) return;

        // Cap to remaining health: a 500 damage hit on a 2 HP mob only counts as 2.
        float amount = Math.min(event.getAmount(), mob.getHealth());
        if (amount <= 0) return;

        Contribution c = RECORDS.computeIfAbsent(mob.getUUID(), k -> new HashMap<>())
                .computeIfAbsent(player.getUUID(), k -> new Contribution());
        c.damage += amount;
        c.lastHitTick = tick;
    }

    /** Which player gets the credit for this damage (player, pet owner, or last attacker for fire/poison). */
    static ServerPlayer attacker(LivingEntity mob, DamageSource source) {
        Entity e = source.getEntity();
        if (e instanceof ServerPlayer p) return real(p);
        if (e != null) {
            if (Config.PETS_COUNT.get()) {
                if (e instanceof OwnableEntity own && own.getOwner() instanceof ServerPlayer p) return real(p);
                if (Config.SUMMON_LOOKUP.get()) {
                    ServerPlayer p = SummonOwners.owner(e);   // summons of other mods (Iron's Spells...)
                    if (p != null) return real(p);
                }
            }
            return null;   // hurt by another mob
        }
        if (Config.CREDIT_INDIRECT.get() && mob.getLastHurtByMob() instanceof ServerPlayer p
                && mob.tickCount - mob.getLastHurtByMobTimestamp() <= 100) return real(p);
        return null;
    }

    private static ServerPlayer real(ServerPlayer p) {
        return p instanceof FakePlayer || p.isSpectator() ? null : p;
    }

    @SubscribeEvent
    public void onLeave(EntityLeaveLevelEvent event) {
        // Unloaded / despawned / killed: forget it (the XP event has already run for a killed mob).
        if (!event.getLevel().isClientSide() && !RECORDS.isEmpty()) RECORDS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        tick++;
        if (tick % 600 != 0 || RECORDS.isEmpty()) return;
        long timeout = Config.CONTRIBUTION_TIMEOUT.get() * 20L;
        RECORDS.values().removeIf(map -> {
            map.values().removeIf(c -> tick - c.lastHitTick > timeout);
            return map.isEmpty();
        });
    }
}
