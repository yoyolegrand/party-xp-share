package com.yoyolegrand.partyxpshare.compat;

import com.yoyolegrand.partyxpshare.Config;
import com.yoyolegrand.partyxpshare.PartyXpShare;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.fml.ModList;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Soft dependency on Wave Survivor: no compile-time link, so Party XP Share runs fine without it.
 * Uses Wave Survivor's own check (HordeUnitGuard.isHordeEntity) when available, otherwise its entity tags.
 */
public final class WaveSurvivorCompat {

    /** Persistent tags Wave Survivor puts on horde units, summons, Kingdom units and invasions. */
    private static final String[] TAGS = {"ws_horde_unit", "ws_invasion", "ws_kingdom_guardian", "ws_kingdom_escort",
            "ws_revenant", "ws_illusion", "ws_elite", "ws_mutated", "ws_obj_champion"};

    private static boolean loaded;
    private static MethodHandle isHordeEntity;

    private WaveSurvivorCompat() {}

    public static void init() {
        loaded = ModList.get().isLoaded("wavesurvivor");
        if (!loaded) return;
        try {
            Class<?> guard = Class.forName("com.wavesurvivor.horde.spawn.HordeUnitGuard");
            isHordeEntity = MethodHandles.publicLookup().findStatic(guard, "isHordeEntity",
                    MethodType.methodType(boolean.class, Entity.class));
            PartyXpShare.LOGGER.info("Wave Survivor detected: horde compatibility enabled");
        } catch (Throwable t) {
            isHordeEntity = null;
            PartyXpShare.LOGGER.info("Wave Survivor detected (tag-based horde detection): {}", t.toString());
        }
    }

    public static boolean isLoaded() {
        return loaded;
    }

    /** True when the compat is active and this mob belongs to a Wave Survivor game (unit, boss, summon, Kingdom...). */
    public static boolean isHordeMob(Entity e) {
        if (!loaded || !Config.WS_ENABLED.get() || e == null) return false;
        if (isHordeEntity != null) {
            try {
                return (boolean) isHordeEntity.invokeExact(e);
            } catch (Throwable t) {
                isHordeEntity = null;   // API changed: fall back to tags from now on
                PartyXpShare.LOGGER.warn("Wave Survivor API call failed, switching to tag detection: {}", t.toString());
            }
        }
        var tag = e.getPersistentData();
        for (String t : TAGS) if (tag.getBoolean(t)) return true;
        return false;
    }
}
