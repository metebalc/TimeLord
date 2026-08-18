package com.timelord.common.network;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PacketChannelsTest {
    @Test
    void declaresEveryChannelOnceAndPreservesLegacyTimeFieldNames() throws IllegalAccessException {
        List<PacketChannels.Channel> channels = Arrays.stream(PacketChannels.class.getFields())
                .filter(field -> Modifier.isStatic(field.getModifiers()))
                .filter(field -> field.getType() == PacketChannels.Channel.class)
                .map(PacketChannelsTest::readChannel)
                .toList();

        assertEquals(25, channels.size());
        assertEquals(25, channels.stream().distinct().count());
        assertEquals("time-lord:made_in_heaven_state", PacketChannels.MADE_IN_HEAVEN_STATE.toString());
        assertEquals("timelord:start_time_field", PacketChannels.START_TIME_FIELD.toString());
        assertEquals("timelord:remove_time_field", PacketChannels.REMOVE_TIME_FIELD.toString());
    }

    private static PacketChannels.Channel readChannel(Field field) {
        try {
            return (PacketChannels.Channel) field.get(null);
        } catch (IllegalAccessException exception) {
            throw new AssertionError(exception);
        }
    }
}
