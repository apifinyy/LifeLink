package net.fabricmc.example;

import net.fabricmc.api.ModInitializer;

public class ExampleMod implements ModInitializer {
    @Override
    public void onInitialize() {
        new LifeLinkMod().onInitialize();
        // Initialize other mod components here
    }
}