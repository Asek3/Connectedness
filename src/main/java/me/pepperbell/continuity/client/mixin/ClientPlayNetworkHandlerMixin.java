package me.pepperbell.continuity.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.pepperbell.continuity.client.util.ClientJoinEvent;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraftforge.common.MinecraftForge;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

	@Inject(method = "onGameJoin", at = @At("RETURN"))
	private void handleServerPlayReady(GameJoinS2CPacket packet, CallbackInfo ci) {
		MinecraftForge.EVENT_BUS.post(new ClientJoinEvent(((ClientPlayNetworkHandler)(Object)this)));
	}

}