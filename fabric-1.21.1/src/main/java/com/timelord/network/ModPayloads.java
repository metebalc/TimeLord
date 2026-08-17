package com.timelord.network;

import com.timelord.common.network.PacketChannels;
import com.timelord.common.network.PacketChannels.Channel;
import com.timelord.common.network.message.AbilityMessages;
import com.timelord.common.network.message.FutureSightMessages;
import com.timelord.common.network.message.JudgementCutMessages;
import com.timelord.common.network.message.TheWorldMessages;
import com.timelord.common.network.message.TimeFieldMessages;
import com.timelord.common.network.message.TimeRewindMessages;
import com.timelord.common.network.message.TimeShiftMessages;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public final class ModPayloads {
	private ModPayloads() {
	}

	public static void register() {
		registerC2S(ActivateAbilityPayload.ID, ActivateAbilityPayload.CODEC);
		registerC2S(StartChargePayload.ID, StartChargePayload.CODEC);
		registerC2S(ReleaseChargePayload.ID, ReleaseChargePayload.CODEC);
		registerC2S(SwitchSlowModePayload.ID, SwitchSlowModePayload.CODEC);
		registerC2S(EquipAbilityPayload.ID, EquipAbilityPayload.CODEC);
		registerC2S(TimeShiftChargeStartPayload.ID, TimeShiftChargeStartPayload.CODEC);
		registerC2S(TimeShiftReleasePayload.ID, TimeShiftReleasePayload.CODEC);

		registerS2C(CooldownPayload.ID, CooldownPayload.CODEC);
		registerS2C(AbilityLoadoutPayload.ID, AbilityLoadoutPayload.CODEC);
		registerS2C(AbilityStatePayload.ID, AbilityStatePayload.CODEC);
		registerS2C(TheWorldStatePayload.ID, TheWorldStatePayload.CODEC);
		registerS2C(TheWorldStoredHitPayload.ID, TheWorldStoredHitPayload.CODEC);
		registerS2C(TheWorldResolveHitPayload.ID, TheWorldResolveHitPayload.CODEC);
		registerS2C(TheWorldActivationPayload.ID, TheWorldActivationPayload.CODEC);
		registerS2C(TimeShiftStatePayload.ID, TimeShiftStatePayload.CODEC);
		registerS2C(TimeShiftBurstPayload.ID, TimeShiftBurstPayload.CODEC);
		registerS2C(JudgementStartPayload.ID, JudgementStartPayload.CODEC);
		registerS2C(JudgementReleasePayload.ID, JudgementReleasePayload.CODEC);
		registerS2C(JudgementClearPayload.ID, JudgementClearPayload.CODEC);
		registerS2C(JudgementMonochromePayload.ID, JudgementMonochromePayload.CODEC);
		registerS2C(FutureSightThreatsPayload.ID, FutureSightThreatsPayload.CODEC);
		registerS2C(TimeRewindEffectPayload.ID, TimeRewindEffectPayload.CODEC);
		registerS2C(TimeFieldStartedPayload.ID, TimeFieldStartedPayload.CODEC);
		registerS2C(TimeFieldRemovedPayload.ID, TimeFieldRemovedPayload.CODEC);
	}

	private static <T extends CustomPayload> void registerC2S(CustomPayload.Id<T> id, PacketCodec<PacketByteBuf, T> codec) {
		PayloadTypeRegistry.playC2S().register(id, codec);
	}

	private static <T extends CustomPayload> void registerS2C(CustomPayload.Id<T> id, PacketCodec<PacketByteBuf, T> codec) {
		PayloadTypeRegistry.playS2C().register(id, codec);
	}

	private static <T extends CustomPayload> CustomPayload.Id<T> id(Channel channel) {
		return new CustomPayload.Id<>(Identifier.of(channel.namespace(), channel.path()));
	}

	public record ActivateAbilityPayload(AbilityMessages.ActivateAbility message) implements CustomPayload {
		public static final Id<ActivateAbilityPayload> ID = id(PacketChannels.ACTIVATE_ABILITY);
		public static final PacketCodec<PacketByteBuf, ActivateAbilityPayload> CODEC = PayloadCodecs.codec(
				(buf, payload) -> buf.writeByte(payload.message.abilityNetworkId()),
				buf -> new ActivateAbilityPayload(new AbilityMessages.ActivateAbility(buf.readByte())));
		@Override public Id<? extends CustomPayload> getId() { return ID; }
	}

	public record StartChargePayload(AbilityMessages.StartCharge message) implements CustomPayload {
		public static final Id<StartChargePayload> ID = id(PacketChannels.START_CHARGE);
		public static final PacketCodec<PacketByteBuf, StartChargePayload> CODEC = PayloadCodecs.codec(
				(buf, payload) -> buf.writeByte(payload.message.abilityNetworkId()),
				buf -> new StartChargePayload(new AbilityMessages.StartCharge(buf.readByte())));
		@Override public Id<? extends CustomPayload> getId() { return ID; }
	}

	public record ReleaseChargePayload(AbilityMessages.ReleaseCharge message) implements CustomPayload {
		public static final Id<ReleaseChargePayload> ID = id(PacketChannels.RELEASE_CHARGE);
		public static final PacketCodec<PacketByteBuf, ReleaseChargePayload> CODEC = PayloadCodecs.codec(
				(buf, payload) -> {}, buf -> new ReleaseChargePayload(new AbilityMessages.ReleaseCharge()));
		@Override public Id<? extends CustomPayload> getId() { return ID; }
	}

	public record SwitchSlowModePayload(AbilityMessages.SwitchSlowMode message) implements CustomPayload {
		public static final Id<SwitchSlowModePayload> ID = id(PacketChannels.SWITCH_SLOW_MODE);
		public static final PacketCodec<PacketByteBuf, SwitchSlowModePayload> CODEC = PayloadCodecs.codec(
				(buf, payload) -> {}, buf -> new SwitchSlowModePayload(new AbilityMessages.SwitchSlowMode()));
		@Override public Id<? extends CustomPayload> getId() { return ID; }
	}

	public record EquipAbilityPayload(AbilityMessages.EquipAbility message) implements CustomPayload {
		public static final Id<EquipAbilityPayload> ID = id(PacketChannels.EQUIP_ABILITY);
		public static final PacketCodec<PacketByteBuf, EquipAbilityPayload> CODEC = PayloadCodecs.codec(
				(buf, payload) -> { buf.writeByte(payload.message.slot()); PayloadCodecs.writeAbility(buf, payload.message.ability()); },
				buf -> new EquipAbilityPayload(new AbilityMessages.EquipAbility(buf.readByte(), PayloadCodecs.readAbility(buf))));
		@Override public Id<? extends CustomPayload> getId() { return ID; }
	}

	public record CooldownPayload(AbilityMessages.CooldownUpdate message) implements CustomPayload {
		public static final Id<CooldownPayload> ID = id(PacketChannels.COOLDOWN);
		public static final PacketCodec<PacketByteBuf, CooldownPayload> CODEC = PayloadCodecs.codec(
				(buf, payload) -> { PayloadCodecs.writeAbility(buf, payload.message.ability()); buf.writeInt(payload.message.remainingTicks()); },
				buf -> new CooldownPayload(new AbilityMessages.CooldownUpdate(PayloadCodecs.readAbility(buf), buf.readInt())));
		@Override public Id<? extends CustomPayload> getId() { return ID; }
	}

	public record AbilityLoadoutPayload(AbilityMessages.LoadoutSync message) implements CustomPayload {
		public static final Id<AbilityLoadoutPayload> ID = id(PacketChannels.ABILITY_LOADOUT);
		public static final PacketCodec<PacketByteBuf, AbilityLoadoutPayload> CODEC = PayloadCodecs.codec(
				(buf, payload) -> { PayloadCodecs.writeAbilities(buf, payload.message.equipped()); PayloadCodecs.writeAbilities(buf, payload.message.unlocked()); },
				buf -> new AbilityLoadoutPayload(new AbilityMessages.LoadoutSync(PayloadCodecs.readAbilities(buf), PayloadCodecs.readAbilities(buf))));
		@Override public Id<? extends CustomPayload> getId() { return ID; }
	}

	public record AbilityStatePayload(AbilityMessages.AbilityStateUpdate message) implements CustomPayload {
		public static final Id<AbilityStatePayload> ID = id(PacketChannels.ABILITY_STATE);
		public static final PacketCodec<PacketByteBuf, AbilityStatePayload> CODEC = PayloadCodecs.codec(
				(buf, payload) -> { PayloadCodecs.writeAbility(buf, payload.message.ability()); buf.writeBoolean(payload.message.active()); buf.writeVarInt(payload.message.remainingTicks()); buf.writeVarInt(payload.message.totalTicks()); },
				buf -> new AbilityStatePayload(new AbilityMessages.AbilityStateUpdate(PayloadCodecs.readAbility(buf), buf.readBoolean(), buf.readVarInt(), buf.readVarInt())));
		@Override public Id<? extends CustomPayload> getId() { return ID; }
	}

	public record TimeShiftStatePayload(TimeShiftMessages.State message) implements CustomPayload {
		public static final Id<TimeShiftStatePayload> ID = id(PacketChannels.TIME_SHIFT_STATE);
		public static final PacketCodec<PacketByteBuf, TimeShiftStatePayload> CODEC = PayloadCodecs.codec(
				(buf, payload) -> { buf.writeBoolean(payload.message.active()); buf.writeInt(payload.message.multiplier()); },
				buf -> new TimeShiftStatePayload(new TimeShiftMessages.State(buf.readBoolean(), buf.readInt())));
		@Override public Id<? extends CustomPayload> getId() { return ID; }
	}

	public record TimeShiftChargeStartPayload(TimeShiftMessages.StartCharge message) implements CustomPayload {
		public static final Id<TimeShiftChargeStartPayload> ID = id(PacketChannels.TIME_SHIFT_CHARGE_START);
		public static final PacketCodec<PacketByteBuf, TimeShiftChargeStartPayload> CODEC = PayloadCodecs.codec(
				(buf, payload) -> {}, buf -> new TimeShiftChargeStartPayload(new TimeShiftMessages.StartCharge()));
		@Override public Id<? extends CustomPayload> getId() { return ID; }
	}

	public record TimeShiftReleasePayload(TimeShiftMessages.Release message) implements CustomPayload {
		public static final Id<TimeShiftReleasePayload> ID = id(PacketChannels.TIME_SHIFT_RELEASE);
		public static final PacketCodec<PacketByteBuf, TimeShiftReleasePayload> CODEC = PayloadCodecs.codec(
				(buf, payload) -> {}, buf -> new TimeShiftReleasePayload(new TimeShiftMessages.Release()));
		@Override public Id<? extends CustomPayload> getId() { return ID; }
	}

	public record TimeShiftBurstPayload(TimeShiftMessages.Burst message) implements CustomPayload {
		public static final Id<TimeShiftBurstPayload> ID = id(PacketChannels.TIME_SHIFT_BURST);
		public static final PacketCodec<PacketByteBuf, TimeShiftBurstPayload> CODEC = PayloadCodecs.codec(
				(buf, payload) -> {}, buf -> new TimeShiftBurstPayload(new TimeShiftMessages.Burst()));
		@Override public Id<? extends CustomPayload> getId() { return ID; }
	}

	public record FutureSightThreatsPayload(FutureSightMessages.Threats message) implements CustomPayload {
		public static final Id<FutureSightThreatsPayload> ID = id(PacketChannels.FUTURE_SIGHT_THREATS);
		public static final PacketCodec<PacketByteBuf, FutureSightThreatsPayload> CODEC = PayloadCodecs.codec(
				(buf, payload) -> PayloadCodecs.writeThreats(buf, payload.message.threats()),
				buf -> new FutureSightThreatsPayload(new FutureSightMessages.Threats(PayloadCodecs.readThreats(buf))));
		@Override public Id<? extends CustomPayload> getId() { return ID; }
	}

	public record JudgementStartPayload(JudgementCutMessages.Start message) implements CustomPayload {
		public static final Id<JudgementStartPayload> ID = id(PacketChannels.JUDGEMENT_VISUAL_START);
		public static final PacketCodec<PacketByteBuf, JudgementStartPayload> CODEC = PayloadCodecs.codec(
				(buf, payload) -> PayloadCodecs.writePosition(buf, payload.message.center()),
				buf -> new JudgementStartPayload(new JudgementCutMessages.Start(PayloadCodecs.readPosition(buf))));
		@Override public Id<? extends CustomPayload> getId() { return ID; }
	}

	public record JudgementReleasePayload(JudgementCutMessages.Release message) implements CustomPayload {
		public static final Id<JudgementReleasePayload> ID = id(PacketChannels.JUDGEMENT_VISUAL_RELEASE);
		public static final PacketCodec<PacketByteBuf, JudgementReleasePayload> CODEC = PayloadCodecs.codec(
				(buf, payload) -> { buf.writeDouble(payload.message.radius()); buf.writeLong(payload.message.seed()); buf.writeInt(payload.message.slashCount()); },
				buf -> new JudgementReleasePayload(new JudgementCutMessages.Release(buf.readDouble(), buf.readLong(), buf.readInt())));
		@Override public Id<? extends CustomPayload> getId() { return ID; }
	}

	public record JudgementClearPayload(JudgementCutMessages.Clear message) implements CustomPayload {
		public static final Id<JudgementClearPayload> ID = id(PacketChannels.JUDGEMENT_VISUAL_CLEAR);
		public static final PacketCodec<PacketByteBuf, JudgementClearPayload> CODEC = PayloadCodecs.codec(
				(buf, payload) -> {}, buf -> new JudgementClearPayload(new JudgementCutMessages.Clear()));
		@Override public Id<? extends CustomPayload> getId() { return ID; }
	}

	public record JudgementMonochromePayload(JudgementCutMessages.Monochrome message) implements CustomPayload {
		public static final Id<JudgementMonochromePayload> ID = id(PacketChannels.JUDGEMENT_MONOCHROME);
		public static final PacketCodec<PacketByteBuf, JudgementMonochromePayload> CODEC = PayloadCodecs.codec(
				(buf, payload) -> buf.writeBoolean(payload.message.active()),
				buf -> new JudgementMonochromePayload(new JudgementCutMessages.Monochrome(buf.readBoolean())));
		@Override public Id<? extends CustomPayload> getId() { return ID; }
	}

	public record TheWorldStatePayload(TheWorldMessages.State message) implements CustomPayload {
		public static final Id<TheWorldStatePayload> ID = id(PacketChannels.THE_WORLD_STATE);
		public static final PacketCodec<PacketByteBuf, TheWorldStatePayload> CODEC = PayloadCodecs.codec(
				(buf, payload) -> { PayloadCodecs.writeDurations(buf, payload.message.activeDurations()); buf.writeVarInt(payload.message.maxDurationTicks()); },
				buf -> new TheWorldStatePayload(new TheWorldMessages.State(PayloadCodecs.readDurations(buf), buf.readVarInt())));
		@Override public Id<? extends CustomPayload> getId() { return ID; }
	}

	public record TheWorldActivationPayload(TheWorldMessages.Activation message) implements CustomPayload {
		public static final Id<TheWorldActivationPayload> ID = id(PacketChannels.THE_WORLD_ACTIVATE);
		public static final PacketCodec<PacketByteBuf, TheWorldActivationPayload> CODEC = PayloadCodecs.codec(
				(buf, payload) -> { buf.writeUuid(payload.message.activatorId()); buf.writeBoolean(payload.message.globalTransition()); },
				buf -> new TheWorldActivationPayload(new TheWorldMessages.Activation(buf.readUuid(), buf.readBoolean())));
		@Override public Id<? extends CustomPayload> getId() { return ID; }
	}

	public record TheWorldStoredHitPayload(TheWorldMessages.StoredHit message) implements CustomPayload {
		public static final Id<TheWorldStoredHitPayload> ID = id(PacketChannels.THE_WORLD_HIT);
		public static final PacketCodec<PacketByteBuf, TheWorldStoredHitPayload> CODEC = PayloadCodecs.codec(
				(buf, payload) -> { buf.writeUuid(payload.message.hitId()); buf.writeUuid(payload.message.targetId()); buf.writeUuid(payload.message.attackerId()); PayloadCodecs.writePosition(buf, payload.message.impactPosition()); PayloadCodecs.writePosition(buf, payload.message.attackDirection()); },
				buf -> new TheWorldStoredHitPayload(new TheWorldMessages.StoredHit(buf.readUuid(), buf.readUuid(), buf.readUuid(), PayloadCodecs.readPosition(buf), PayloadCodecs.readPosition(buf))));
		@Override public Id<? extends CustomPayload> getId() { return ID; }
	}

	public record TheWorldResolveHitPayload(TheWorldMessages.ResolveHit message) implements CustomPayload {
		public static final Id<TheWorldResolveHitPayload> ID = id(PacketChannels.THE_WORLD_RESOLVE);
		public static final PacketCodec<PacketByteBuf, TheWorldResolveHitPayload> CODEC = PayloadCodecs.codec(
				(buf, payload) -> { buf.writeUuid(payload.message.hitId()); buf.writeVarInt(payload.message.sequenceIndex()); buf.writeVarInt(payload.message.totalHits()); },
				buf -> new TheWorldResolveHitPayload(new TheWorldMessages.ResolveHit(buf.readUuid(), buf.readVarInt(), buf.readVarInt())));
		@Override public Id<? extends CustomPayload> getId() { return ID; }
	}

	public record TimeFieldStartedPayload(TimeFieldMessages.Started message) implements CustomPayload {
		public static final Id<TimeFieldStartedPayload> ID = id(PacketChannels.START_TIME_FIELD);
		public static final PacketCodec<PacketByteBuf, TimeFieldStartedPayload> CODEC = PayloadCodecs.codec(
				(buf, payload) -> { buf.writeUuid(payload.message.ownerId()); PayloadCodecs.writePosition(buf, payload.message.center()); buf.writeDouble(payload.message.radius()); buf.writeInt(payload.message.durationTicks()); },
				buf -> new TimeFieldStartedPayload(new TimeFieldMessages.Started(buf.readUuid(), PayloadCodecs.readPosition(buf), buf.readDouble(), buf.readInt())));
		@Override public Id<? extends CustomPayload> getId() { return ID; }
	}

	public record TimeFieldRemovedPayload(TimeFieldMessages.Removed message) implements CustomPayload {
		public static final Id<TimeFieldRemovedPayload> ID = id(PacketChannels.REMOVE_TIME_FIELD);
		public static final PacketCodec<PacketByteBuf, TimeFieldRemovedPayload> CODEC = PayloadCodecs.codec(
				(buf, payload) -> buf.writeUuid(payload.message.ownerId()),
				buf -> new TimeFieldRemovedPayload(new TimeFieldMessages.Removed(buf.readUuid())));
		@Override public Id<? extends CustomPayload> getId() { return ID; }
	}

	public record TimeRewindEffectPayload(TimeRewindMessages.Effect message) implements CustomPayload {
		public static final Id<TimeRewindEffectPayload> ID = id(PacketChannels.TIME_REWIND_EFFECT);
		public static final PacketCodec<PacketByteBuf, TimeRewindEffectPayload> CODEC = PayloadCodecs.codec(
				(buf, payload) -> { buf.writeUuid(payload.message.playerId()); PayloadCodecs.writePosition(buf, payload.message.origin()); PayloadCodecs.writePosition(buf, payload.message.destination()); buf.writeVarInt(payload.message.durationTicks()); },
				buf -> new TimeRewindEffectPayload(new TimeRewindMessages.Effect(buf.readUuid(), PayloadCodecs.readPosition(buf), PayloadCodecs.readPosition(buf), buf.readVarInt())));
		@Override public Id<? extends CustomPayload> getId() { return ID; }
	}
}
