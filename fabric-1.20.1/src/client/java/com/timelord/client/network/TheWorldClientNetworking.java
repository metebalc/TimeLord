package com.timelord.client.network;

import com.timelord.adapter.TemporalPositionAdapter;
import com.timelord.client.render.TheWorldHitRenderer;
import com.timelord.client.render.TheWorldRenderer;
import com.timelord.client.render.TheWorldShockwaveRenderer;
import com.timelord.client.time.TheWorldClientState;
import com.timelord.common.network.message.TheWorldMessages;
import com.timelord.network.PacketIds;
import com.timelord.network.codec.TheWorldPacketCodec;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class TheWorldClientNetworking {
    private TheWorldClientNetworking() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(
                PacketIds.THE_WORLD_STATE,
                (client, handler, buffer, responseSender) -> {
                    TheWorldMessages.State message = TheWorldPacketCodec.decodeState(buffer);
                    client.execute(() -> {
                        TheWorldClientState.setActiveUsers(
                                message.activeDurations(), message.maxDurationTicks());
                        if (!TheWorldClientState.isTimeStopped()) {
                            TheWorldRenderer.setActive(false);
                            TheWorldShockwaveRenderer.clear();
                        }
                    });
                }
        );

        ClientPlayNetworking.registerGlobalReceiver(
                PacketIds.THE_WORLD_HIT,
                (client, handler, buffer, responseSender) -> {
                    TheWorldMessages.StoredHit message = TheWorldPacketCodec.decodeStoredHit(buffer);
                    client.execute(() -> TheWorldHitRenderer.addHit(
                            message.hitId(),
                            message.targetId(),
                            message.attackerId(),
                            TemporalPositionAdapter.toMinecraft(message.impactPosition()),
                            TemporalPositionAdapter.toMinecraft(message.attackDirection())
                    ));
                }
        );

        ClientPlayNetworking.registerGlobalReceiver(
                PacketIds.THE_WORLD_RESOLVE,
                (client, handler, buffer, responseSender) -> {
                    TheWorldMessages.ResolveHit message = TheWorldPacketCodec.decodeResolveHit(buffer);
                    client.execute(() -> TheWorldHitRenderer.resolveHit(
                            message.hitId(), message.sequenceIndex()));
                }
        );

        ClientPlayNetworking.registerGlobalReceiver(
                PacketIds.THE_WORLD_ACTIVATE,
                (client, handler, buffer, responseSender) -> {
                    TheWorldMessages.Activation message = TheWorldPacketCodec.decodeActivation(buffer);
                    client.execute(() -> {
                        if (client.player == null)
                            return;

                        boolean localActivator = message.activatorId().equals(client.player.getUuid());
                        if (message.globalTransition()) {
                            if (localActivator) {
                                TheWorldRenderer.setActive(true);
                            } else {
                                TheWorldRenderer.setRemoteActive(true);
                                TheWorldShockwaveRenderer.start(message.activatorId());
                            }
                            return;
                        }

                        TheWorldShockwaveRenderer.start(message.activatorId());
                    });
                }
        );
    }
}
