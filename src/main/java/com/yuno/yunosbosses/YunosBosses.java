package com.yuno.yunosbosses;

import com.yuno.yunosbosses.event.ModCommands;
import com.yuno.yunosbosses.item.ModItems;
import com.yuno.yunosbosses.network.ModMessages;
import com.yuno.yunosbosses.spell.ModSpells;
import net.fabricmc.api.ModInitializer;

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

		// Register payload types
		ModMessages.registerC2SPackets();
	}
}