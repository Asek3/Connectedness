package me.pepperbell.continuity.client.util;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraftforge.eventbus.api.Event;

public class ClientJoinEvent extends Event {
	
    private final ClientPlayNetworkHandler handler;

    @ApiStatus.Internal
	public ClientJoinEvent(final ClientPlayNetworkHandler handler)
    {
        this.handler = handler;
    }

    public ClientPlayNetworkHandler getHandler()
    {
        return handler;
    }

}
