package com.timelord.network;

import com.timelord.common.network.PacketChannels;
import com.timelord.common.network.PacketChannels.Channel;
import net.minecraft.util.Identifier;

/** Minecraft 1.20.1 identifiers derived from common channel definitions. */
public final class PacketIds {
    public static final Identifier ACTIVATE_ABILITY = id(PacketChannels.ACTIVATE_ABILITY);
    public static final Identifier START_CHARGE = id(PacketChannels.START_CHARGE);
    public static final Identifier RELEASE_CHARGE = id(PacketChannels.RELEASE_CHARGE);
    public static final Identifier SWITCH_SLOW_MODE = id(PacketChannels.SWITCH_SLOW_MODE);
    public static final Identifier COOLDOWN = id(PacketChannels.COOLDOWN);
    public static final Identifier EQUIP_ABILITY = id(PacketChannels.EQUIP_ABILITY);
    public static final Identifier ABILITY_LOADOUT = id(PacketChannels.ABILITY_LOADOUT);
    public static final Identifier ABILITY_STATE = id(PacketChannels.ABILITY_STATE);
    public static final Identifier THE_WORLD_STATE = id(PacketChannels.THE_WORLD_STATE);
    public static final Identifier THE_WORLD_HIT = id(PacketChannels.THE_WORLD_HIT);
    public static final Identifier THE_WORLD_RESOLVE = id(PacketChannels.THE_WORLD_RESOLVE);
    public static final Identifier THE_WORLD_ACTIVATE = id(PacketChannels.THE_WORLD_ACTIVATE);
    public static final Identifier TIME_SHIFT_STATE = id(PacketChannels.TIME_SHIFT_STATE);
    public static final Identifier TIME_SHIFT_CHARGE_START = id(PacketChannels.TIME_SHIFT_CHARGE_START);
    public static final Identifier TIME_SHIFT_RELEASE = id(PacketChannels.TIME_SHIFT_RELEASE);
    public static final Identifier TIME_SHIFT_BURST = id(PacketChannels.TIME_SHIFT_BURST);
    public static final Identifier JUDGEMENT_VISUAL_START = id(PacketChannels.JUDGEMENT_VISUAL_START);
    public static final Identifier JUDGEMENT_VISUAL_RELEASE = id(PacketChannels.JUDGEMENT_VISUAL_RELEASE);
    public static final Identifier JUDGEMENT_VISUAL_CLEAR = id(PacketChannels.JUDGEMENT_VISUAL_CLEAR);
    public static final Identifier JUDGEMENT_MONOCHROME = id(PacketChannels.JUDGEMENT_MONOCHROME);
    public static final Identifier FUTURE_SIGHT_THREATS = id(PacketChannels.FUTURE_SIGHT_THREATS);
    public static final Identifier TIME_REWIND_EFFECT = id(PacketChannels.TIME_REWIND_EFFECT);
    public static final Identifier START_TIME_FIELD = id(PacketChannels.START_TIME_FIELD);
    public static final Identifier REMOVE_TIME_FIELD = id(PacketChannels.REMOVE_TIME_FIELD);

    private PacketIds() {}

    private static Identifier id(Channel channel) {
        return new Identifier(channel.namespace(), channel.path());
    }
}
