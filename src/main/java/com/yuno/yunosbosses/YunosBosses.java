package com.yuno.yunosbosses;

import com.yuno.yunosbosses.entity.ModEntities;
import com.yuno.yunosbosses.event.ModCommands;
import com.yuno.yunosbosses.item.ModItems;
import com.yuno.yunosbosses.network.BarrierPayload;
import com.yuno.yunosbosses.network.BeamPayload;
import com.yuno.yunosbosses.network.ModMessages;
import com.yuno.yunosbosses.particle.ModParticles;
import com.yuno.yunosbosses.sound.ModSounds;
import com.yuno.yunosbosses.spell.ModSpells;
import com.yuno.yunosbosses.util.BarrierManager;
import com.yuno.yunosbosses.util.DelayedServerEffects;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.server.world.ServerWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YunosBosses implements ModInitializer {
	public static final String MOD_ID = "yunosbosses";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		ModItems.registerModItems();
		ModSpells.registerModSpells();
		ModCommands.register();
		ModEntities.registerAttributes();
		ModEntities.registerModEntities();
		ModParticles.registerParticles();
		ModSounds.registerSounds();

		// Register payload types
		ModMessages.registerC2SPackets();
		BeamPayload.register();
		BarrierPayload.register();

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			DelayedServerEffects.tick();

			for (ServerWorld world : server.getWorlds()) {
				BarrierManager.tick(world);
			}
		});
	}
}