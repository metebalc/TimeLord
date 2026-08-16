package com.timelord.client.time;

import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class ClientTimeField {

    private static final Map<UUID, Field> FIELDS = new HashMap<>();

    private ClientTimeField() {
    }

    public static void set(
            UUID owner,
            Vec3d center,
            double radius,
            int durationTicks
    ) {
        FIELDS.put(
                owner,
                new Field(
                        center,
                        radius,
                        durationTicks
                )
        );
    }

    public static void remove(UUID owner) {
        FIELDS.remove(owner);
    }

    public static void clear() {
        FIELDS.clear();
    }

    public static void tick() {
        Iterator<Map.Entry<UUID, Field>> iterator = FIELDS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Field> entry = iterator.next();
            Field field = entry.getValue();

            int ticksLeft = field.ticksLeft() - 1;
            if (ticksLeft <= 0) {
                iterator.remove();
                continue;
            }

            entry.setValue(
                    new Field(
                            field.center(),
                            field.radius(),
                            ticksLeft
                    )
            );
        }
    }

    public static Map<UUID, Field> getFields() {
        return FIELDS;
    }

    public static boolean isActive() {
        return !FIELDS.isEmpty();
    }

    public record Field(
            Vec3d center,
            double radius,
            int ticksLeft
    ) {
    }
}