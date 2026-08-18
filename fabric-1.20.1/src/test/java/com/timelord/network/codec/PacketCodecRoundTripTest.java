package com.timelord.network.codec;

import com.timelord.common.ability.AbilityId;
import com.timelord.common.model.TemporalPosition;
import com.timelord.common.model.ThreatInfo;
import com.timelord.common.model.ThreatType;
import com.timelord.common.network.message.AbilityMessages;
import com.timelord.common.network.message.FutureSightMessages;
import com.timelord.common.network.message.JudgementCutMessages;
import com.timelord.common.network.message.TheWorldMessages;
import com.timelord.common.network.message.TimeFieldMessages;
import com.timelord.common.network.message.TimeRewindMessages;
import com.timelord.common.network.message.TimeShiftMessages;
import com.timelord.mih.MadeInHeavenState;
import com.timelord.mih.MadeInHeavenSyncState;
import net.minecraft.network.PacketByteBuf;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PacketCodecRoundTripTest {
    @Test
    void roundTripsAbilityMessages() {
        AbilityMessages.CooldownUpdate cooldown =
                new AbilityMessages.CooldownUpdate(AbilityId.TIME_REWIND, 173);
        assertEquals(cooldown, decode(AbilityPacketCodec.encode(cooldown), AbilityPacketCodec::decodeCooldown));

        AbilityMessages.LoadoutSync loadout = new AbilityMessages.LoadoutSync(
                List.of(AbilityId.SLOW_TIME, AbilityId.THE_WORLD, AbilityId.MADE_IN_HEAVEN),
                List.of(
                        AbilityId.SLOW_TIME,
                        AbilityId.THE_WORLD,
                        AbilityId.TIME_SHIFT,
                        AbilityId.FUTURE_SIGHT,
                        AbilityId.MADE_IN_HEAVEN
                )
        );
        assertEquals(loadout, decode(AbilityPacketCodec.encode(loadout), AbilityPacketCodec::decodeLoadout));
    }

    @Test
    void roundTripsTheWorldMessagesInInsertionOrder() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        LinkedHashMap<UUID, Integer> durations = new LinkedHashMap<>();
        durations.put(first, 120);
        durations.put(second, 80);
        TheWorldMessages.State state = new TheWorldMessages.State(durations, 200);

        TheWorldMessages.State decoded =
                decode(TheWorldPacketCodec.encode(state), TheWorldPacketCodec::decodeState);
        assertEquals(List.of(first, second), decoded.activeDurations().keySet().stream().toList());
        assertEquals(state, decoded);

        TheWorldMessages.StoredHit hit = new TheWorldMessages.StoredHit(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                new TemporalPosition(1.0D, 2.0D, 3.0D),
                new TemporalPosition(0.5D, 0.0D, -0.5D)
        );
        assertEquals(hit, decode(TheWorldPacketCodec.encode(hit), TheWorldPacketCodec::decodeStoredHit));
    }

    @Test
    void roundTripsEffectAndThreatMessages() {
        TimeRewindMessages.Effect rewind = new TimeRewindMessages.Effect(
                UUID.randomUUID(), TemporalPosition.ZERO, new TemporalPosition(4.0D, 5.0D, 6.0D), 10);
        assertEquals(rewind, decode(TimeRewindPacketCodec.encode(rewind), TimeRewindPacketCodec::decodeEffect));

        FutureSightMessages.Threats threats = new FutureSightMessages.Threats(List.of(
                new ThreatInfo(42, ThreatType.DANGEROUS_PROJECTILE),
                new ThreatInfo(7, ThreatType.HOSTILE_MOB)
        ));
        assertEquals(threats, decode(
                FutureSightPacketCodec.encode(threats), FutureSightPacketCodec::decodeThreats));
    }

    @Test
    void roundTripsRemainingStateMessages() {
        TimeFieldMessages.Started field = new TimeFieldMessages.Started(
                UUID.randomUUID(), new TemporalPosition(2.0D, 64.0D, 3.0D), 12.0D, 200);
        assertEquals(field, decode(TimeFieldPacketCodec.encode(field), TimeFieldPacketCodec::decodeStarted));

        JudgementCutMessages.Release release = new JudgementCutMessages.Release(8.5D, 12345L, 128);
        assertEquals(release, decode(
                JudgementCutPacketCodec.encode(release), JudgementCutPacketCodec::decodeRelease));

        TimeShiftMessages.State timeShift = new TimeShiftMessages.State(true, 10);
        assertEquals(timeShift, decode(
                TimeShiftPacketCodec.encode(timeShift), TimeShiftPacketCodec::decodeState));
    }

    @Test
    void roundTripsMadeInHeavenStateWithoutSnapshots() {
        LinkedHashSet<UUID> activeUsers = new LinkedHashSet<>();
        activeUsers.add(UUID.randomUUID());
        activeUsers.add(UUID.randomUUID());
        MadeInHeavenSyncState state = new MadeInHeavenSyncState(
                7L,
                MadeInHeavenState.Phase.BUILDUP,
                843,
                0,
                12_345,
                false,
                activeUsers
        );

        assertEquals(state, decode(
                MadeInHeavenPacketCodec.encode(state),
                MadeInHeavenPacketCodec::decode
        ));
    }

    private static <T> T decode(PacketByteBuf buffer, Function<PacketByteBuf, T> decoder) {
        try {
            return decoder.apply(buffer);
        } finally {
            buffer.release();
        }
    }
}
