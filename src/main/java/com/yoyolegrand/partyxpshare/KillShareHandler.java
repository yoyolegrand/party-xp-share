package com.yoyolegrand.partyxpshare;

import com.yoyolegrand.partyxpshare.ShareMath.Mode;
import com.yoyolegrand.partyxpshare.compat.WaveSurvivorCompat;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/** When a mob drops its XP, split it between the players who hurt it. */
public final class KillShareHandler {

    private static final Random RNG = new Random();

    // LOWEST: we split the final amount, after every XP bonus from other mods (e.g. Wave Survivor's Shrine gear).
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onXpDrop(LivingExperienceDropEvent event) {
        LivingEntity mob = event.getEntity();
        if (mob.level().isClientSide || mob instanceof Player) return;
        Map<UUID, DamageTracker.Contribution> record = DamageTracker.take(mob);
        if (record == null || record.isEmpty()) return;
        if (!Config.ENABLED.get() || !Config.KILL_ENABLED.get()) return;

        int xp = event.getDroppedExperience();
        if (xp <= 0 || !XpUtil.isShareable(mob.getType())) return;

        MinecraftServer server = mob.getServer();
        if (server == null) return;

        boolean horde = WaveSurvivorCompat.isHordeMob(mob);
        ShareMath.Settings settings = settings(horde);
        int radius = horde ? Config.WS_RADIUS.get() : Config.SHARE_RADIUS.get();
        double r2 = (double) radius * radius;
        long timeout = Config.CONTRIBUTION_TIMEOUT.get() * 20L;
        long now = DamageTracker.now();

        UUID killer = event.getAttackingPlayer() instanceof ServerPlayer kp ? kp.getUUID() : null;

        List<ShareMath.Participant> participants = new ArrayList<>();
        Set<UUID> hitters = new HashSet<>(record.keySet());
        for (var e : record.entrySet()) {
            DamageTracker.Contribution c = e.getValue();
            if (now - c.lastHitTick > timeout) continue;
            ServerPlayer p = player(server, e.getKey());
            if (!XpUtil.canReceive(p) || p.level() != mob.level()) continue;
            if (radius > 0 && p.distanceToSqr(mob) > r2) continue;
            participants.add(new ShareMath.Participant(p.getUUID(), c.damage, p.getUUID().equals(killer)));
        }

        List<UUID> supporters = new ArrayList<>();
        if (horde && settings.supportSharePercent() > 0) {
            for (ServerPlayer p : XpUtil.playersNear(mob, radius)) {
                if (!hitters.contains(p.getUUID())) supporters.add(p.getUUID());
            }
        }

        List<ShareMath.Share> shares = ShareMath.compute(xp, participants, supporters, settings, RNG);
        if (shares == null) return;   // single recipient: vanilla drop untouched

        event.setDroppedExperience(0);
        Config.Delivery delivery = Config.DELIVERY.get();
        boolean messages = Config.SHOW_MESSAGES.get() && !(horde && Config.WS_HIDE_MESSAGES.get());
        for (ShareMath.Share s : shares) {
            ServerPlayer p = player(server, s.id());
            if (p == null || s.xp() <= 0) continue;
            XpUtil.deliver(p, s.xp(), delivery);
            if (messages) p.displayClientMessage(message(s, settings.mode()), true);
        }
    }

    private static ServerPlayer player(MinecraftServer server, UUID id) {
        return server.getPlayerList().getPlayer(id);
    }

    static ShareMath.Settings settings(boolean horde) {
        Mode mode = Config.MODE.get();
        double minContribution = Config.MIN_CONTRIBUTION.get();
        double groupPer = Config.GROUP_BONUS_PER_PLAYER.get();
        double support = 0;
        if (horde) {
            if (Config.WS_MODE.get() != Config.ModeOverride.GLOBAL) mode = Mode.valueOf(Config.WS_MODE.get().name());
            if (Config.WS_MIN_CONTRIBUTION.get() >= 0) minContribution = Config.WS_MIN_CONTRIBUTION.get();
            if (Config.WS_GROUP_BONUS_PER_PLAYER.get() >= 0) groupPer = Config.WS_GROUP_BONUS_PER_PLAYER.get();
            support = Config.WS_SUPPORT_SHARE.get();
        }
        return new ShareMath.Settings(mode, Config.KILLER_BONUS.get(), minContribution, groupPer,
                Config.GROUP_BONUS_MAX.get(), support);
    }

    private static Component message(ShareMath.Share s, Mode mode) {
        boolean fr = Config.fr();
        String detail = switch (s.kind()) {
            case SUPPORT -> fr ? "soutien" : "support";
            case KILLER_BONUS_ONLY -> fr ? "coup fatal" : "killing blow";
            case ATTACKER -> mode == Mode.EQUAL
                    ? (fr ? "part égale" : "equal share")
                    : Math.round(s.damagePercent()) + (fr ? "% des dégâts" : "% damage");
        };
        return Component.literal("§a+" + s.xp() + " XP §7(" + detail + ")");
    }
}
