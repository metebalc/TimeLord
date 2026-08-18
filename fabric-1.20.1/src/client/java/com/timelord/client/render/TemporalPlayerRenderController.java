package com.timelord.client.render;

import com.timelord.client.time.MadeInHeavenClientState;
import com.timelord.client.time.MadeInHeavenPresentationSettings;
import com.timelord.client.time.TheWorldClientState;
import com.timelord.mih.RelativeTemporalFactor;
import com.timelord.mih.TemporalEchoPolicy;
import com.timelord.mih.TemporalRenderMode;
import com.timelord.mih.TemporalSkipCadence;
import com.timelord.mih.TemporalState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Uses only client-received entity positions to build observer-relative presentation. */
public final class TemporalPlayerRenderController {
    private static final int MAX_HISTORY_SAMPLES = 48;
    private static final int STATIONARY_SETTLE_TICKS = 4;
    private static final int STALE_HISTORY_TICKS = 40;
    private static final double MOVEMENT_EPSILON_SQUARED = 0.0004D;
    private static final double DISCONTINUITY_DISTANCE_SQUARED = 64.0D;
    private static final double MAX_ECHO_DISTANCE_SQUARED = 48.0D * 48.0D;

    private static final Map<UUID, PlayerHistory> HISTORIES = new HashMap<>();
    private static final Map<UUID, FramePresentation> FRAME_PRESENTATIONS = new HashMap<>();
    private static long clientTick;
    private static int echoPassDepth;

    private TemporalPlayerRenderController() {}

    public static void tick() {
        clientTick++;
        FRAME_PRESENTATIONS.clear();

        Iterator<Map.Entry<UUID, PlayerHistory>> iterator = HISTORIES.entrySet().iterator();
        while (iterator.hasNext()) {
            PlayerHistory history = iterator.next().getValue();
            if (clientTick - history.lastSeenTick > STALE_HISTORY_TICKS)
                iterator.remove();
        }
    }

    public static void record(PlayerEntity player) {
        PlayerHistory history = HISTORIES.computeIfAbsent(
                player.getUuid(),
                ignored -> new PlayerHistory()
        );
        Vec3d position = player.getPos();
        PositionSample previous = history.samples.peekLast();

        if (previous != null && previous.clientTick == clientTick) {
            history.samples.removeLast();
            previous = history.samples.peekLast();
        }
        if (previous != null) {
            double distanceSquared = previous.position.squaredDistanceTo(position);
            if (distanceSquared > DISCONTINUITY_DISTANCE_SQUARED) {
                history.samples.clear();
                history.lastMovementTick = Long.MIN_VALUE / 2L;
                history.lastMovementMagnitude = 0.0D;
                history.resetPlayback();
            } else if (distanceSquared > MOVEMENT_EPSILON_SQUARED) {
                long elapsedTicks = Math.max(1L, clientTick - previous.clientTick);
                history.lastMovementTick = clientTick;
                history.lastMovementMagnitude = Math.sqrt(distanceSquared) / elapsedTicks;
            }
        }

        history.samples.addLast(new PositionSample(position, clientTick));
        history.lastSeenTick = clientTick;
        while (history.samples.size() > MAX_HISTORY_SAMPLES)
            history.samples.removeFirst();
    }

    public static void prepareRender(AbstractClientPlayerEntity entity, float tickDelta) {
        if (!isEchoPass())
            FRAME_PRESENTATIONS.put(entity.getUuid(), presentationFor(entity, tickDelta));
    }

    /** Returns whether vanilla should render this player on the current frame. */
    public static boolean beginPreparedRender(
            AbstractClientPlayerEntity entity,
            float tickDelta,
            MatrixStack matrices
    ) {
        FramePresentation presentation = FRAME_PRESENTATIONS.getOrDefault(
                entity.getUuid(), FramePresentation.normal());
        if (!presentation.render)
            return false;

        if (presentation.position == null)
            return true;

        double currentX = MathHelper.lerp(tickDelta, entity.prevX, entity.getX());
        double currentY = MathHelper.lerp(tickDelta, entity.prevY, entity.getY());
        double currentZ = MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ());
        matrices.push();
        matrices.translate(
                presentation.position.x - currentX,
                presentation.position.y - currentY,
                presentation.position.z - currentZ
        );
        presentation.matrixPushed = true;
        return true;
    }

    public static List<RenderEcho> echoesFor(
            AbstractClientPlayerEntity entity,
            float tickDelta
    ) {
        FramePresentation presentation = FRAME_PRESENTATIONS.get(entity.getUuid());
        if (presentation == null || presentation.echoes.isEmpty())
            return Collections.emptyList();

        double currentX = MathHelper.lerp(tickDelta, entity.prevX, entity.getX());
        double currentY = MathHelper.lerp(tickDelta, entity.prevY, entity.getY());
        double currentZ = MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ());
        Vec3d current = new Vec3d(currentX, currentY, currentZ);
        List<RenderEcho> echoes = new ArrayList<>(presentation.echoes.size());
        for (EchoSample echo : presentation.echoes) {
            Vec3d offset = echo.position.subtract(current);
            float alpha = MadeInHeavenPresentationSettings.echoAlpha(echo.alpha);
            if (alpha > 0.001F && offset.lengthSquared() > MOVEMENT_EPSILON_SQUARED)
                echoes.add(new RenderEcho(offset, alpha));
        }
        return List.copyOf(echoes);
    }

    public static void beginEchoPass() {
        echoPassDepth++;
    }

    public static void endEchoPass() {
        echoPassDepth = Math.max(0, echoPassDepth - 1);
    }

    public static boolean isEchoPass() {
        return echoPassDepth > 0;
    }

    public static void endRender(AbstractClientPlayerEntity entity, MatrixStack matrices) {
        FramePresentation presentation = FRAME_PRESENTATIONS.get(entity.getUuid());
        if (presentation != null && presentation.matrixPushed)
            matrices.pop();
    }

    public static boolean shouldSuppressShadow(Entity entity) {
        if (!(entity instanceof PlayerEntity))
            return false;
        FramePresentation presentation = FRAME_PRESENTATIONS.get(entity.getUuid());
        return presentation != null && presentation.suppressShadow;
    }

    public static void clear() {
        HISTORIES.clear();
        FRAME_PRESENTATIONS.clear();
        clientTick = 0L;
        echoPassDepth = 0;
    }

    private static FramePresentation presentationFor(
            AbstractClientPlayerEntity entity,
            float tickDelta
    ) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null
                || client.player.getUuid().equals(entity.getUuid()))
            return FramePresentation.normal();

        PlayerHistory history = HISTORIES.get(entity.getUuid());
        if (history == null || history.samples.isEmpty())
            return FramePresentation.normal();

        TemporalState viewerTime = temporalStateFor(client.player.getUuid());
        TemporalState entityTime = temporalStateFor(entity.getUuid());
        RelativeTemporalFactor relative = RelativeTemporalFactor.between(viewerTime, entityTime);
        if (relative.relation() != RelativeTemporalFactor.Relation.RUNNING)
            return FramePresentation.normal();

        long ticksSinceMovement = Math.max(0L, clientTick - history.lastMovementTick);
        double settling = ticksSinceMovement > STATIONARY_SETTLE_TICKS
                ? 0.0D
                : 1.0D - ticksSinceMovement / (double) (STATIONARY_SETTLE_TICKS + 1);
        double movementMagnitude = history.lastMovementMagnitude * settling;
        TemporalRenderMode mode = TemporalRenderMode.select(relative, movementMagnitude);
        if (mode == TemporalRenderMode.TEMPORAL_SKIP
                && !MadeInHeavenPresentationSettings.allowTemporalSkipping())
            mode = TemporalRenderMode.AFTERIMAGE;
        if (mode == TemporalRenderMode.NORMAL) {
            history.resetPlayback();
            return FramePresentation.normal();
        }
        if (mode == TemporalRenderMode.SLOW_MOTION)
            return new FramePresentation(
                    true,
                    false,
                    slowPosition(history, relative.factor(), tickDelta),
                    Collections.emptyList()
            );

        boolean resolve = true;
        if (mode == TemporalRenderMode.TEMPORAL_SKIP) {
            int phase = entity.getUuid().hashCode() & Integer.MAX_VALUE;
            resolve = TemporalSkipCadence.shouldResolve(clientTick, phase, relative.factor());
        }

        PositionSample sample = history.samples.peekLast();
        List<EchoSample> echoes = client.player.squaredDistanceTo(entity) <= MAX_ECHO_DISTANCE_SQUARED
                ? selectEchoes(history, mode, resolve)
                : Collections.emptyList();
        return new FramePresentation(
                resolve,
                mode == TemporalRenderMode.TEMPORAL_SKIP && !resolve,
                resolve ? sample.position : null,
                echoes
        );
    }

    private static TemporalState temporalStateFor(UUID playerId) {
        if (!TheWorldClientState.isTimeStopped())
            return TemporalState.running(MadeInHeavenClientState.perceptualScaleFor(playerId));
        if (TheWorldClientState.canMove(playerId))
            return TemporalState.running(1.0D);

        double resistance = MadeInHeavenClientState.theWorldResistanceFor(playerId);
        return resistance > 0.0D
                ? TemporalState.running(resistance)
                : TemporalState.stopped();
    }

    /**
     * Advances a bounded playback cursor through already received samples. The cursor
     * can lag but never move ahead of the newest authoritative client position.
     */
    private static Vec3d slowPosition(
            PlayerHistory history,
            double perceptualScale,
            float tickDelta
    ) {
        if (history.samples.size() < 2)
            return null;

        PositionSample oldest = history.samples.peekFirst();
        PositionSample newest = history.samples.peekLast();
        if (!Double.isFinite(history.playbackTick)) {
            history.playbackTick = newest.clientTick;
            history.lastPlaybackAdvanceTick = clientTick;
        } else if (history.lastPlaybackAdvanceTick != clientTick) {
            long elapsed = Math.max(1L, clientTick - history.lastPlaybackAdvanceTick);
            history.playbackTick += elapsed * Math.max(0.01D, Math.min(1.0D, perceptualScale));
            history.lastPlaybackAdvanceTick = clientTick;
        }

        history.playbackTick = Math.max(oldest.clientTick,
                Math.min(newest.clientTick, history.playbackTick));
        double sampleTime = Math.min(
                newest.clientTick,
                history.playbackTick + tickDelta * Math.max(0.01D, perceptualScale)
        );

        PositionSample before = oldest;
        PositionSample after = newest;
        for (PositionSample sample : history.samples) {
            if (sample.clientTick <= sampleTime)
                before = sample;
            if (sample.clientTick >= sampleTime) {
                after = sample;
                break;
            }
        }

        long duration = after.clientTick - before.clientTick;
        if (duration <= 0L)
            return before.position;
        double progress = (sampleTime - before.clientTick) / duration;
        return before.position.lerp(after.position, progress);
    }

    private static List<EchoSample> selectEchoes(
            PlayerHistory history,
            TemporalRenderMode mode,
            boolean resolvingSkipFrame
    ) {
        int count = TemporalEchoPolicy.echoCount(mode, resolvingSkipFrame);
        if (count <= 0 || history.samples.size() < 2)
            return Collections.emptyList();

        PositionSample newest = history.samples.peekLast();
        List<EchoSample> echoes = new ArrayList<>(count);
        for (int echoIndex = 0; echoIndex < count; echoIndex++) {
            int desiredAge = TemporalEchoPolicy.desiredSampleAgeTicks(echoIndex);
            PositionSample selected = null;
            Iterator<PositionSample> descending = history.samples.descendingIterator();
            while (descending.hasNext()) {
                PositionSample candidate = descending.next();
                long age = clientTick - candidate.clientTick;
                if (age > TemporalEchoPolicy.MAX_SAMPLE_AGE_TICKS)
                    break;
                if (age >= desiredAge) {
                    selected = candidate;
                    break;
                }
            }

            if (selected == null
                    || selected.position.squaredDistanceTo(newest.position) <= MOVEMENT_EPSILON_SQUARED
                    || containsPosition(echoes, selected.position))
                continue;

            echoes.add(new EchoSample(
                    selected.position,
                    TemporalEchoPolicy.alpha(echoIndex, mode, resolvingSkipFrame)
            ));
        }
        return List.copyOf(echoes);
    }

    private static boolean containsPosition(List<EchoSample> echoes, Vec3d position) {
        return echoes.stream().anyMatch(
                echo -> echo.position.squaredDistanceTo(position) <= MOVEMENT_EPSILON_SQUARED);
    }

    private static final class PlayerHistory {
        private final Deque<PositionSample> samples = new ArrayDeque<>();
        private long lastMovementTick = Long.MIN_VALUE / 2L;
        private double lastMovementMagnitude;
        private long lastSeenTick;
        private double playbackTick = Double.NaN;
        private long lastPlaybackAdvanceTick;

        private void resetPlayback() {
            playbackTick = Double.NaN;
            lastPlaybackAdvanceTick = clientTick;
        }
    }

    private record PositionSample(Vec3d position, long clientTick) {}

    private record EchoSample(Vec3d position, float alpha) {}

    public record RenderEcho(Vec3d offset, float alpha) {}

    private static final class FramePresentation {
        private final boolean render;
        private final boolean suppressShadow;
        private final Vec3d position;
        private final List<EchoSample> echoes;
        private boolean matrixPushed;

        private FramePresentation(
                boolean render,
                boolean suppressShadow,
                Vec3d position,
                List<EchoSample> echoes
        ) {
            this.render = render;
            this.suppressShadow = suppressShadow;
            this.position = position;
            this.echoes = echoes;
        }

        private static FramePresentation normal() {
            return new FramePresentation(true, false, null, Collections.emptyList());
        }
    }
}
