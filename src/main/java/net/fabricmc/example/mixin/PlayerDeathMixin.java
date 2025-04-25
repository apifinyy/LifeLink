package net.fabricmc.example.mixin;

import net.fabricmc.example.LifeLinkMod;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class PlayerDeathMixin {
    @Inject(method = "onDeath", at = @At("HEAD"))
    private void lifelink_onDeath(DamageSource source, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.getWorld().isClient && self instanceof ServerPlayerEntity player) {
            LifeLinkMod.LOGGER.info("PlayerDeathMixin fired for: {}", player.getName().getString());
            if (LifeLinkMod.isActive && LifeLinkMod.naturalDeathsEnabled && !LifeLinkMod.deadPlayers.contains(player.getName().getString())) {
                LifeLinkMod.LOGGER.info("Natural death detected for player: {}", player.getName().getString());
                LifeLinkMod.firstDeathPlayer = player.getName().getString();
                MinecraftServer server = player.getServer();
                if (server != null) {
                    LifeLinkMod.killAllPlayers(server);
                }
            }
        }
    }
}
