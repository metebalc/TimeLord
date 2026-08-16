package com.timelord.client;

import com.timelord.client.judgement.SlashInstance;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class ClientJudgementCut {
    private static boolean charging;
    private static Vec3d center;

    private static long startTime;
    private static long releaseTime;

    private static double finalRadius;

    private static boolean monochrome;

    private static boolean suspendedCutsActive;
    private static final List<SlashInstance> SLASHES = new ArrayList<>();

    private ClientJudgementCut() {}

    public static void start(Vec3d newCenter) {
        center = newCenter;
        startTime = System.currentTimeMillis();
        releaseTime = 0L;
        charging = true;
        suspendedCutsActive = false;
        finalRadius = 0.0D;
        SLASHES.clear();
    }

    public static void release(double radius, long seed, int slashCount) {
        charging = false;
        finalRadius = radius;
        releaseTime = System.currentTimeMillis();
        suspendedCutsActive = true;
        generateSlashes(seed, slashCount, radius);
    }

    public static void clear() {
        charging = false;
        center = null;
        finalRadius = 0.0D;
        monochrome = false;
        suspendedCutsActive = false;
        releaseTime = 0L;
        SLASHES.clear();
    }

    private static void generateSlashes(long seed, int slashCount, double radius) {
        SLASHES.clear();

        if (center == null)
            return;

        Random random = new Random(seed);

        for (int i = 0; i < slashCount; i++) {
            Vec3d offset = randomPointInsideSphere(random, radius * 0.80D);
            Vec3d position = center.add(offset);

            float yaw = random.nextFloat() * 360.0F;
            float pitch = -70.0F + random.nextFloat() * 140.0F;
            float roll = random.nextFloat() * 360.0F;
            float length = 1.8F + random.nextFloat() * 3.2F;
            float width = 0.08F + random.nextFloat() * 0.16F;
            float intensity = 0.75F + random.nextFloat() * 0.25F;

            SLASHES.add(
                    new SlashInstance(
                            position,
                            yaw,
                            pitch,
                            roll,
                            length,
                            width,
                            intensity
                    )
            );
        }
    }

    private static Vec3d randomPointInsideSphere(Random random, double radius) {
        double x;
        double y;
        double z;

        do {
            x = (random.nextDouble() * 2.0D - 1.0D) * radius;
            y = (random.nextDouble() * 2.0D - 1.0D) * radius;
            z = (random.nextDouble() * 2.0D - 1.0D) * radius;
        } while (x * x + y * y + z * z > radius * radius);

        return new Vec3d(x, y, z);
    }

    public static boolean exists() {
        return center != null;
    }

    public static boolean isCharging() {
        return charging;
    }

    public static Vec3d getCenter() {
        return center;
    }

    public static long getStartTime() {
        return startTime;
    }

    public static long getReleaseTime() {
        return releaseTime;
    }

    public static double getFinalRadius() {
        return finalRadius;
    }

    public static List<SlashInstance> getSlashes() {
        return Collections.unmodifiableList(SLASHES);
    }

    public static boolean hasSuspendedCuts() {
        return suspendedCutsActive && !SLASHES.isEmpty();
    }

    public static void setMonochrome(boolean active) {
        monochrome = active;
    }

    public static boolean isMonochrome() {
        return monochrome;
    }

}