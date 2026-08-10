package com.timelord.client.network;

import com.timelord.client.time.ClientTimeField;
import com.timelord.network.TimeFieldNetworking;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

public final class TimeFieldClientNetworking {

    private TimeFieldClientNetworking() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(
                TimeFieldNetworking.START_FIELD,
                (client, handler, buf, responseSender) -> {
                    UUID owner = buf.readUuid();
                    double x = buf.readDouble();
                    double y = buf.readDouble();
                    double z = buf.readDouble();
                    double radius = buf.readDouble();

                    int durationTicks = buf.readInt();

                    Vec3d center = new Vec3d(x, y, z);
                    client.execute(() -> {
                        ClientTimeField.set(
                                owner,
                                center,
                                radius,
                                durationTicks
                        );
                    });
                }
        );

        ClientPlayNetworking.registerGlobalReceiver(
                TimeFieldNetworking.REMOVE_FIELD,
                (client, handler, buf, responseSender) -> {
                    UUID owner = buf.readUuid();
                    client.execute(() -> {
                        ClientTimeField.remove(owner);
                    });
                }
        );
    }
}