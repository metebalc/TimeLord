package com.timelord.client.network;

import com.timelord.adapter.TemporalPositionAdapter;
import com.timelord.client.ClientJudgementCut;

import com.timelord.client.TimeLordClient;
import com.timelord.common.network.message.JudgementCutMessages;
import com.timelord.network.PacketIds;
import com.timelord.network.codec.JudgementCutPacketCodec;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class JudgementCutClientNetworking {

    private JudgementCutClientNetworking() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(
                PacketIds.JUDGEMENT_VISUAL_START,
                (client, handler, buf, responseSender) -> {
                    JudgementCutMessages.Start message = JudgementCutPacketCodec.decodeStart(buf);
                    client.execute(() -> ClientJudgementCut.start(
                            TemporalPositionAdapter.toMinecraft(message.center())));
                }
        );

        ClientPlayNetworking.registerGlobalReceiver(
                PacketIds.JUDGEMENT_VISUAL_RELEASE,
                (client, handler, buf, responseSender) -> {
                    JudgementCutMessages.Release message = JudgementCutPacketCodec.decodeRelease(buf);

                    client.execute(() ->
                            ClientJudgementCut.release(
                                    message.radius(),
                                    message.seed(),
                                    message.slashCount()
                            )
                    );
                }
        );

        ClientPlayNetworking.registerGlobalReceiver(
                PacketIds.JUDGEMENT_VISUAL_CLEAR,
                (client, handler, buf, responseSender) -> {
                    client.execute(() -> {
                        ClientJudgementCut.clear();
                        TimeLordClient.updateMonochromeShader(client);
                    });
                }
        );

        ClientPlayNetworking.registerGlobalReceiver(
                PacketIds.JUDGEMENT_MONOCHROME,
                (client,
                 handler,
                 buf,
                 responseSender) -> {
                    JudgementCutMessages.Monochrome message = JudgementCutPacketCodec.decodeMonochrome(buf);
                    client.execute(() -> {
                        ClientJudgementCut.setMonochrome(message.active());
                        TimeLordClient.updateMonochromeShader(client);
                    });
                }
        );
    }
}
