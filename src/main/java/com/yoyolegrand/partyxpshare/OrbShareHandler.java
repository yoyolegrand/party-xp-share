package com.yoyolegrand.partyxpshare;

import com.yoyolegrand.partyxpshare.compat.FtbTeamsCompat;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Orb sharing: a picked-up orb is shared with nearby players.
 * SPLIT lowers the orb's value for this pickup only (restored at the end of the tick, so stacked orbs stay correct).
 */
public final class OrbShareHandler {

    private static final Random RNG = new Random();
    /** Orbs whose value was lowered this tick -> original value. */
    private static final Map<ExperienceOrb, Integer> PENDING = new IdentityHashMap<>();

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPickup(PlayerXpEvent.PickupXp event) {
        if (!Config.ENABLED.get() || !Config.ORB_ENABLED.get()) return;
        if (!(event.getEntity() instanceof ServerPlayer picker) || !XpUtil.canReceive(picker)) return;
        ExperienceOrb orb = event.getOrb();
        if (orb.getPersistentData().getBoolean(XpUtil.SHARED_TAG)) return;

        int value = PENDING.getOrDefault(orb, orb.value);
        if (value <= 0) return;

        List<ServerPlayer> others = XpUtil.playersNear(picker, Config.ORB_RADIUS.get());
        others.remove(picker);
        if (FtbTeamsCompat.active()) others.removeIf(p -> !FtbTeamsCompat.sameTeam(picker.getUUID(), p.getUUID()));
        if (others.isEmpty()) return;

        if (Config.ORB_MODE.get() == Config.OrbMode.SPLIT) {
            double each = (double) value / (others.size() + 1);
            for (ServerPlayer p : others) XpUtil.giveWithMending(p, ShareMath.randomRound(each, RNG));
            PENDING.putIfAbsent(orb, value);
            orb.value = ShareMath.randomRound(each, RNG);   // the picker's part goes through the vanilla pickup (Mending included)
        } else {
            double each = value * Config.ORB_BONUS.get() / 100.0;
            for (ServerPlayer p : others) XpUtil.giveWithMending(p, ShareMath.randomRound(each, RNG));
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PENDING.isEmpty()) return;
        PENDING.forEach((orb, original) -> {
            if (!orb.isRemoved()) orb.value = original;
        });
        PENDING.clear();
    }
}
