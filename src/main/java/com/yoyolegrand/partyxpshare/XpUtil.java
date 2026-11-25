package com.yoyolegrand.partyxpshare;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public final class XpUtil {

    /** Orbs created by Party XP Share: never shared a second time by orb sharing. */
    public static final String SHARED_TAG = "partyxpshare_shared";

    private XpUtil() {}

    public static void deliver(ServerPlayer player, int amount, Config.Delivery mode) {
        if (amount <= 0) return;
        if (mode == Config.Delivery.ORBS) spawnOrbs(player, amount);
        else giveWithMending(player, amount);
    }

    /** Spawns XP orbs on the player (vanilla orb sizes), tagged so orb sharing ignores them. */
    public static void spawnOrbs(ServerPlayer player, int amount) {
        ServerLevel level = player.serverLevel();
        while (amount > 0) {
            int v = ExperienceOrb.getExperienceValue(amount);
            amount -= v;
            ExperienceOrb orb = new ExperienceOrb(level, player.getX(), player.getY() + 0.5, player.getZ(), v);
            orb.getPersistentData().putBoolean(SHARED_TAG, true);
            level.addFreshEntity(orb);
        }
    }

    /** Gives XP points directly, repairing Mending items first exactly like an orb would. */
    public static void giveWithMending(ServerPlayer player, int amount) {
        int remaining = amount;
        for (int guard = 0; remaining > 0 && guard < 64; guard++) {
            var entry = EnchantmentHelper.getRandomItemWith(Enchantments.MENDING, player, ItemStack::isDamaged);
            if (entry == null) break;
            ItemStack stack = entry.getValue();
            float ratio = stack.getXpRepairRatio();
            if (ratio <= 0) break;
            int repair = Math.min((int) (remaining * ratio), stack.getDamageValue());
            if (repair <= 0) break;
            stack.setDamageValue(stack.getDamageValue() - repair);
            remaining -= Math.max(1, (int) Math.ceil(repair / ratio));
        }
        if (remaining > 0) player.giveExperiencePoints(remaining);
    }

    /** Real, living, non-spectator players in the same level within radius of the entity (radius <= 0: whole level). */
    public static List<ServerPlayer> playersNear(Entity center, double radius) {
        List<ServerPlayer> out = new ArrayList<>();
        if (!(center.level() instanceof ServerLevel level)) return out;
        double r2 = radius * radius;
        for (ServerPlayer p : level.players()) {
            if (!canReceive(p)) continue;
            if (radius > 0 && p.distanceToSqr(center) > r2) continue;
            out.add(p);
        }
        return out;
    }

    public static boolean canReceive(ServerPlayer p) {
        return p != null && p.isAlive() && !p.isSpectator() && !(p instanceof FakePlayer);
    }

    /** Blacklist / whitelist check (entity ids or #tags). */
    public static boolean isShareable(EntityType<?> type) {
        List<? extends String> white = Config.WHITELIST.get();
        if (!white.isEmpty() && !matches(type, white)) return false;
        return !matches(type, Config.BLACKLIST.get());
    }

    private static boolean matches(EntityType<?> type, List<? extends String> list) {
        if (list.isEmpty()) return false;
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
        for (String s : list) {
            String entry = s.trim();
            if (entry.isEmpty()) continue;
            if (entry.startsWith("#")) {
                ResourceLocation tag = ResourceLocation.tryParse(entry.substring(1));
                if (tag != null && type.is(TagKey.create(Registries.ENTITY_TYPE, tag))) return true;
            } else if (id != null && entry.equals(id.toString())) {
                return true;
            }
        }
        return false;
    }
}
