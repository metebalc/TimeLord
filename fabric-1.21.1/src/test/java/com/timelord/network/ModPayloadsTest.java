package com.timelord.network;

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
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ModPayloadsTest {
	private static final UUID FIRST_ID = UUID.fromString("01234567-89ab-cdef-0123-456789abcdef");
	private static final UUID SECOND_ID = UUID.fromString("fedcba98-7654-3210-fedc-ba9876543210");
	private static final UUID THIRD_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
	private static final TemporalPosition FIRST_POSITION = new TemporalPosition(1.25D, -2.5D, 3.75D);
	private static final TemporalPosition SECOND_POSITION = new TemporalPosition(-4.5D, 5.25D, 0.125D);

	@Test
	void everyPayloadCodecRoundTripsWithoutTrailingBytes() {
		assertRoundTrip(new ModPayloads.ActivateAbilityPayload(new AbilityMessages.ActivateAbility(3)), ModPayloads.ActivateAbilityPayload.CODEC);
		assertRoundTrip(new ModPayloads.StartChargePayload(new AbilityMessages.StartCharge(2)), ModPayloads.StartChargePayload.CODEC);
		assertRoundTrip(new ModPayloads.ReleaseChargePayload(new AbilityMessages.ReleaseCharge()), ModPayloads.ReleaseChargePayload.CODEC);
		assertRoundTrip(new ModPayloads.SwitchSlowModePayload(new AbilityMessages.SwitchSlowMode()), ModPayloads.SwitchSlowModePayload.CODEC);
		assertRoundTrip(new ModPayloads.EquipAbilityPayload(new AbilityMessages.EquipAbility(1, AbilityId.FUTURE_SIGHT)), ModPayloads.EquipAbilityPayload.CODEC);
		assertRoundTrip(new ModPayloads.CooldownPayload(new AbilityMessages.CooldownUpdate(AbilityId.TIME_REWIND, 287)), ModPayloads.CooldownPayload.CODEC);
		assertRoundTrip(new ModPayloads.AbilityLoadoutPayload(new AbilityMessages.LoadoutSync(
				List.of(AbilityId.SLOW_TIME, AbilityId.THE_WORLD, AbilityId.TIME_SHIFT),
				List.of(AbilityId.SLOW_TIME, AbilityId.THE_WORLD, AbilityId.DIMENSION_CUT, AbilityId.TIME_SHIFT))), ModPayloads.AbilityLoadoutPayload.CODEC);
		assertRoundTrip(new ModPayloads.AbilityStatePayload(new AbilityMessages.AbilityStateUpdate(AbilityId.DIMENSION_CUT, true, 15, 40)), ModPayloads.AbilityStatePayload.CODEC);

		assertRoundTrip(new ModPayloads.TimeShiftStatePayload(new TimeShiftMessages.State(true, 4)), ModPayloads.TimeShiftStatePayload.CODEC);
		assertRoundTrip(new ModPayloads.TimeShiftChargeStartPayload(new TimeShiftMessages.StartCharge()), ModPayloads.TimeShiftChargeStartPayload.CODEC);
		assertRoundTrip(new ModPayloads.TimeShiftReleasePayload(new TimeShiftMessages.Release()), ModPayloads.TimeShiftReleasePayload.CODEC);
		assertRoundTrip(new ModPayloads.TimeShiftBurstPayload(new TimeShiftMessages.Burst()), ModPayloads.TimeShiftBurstPayload.CODEC);

		assertRoundTrip(new ModPayloads.FutureSightThreatsPayload(new FutureSightMessages.Threats(List.of(
				new ThreatInfo(17, ThreatType.DANGEROUS_PROJECTILE),
				new ThreatInfo(2048, ThreatType.HOSTILE_MOB)))), ModPayloads.FutureSightThreatsPayload.CODEC);

		assertRoundTrip(new ModPayloads.JudgementStartPayload(new JudgementCutMessages.Start(FIRST_POSITION)), ModPayloads.JudgementStartPayload.CODEC);
		assertRoundTrip(new ModPayloads.JudgementReleasePayload(new JudgementCutMessages.Release(12.5D, 987654321L, 27)), ModPayloads.JudgementReleasePayload.CODEC);
		assertRoundTrip(new ModPayloads.JudgementClearPayload(new JudgementCutMessages.Clear()), ModPayloads.JudgementClearPayload.CODEC);
		assertRoundTrip(new ModPayloads.JudgementMonochromePayload(new JudgementCutMessages.Monochrome(true)), ModPayloads.JudgementMonochromePayload.CODEC);

		Map<UUID, Integer> durations = new LinkedHashMap<>();
		durations.put(FIRST_ID, 160);
		durations.put(SECOND_ID, 42);
		assertRoundTrip(new ModPayloads.TheWorldStatePayload(new TheWorldMessages.State(durations, 200)), ModPayloads.TheWorldStatePayload.CODEC);
		assertRoundTrip(new ModPayloads.TheWorldActivationPayload(new TheWorldMessages.Activation(FIRST_ID, true)), ModPayloads.TheWorldActivationPayload.CODEC);
		assertRoundTrip(new ModPayloads.TheWorldStoredHitPayload(new TheWorldMessages.StoredHit(FIRST_ID, SECOND_ID, THIRD_ID, FIRST_POSITION, SECOND_POSITION)), ModPayloads.TheWorldStoredHitPayload.CODEC);
		assertRoundTrip(new ModPayloads.TheWorldResolveHitPayload(new TheWorldMessages.ResolveHit(FIRST_ID, 3, 12)), ModPayloads.TheWorldResolveHitPayload.CODEC);

		assertRoundTrip(new ModPayloads.TimeFieldStartedPayload(new TimeFieldMessages.Started(FIRST_ID, FIRST_POSITION, 8.5D, 120)), ModPayloads.TimeFieldStartedPayload.CODEC);
		assertRoundTrip(new ModPayloads.TimeFieldRemovedPayload(new TimeFieldMessages.Removed(FIRST_ID)), ModPayloads.TimeFieldRemovedPayload.CODEC);
		assertRoundTrip(new ModPayloads.TimeRewindEffectPayload(new TimeRewindMessages.Effect(FIRST_ID, FIRST_POSITION, SECOND_POSITION, 18)), ModPayloads.TimeRewindEffectPayload.CODEC);
	}

	@Test
	void payloadIdsPreserveAllExistingChannelNames() {
		assertIds(
				"time-lord:activate_ability", ModPayloads.ActivateAbilityPayload.ID,
				"time-lord:start_charge", ModPayloads.StartChargePayload.ID,
				"time-lord:release_charge", ModPayloads.ReleaseChargePayload.ID,
				"time-lord:switch_slow_mode", ModPayloads.SwitchSlowModePayload.ID,
				"time-lord:cooldown", ModPayloads.CooldownPayload.ID,
				"time-lord:equip_ability", ModPayloads.EquipAbilityPayload.ID,
				"time-lord:ability_loadout", ModPayloads.AbilityLoadoutPayload.ID,
				"time-lord:ability_state", ModPayloads.AbilityStatePayload.ID,
				"time-lord:the_world_state", ModPayloads.TheWorldStatePayload.ID,
				"time-lord:the_world_hit", ModPayloads.TheWorldStoredHitPayload.ID,
				"time-lord:the_world_resolve", ModPayloads.TheWorldResolveHitPayload.ID,
				"time-lord:the_world_activate", ModPayloads.TheWorldActivationPayload.ID,
				"time-lord:time_shift_state", ModPayloads.TimeShiftStatePayload.ID,
				"time-lord:time_shift_charge_start", ModPayloads.TimeShiftChargeStartPayload.ID,
				"time-lord:time_shift_release", ModPayloads.TimeShiftReleasePayload.ID,
				"time-lord:time_shift_burst", ModPayloads.TimeShiftBurstPayload.ID,
				"time-lord:judgement_visual_start", ModPayloads.JudgementStartPayload.ID,
				"time-lord:judgement_visual_release", ModPayloads.JudgementReleasePayload.ID,
				"time-lord:judgement_visual_clear", ModPayloads.JudgementClearPayload.ID,
				"time-lord:judgement_monochrome", ModPayloads.JudgementMonochromePayload.ID,
				"time-lord:future_sight_threats", ModPayloads.FutureSightThreatsPayload.ID,
				"time-lord:time_rewind_effect", ModPayloads.TimeRewindEffectPayload.ID,
				"timelord:start_time_field", ModPayloads.TimeFieldStartedPayload.ID,
				"timelord:remove_time_field", ModPayloads.TimeFieldRemovedPayload.ID);
	}

	private static <T extends CustomPayload> void assertRoundTrip(T expected, PacketCodec<PacketByteBuf, T> codec) {
		PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
		try {
			codec.encode(buf, expected);
			assertEquals(expected, codec.decode(buf));
			assertEquals(0, buf.readableBytes(), "codec left unread bytes for " + expected.getId().id());
		} finally {
			buf.release();
		}
	}

	private static void assertIds(Object... expectedAndIds) {
		for (int index = 0; index < expectedAndIds.length; index += 2) {
			String expected = (String) expectedAndIds[index];
			CustomPayload.Id<?> id = (CustomPayload.Id<?>) expectedAndIds[index + 1];
			assertEquals(expected, id.id().toString());
		}
	}
}
