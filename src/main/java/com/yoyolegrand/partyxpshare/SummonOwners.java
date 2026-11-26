package com.yoyolegrand.partyxpshare;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Fallback for summons that do not implement OwnableEntity (Iron's Spells uses IMagicSummon.getSummoner()):
 * a public no-arg getSummoner() or getOwner() returning an Entity or a UUID. The method is looked up once per class.
 */
final class SummonOwners {

    private static final Method NONE;
    private static final Map<Class<?>, Method> CACHE = new HashMap<>();

    static {
        try {
            NONE = Object.class.getMethod("hashCode");   // sentinel: "this class has no owner getter"
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    private SummonOwners() {}

    /** The player who owns this summon, or null. */
    static ServerPlayer owner(Entity summon) {
        Method m = CACHE.computeIfAbsent(summon.getClass(), SummonOwners::find);
        if (m == NONE) return null;
        try {
            Object o = m.invoke(summon);
            if (o instanceof ServerPlayer p) return p;
            if (o instanceof UUID id && summon.getServer() != null) return summon.getServer().getPlayerList().getPlayer(id);
        } catch (Throwable t) {
            CACHE.put(summon.getClass(), NONE);   // broken getter: never try this class again
        }
        return null;
    }

    private static Method find(Class<?> c) {
        for (String name : new String[]{"getSummoner", "getOwner"}) {
            try {
                Method m = c.getMethod(name);
                Class<?> r = m.getReturnType();
                if (Entity.class.isAssignableFrom(r) || r.isAssignableFrom(Entity.class) || r == UUID.class) return m;
            } catch (Throwable ignored) {
            }
        }
        return NONE;
    }

    static void clear() {
        CACHE.clear();
    }
}
