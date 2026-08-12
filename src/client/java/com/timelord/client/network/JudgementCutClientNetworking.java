package com.timelord.client.network;

import com.timelord.TimeLord;
import com.timelord.client.ClientJudgementCut;

import com.timelord.client.TimeLordClient;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.util.math.Vec3d;

public final class JudgementCutClientNetworking {

    private JudgementCutClientNetworking() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(
                TimeLord.JUDGEMENT_VISUAL_START_PACKET,
                (client, handler, buf, responseSender) -> {
                    double x = buf.readDouble();
                    double y = buf.readDouble();
                    double z = buf.readDouble();

                    Vec3d center = new Vec3d(x, y, z);
                    client.execute(() -> ClientJudgementCut.start(center));
                }
        );

        ClientPlayNetworking.registerGlobalReceiver(
                TimeLord.JUDGEMENT_VISUAL_RELEASE_PACKET,
                (client, handler, buf, responseSender) -> {
                    double radius = buf.readDouble();
                    long seed = buf.readLong();
                    int slashCount = buf.readInt();

                    client.execute(() ->
                            ClientJudgementCut.release(
                                    radius,
                                    seed,
                                    slashCount
                            )
                    );
                }
        );

        ClientPlayNetworking.registerGlobalReceiver(
                TimeLord.JUDGEMENT_VISUAL_CLEAR_PACKET,
                (client, handler, buf, responseSender) -> {
                    client.execute(() -> {
                        ClientJudgementCut.clear();
                        TimeLordClient.updateMonochromeShader(client);
                    });
                }
        );

        ClientPlayNetworking.registerGlobalReceiver(
                TimeLord.JUDGEMENT_MONOCHROME_PACKET,
                (client,
                 handler,
                 buf,
                 responseSender) -> {
                    boolean active = buf.readBoolean();
                    client.execute(() -> {
                        ClientJudgementCut.setMonochrome(active);
                        TimeLordClient.updateMonochromeShader(client);
                    });
                }
        );
    }
}