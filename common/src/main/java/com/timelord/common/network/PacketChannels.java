package com.timelord.common.network;

import java.util.Objects;

/** Stable channel names shared by every version-specific networking implementation. */
public final class PacketChannels {
    public static final String MOD_ID = "time-lord";

    public static final Channel ACTIVATE_ABILITY = mod("activate_ability");
    public static final Channel START_CHARGE = mod("start_charge");
    public static final Channel RELEASE_CHARGE = mod("release_charge");
    public static final Channel SWITCH_SLOW_MODE = mod("switch_slow_mode");
    public static final Channel COOLDOWN = mod("cooldown");
    public static final Channel EQUIP_ABILITY = mod("equip_ability");
    public static final Channel ABILITY_LOADOUT = mod("ability_loadout");
    public static final Channel ABILITY_STATE = mod("ability_state");

    public static final Channel THE_WORLD_STATE = mod("the_world_state");
    public static final Channel THE_WORLD_HIT = mod("the_world_hit");
    public static final Channel THE_WORLD_RESOLVE = mod("the_world_resolve");
    public static final Channel THE_WORLD_ACTIVATE = mod("the_world_activate");

    public static final Channel TIME_SHIFT_STATE = mod("time_shift_state");
    public static final Channel TIME_SHIFT_CHARGE_START = mod("time_shift_charge_start");
    public static final Channel TIME_SHIFT_RELEASE = mod("time_shift_release");
    public static final Channel TIME_SHIFT_BURST = mod("time_shift_burst");

    public static final Channel JUDGEMENT_VISUAL_START = mod("judgement_visual_start");
    public static final Channel JUDGEMENT_VISUAL_RELEASE = mod("judgement_visual_release");
    public static final Channel JUDGEMENT_VISUAL_CLEAR = mod("judgement_visual_clear");
    public static final Channel JUDGEMENT_MONOCHROME = mod("judgement_monochrome");

    public static final Channel FUTURE_SIGHT_THREATS = mod("future_sight_threats");
    public static final Channel TIME_REWIND_EFFECT = mod("time_rewind_effect");

    // Preserve the legacy namespace exactly for multiplayer protocol compatibility.
    public static final Channel START_TIME_FIELD = new Channel("timelord", "start_time_field");
    public static final Channel REMOVE_TIME_FIELD = new Channel("timelord", "remove_time_field");

    private PacketChannels() {}

    private static Channel mod(String path) {
        return new Channel(MOD_ID, path);
    }

    public record Channel(String namespace, String path) {
        public Channel {
            Objects.requireNonNull(namespace, "namespace");
            Objects.requireNonNull(path, "path");
        }

        @Override
        public String toString() {
            return namespace + ':' + path;
        }
    }
}
