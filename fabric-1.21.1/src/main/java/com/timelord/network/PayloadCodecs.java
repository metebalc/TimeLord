package com.timelord.network;

import com.timelord.common.ability.AbilityId;
import com.timelord.common.model.TemporalPosition;
import com.timelord.common.model.ThreatInfo;
import com.timelord.common.model.ThreatType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class PayloadCodecs {
	private static final int MAX_ABILITY_LIST_SIZE = AbilityId.values().length;
	private static final int MAX_THREATS = 4096;
	private static final int MAX_ACTIVE_PLAYERS = 1024;

	private PayloadCodecs() {
	}

	static <T> PacketCodec<PacketByteBuf, T> codec(Encoder<T> encoder, Decoder<T> decoder) {
		return PacketCodec.of((value, buf) -> encoder.encode(buf, value), decoder::decode);
	}

	static void writeAbility(PacketByteBuf buf, AbilityId ability) {
		buf.writeByte(ability.networkId());
	}

	static AbilityId readAbility(PacketByteBuf buf) {
		int networkId = buf.readByte();
		AbilityId ability = AbilityId.fromNetworkId(networkId);
		if (ability == null) {
			throw new IllegalArgumentException("Unknown ability network ID: " + networkId);
		}
		return ability;
	}

	static void writeAbilities(PacketByteBuf buf, List<AbilityId> abilities) {
		buf.writeVarInt(abilities.size());
		abilities.forEach(ability -> writeAbility(buf, ability));
	}

	static List<AbilityId> readAbilities(PacketByteBuf buf) {
		int size = readBoundedSize(buf, MAX_ABILITY_LIST_SIZE, "ability list");
		List<AbilityId> abilities = new ArrayList<>(size);
		for (int index = 0; index < size; index++) {
			abilities.add(readAbility(buf));
		}
		return abilities;
	}

	static void writePosition(PacketByteBuf buf, TemporalPosition position) {
		buf.writeDouble(position.x());
		buf.writeDouble(position.y());
		buf.writeDouble(position.z());
	}

	static TemporalPosition readPosition(PacketByteBuf buf) {
		return new TemporalPosition(buf.readDouble(), buf.readDouble(), buf.readDouble());
	}

	static void writeThreats(PacketByteBuf buf, List<ThreatInfo> threats) {
		buf.writeVarInt(threats.size());
		for (ThreatInfo threat : threats) {
			buf.writeVarInt(threat.entityId());
			buf.writeByte(threat.type().networkId());
		}
	}

	static List<ThreatInfo> readThreats(PacketByteBuf buf) {
		int size = readBoundedSize(buf, MAX_THREATS, "threat list");
		List<ThreatInfo> threats = new ArrayList<>(size);
		for (int index = 0; index < size; index++) {
			int entityId = buf.readVarInt();
			int networkId = buf.readByte();
			ThreatType type = ThreatType.fromNetworkId(networkId);
			if (type == null) {
				throw new IllegalArgumentException("Unknown threat type network ID: " + networkId);
			}
			threats.add(new ThreatInfo(entityId, type));
		}
		return threats;
	}

	static void writeDurations(PacketByteBuf buf, Map<UUID, Integer> durations) {
		buf.writeVarInt(durations.size());
		durations.forEach((playerId, duration) -> {
			buf.writeUuid(playerId);
			buf.writeVarInt(duration);
		});
	}

	static Map<UUID, Integer> readDurations(PacketByteBuf buf) {
		int size = readBoundedSize(buf, MAX_ACTIVE_PLAYERS, "active-player map");
		Map<UUID, Integer> durations = new LinkedHashMap<>(size);
		for (int index = 0; index < size; index++) {
			durations.put(buf.readUuid(), buf.readVarInt());
		}
		return durations;
	}

	private static int readBoundedSize(PacketByteBuf buf, int maximum, String description) {
		int size = buf.readVarInt();
		if (size < 0 || size > maximum) {
			throw new IllegalArgumentException("Invalid " + description + " size: " + size);
		}
		return size;
	}

	@FunctionalInterface
	interface Encoder<T> {
		void encode(PacketByteBuf buf, T value);
	}

	@FunctionalInterface
	interface Decoder<T> {
		T decode(PacketByteBuf buf);
	}
}
