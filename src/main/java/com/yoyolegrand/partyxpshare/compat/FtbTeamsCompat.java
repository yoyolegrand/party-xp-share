package com.yoyolegrand.partyxpshare.compat;

import com.yoyolegrand.partyxpshare.Config;
import com.yoyolegrand.partyxpshare.PartyXpShare;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Soft dependency on FTB Teams: no compile-time link, the API is reached by reflection
 * (FTBTeamsAPI.api().getManager().arePlayersInSameTeam(UUID, UUID)), so Party XP Share runs fine without it.
 */
public final class FtbTeamsCompat {

    private static boolean loaded;
    private static Method api, isManagerLoaded, getManager, sameTeam;

    private FtbTeamsCompat() {}

    public static void init() {
        loaded = false;
        if (!ModList.get().isLoaded("ftbteams")) return;
        try {
            Class<?> apiClass = Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI");
            Class<?> apiInterface = Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI$API");
            Class<?> manager = Class.forName("dev.ftb.mods.ftbteams.api.TeamManager");
            api = apiClass.getMethod("api");
            isManagerLoaded = apiInterface.getMethod("isManagerLoaded");
            getManager = apiInterface.getMethod("getManager");
            sameTeam = manager.getMethod("arePlayersInSameTeam", UUID.class, UUID.class);
            loaded = true;
            PartyXpShare.LOGGER.info("FTB Teams detected: team sharing available");
        } catch (Throwable t) {
            PartyXpShare.LOGGER.warn("FTB Teams detected but its API could not be found, team sharing disabled: {}", t.toString());
        }
    }

    public static boolean isLoaded() {
        return loaded;
    }

    /** True when XP must stay inside teams (FTB Teams installed, compat on, mode TEAM_ONLY). */
    public static boolean active() {
        return loaded && Config.TEAMS_ENABLED.get() && Config.TEAM_MODE.get() == Config.TeamMode.TEAM_ONLY;
    }

    /** Same FTB team? A player without a team (personal team) is only in a team with themself. */
    public static boolean sameTeam(UUID a, UUID b) {
        if (a.equals(b)) return true;
        try {
            Object instance = api.invoke(null);
            if (!(boolean) isManagerLoaded.invoke(instance)) return true;   // server not ready: no filtering
            return (boolean) sameTeam.invoke(getManager.invoke(instance), a, b);
        } catch (Throwable t) {
            loaded = false;   // API changed: back to the 1.0 behaviour from now on
            PartyXpShare.LOGGER.warn("FTB Teams API call failed, team sharing disabled: {}", t.toString());
            return true;
        }
    }
}
