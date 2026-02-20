package com.yuno.yunosbosses;

import com.yuno.yunosbosses.event.ModKeybindings;
import net.fabricmc.api.ClientModInitializer;

public class YunosBossesClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Register keybindings
        ModKeybindings.register();
    }
}
