package com.yoyolegrand.partyxpshare;

import com.mojang.logging.LogUtils;
import com.yoyolegrand.partyxpshare.compat.FtbTeamsCompat;
import com.yoyolegrand.partyxpshare.compat.WaveSurvivorCompat;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(PartyXpShare.MODID)
public class PartyXpShare {

    public static final String MODID = "partyxpshare";
    public static final Logger LOGGER = LogUtils.getLogger();

    public PartyXpShare() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);

        MinecraftForge.EVENT_BUS.register(new DamageTracker());
        MinecraftForge.EVENT_BUS.register(new KillShareHandler());
        MinecraftForge.EVENT_BUS.register(new OrbShareHandler());
        MinecraftForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> PartyXpCommand.register(e.getDispatcher()));
        MinecraftForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> DamageTracker.clear());
    }

    private void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            WaveSurvivorCompat.init();
            FtbTeamsCompat.init();
        });
    }
}
