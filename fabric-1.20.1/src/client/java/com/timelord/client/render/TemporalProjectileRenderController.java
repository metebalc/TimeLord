package com.timelord.client.render;

import com.timelord.client.time.MadeInHeavenClientState;
import com.timelord.client.time.MadeInHeavenPresentationSettings;
import com.timelord.client.time.TheWorldClientState;
import com.timelord.mih.RelativeTemporalFactor;
import com.timelord.mih.ProjectileTemporalPolicy;
import com.timelord.mih.TemporalEchoPolicy;
import com.timelord.mih.TemporalRenderMode;
import com.timelord.mih.TemporalSkipCadence;
import com.timelord.mih.TemporalState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Observer-relative projectile trails built only from actual client entity positions. */
public final class TemporalProjectileRenderController {
    private static final int MAX_HISTORY_SAMPLES = 8;
    private static final int STATIONARY_SETTLE_TICKS = 3;
    private static final int STALE_HISTORY_TICKS = 40;
    private static final double MOVEMENT_EPSILON_SQUARED = 0.0004D;
    private static final double DISCONTINUITY_DISTANCE_SQUARED = 64.0D;
    private static final double MAX_ECHO_DISTANCE_SQUARED = 64.0D * 64.0D;

    private static final Map<UUID, ProjectileHistory> HISTORIES = new HashMap<>();
    private static final Map<UUID, ProjectilePresentation> PRESENTATIONS = new HashMap<>();
    private static long clientTick;
    private static int echoPassDepth;

    private TemporalProjectileRenderController() {}

    public static void tick() {
        clientTick++;
        PRESENTATIONS.clear();
        HISTORIES.entrySet().removeIf(
                entry -> clientTick - entry.getValue().lastSeenTick > STALE_HISTORY_TICKS);
    }

    public static void record(ProjectileEntity projectile) {
        ProjectileHistory history = HISTORIES.computeIfAbsent(
                projectile.getUuid(), ignored -> new ProjectileHistory());
        Vec3d position = projectile.getPos();
        PositionSample previous = history.samples.peekLast();
        if (previous != null && previous.tick == clientTick) {
            history.samples.removeLast();
            previous = history.samples.peekLast();
        }

        if (previous != null) {
            double distanceSquared = previous.position.squaredDistanceTo(position);
            if (distanceSquared > DISCONTINUITY_DISTANCE_SQUARED) {
                history.samples.clear();
                history.lastMovementTick = Long.MIN_VALUE / 2L;
                history.movementMagnitude = 0.0D;
            } else if (distanceSquared > MOVEMENT_EPSILON_SQUARED) {
                long elapsed = Math.max(1L, clientTick - previous.tick);
                history.lastMovementTick = clientTick;
                history.movementMagnitude = Math.sqrt(distanceSquared) / elapsed;
            }
        }

        history.samples.addLast(new PositionSample(position, clientTick));
        history.lastSeenTick = clientTick;
        while (history.samples.size() > MAX_HISTORY_SAMPLES)
            history.samples.removeFirst();
    }

    public static void prepare(ProjectileEntity projectile, float tickDelta) {
        if (!isEchoPass())
            PRESENTATIONS.put(projectile.getUuid(), presentationFor(projectile, tickDelta));
    }

    public static boolean beginMainRender(
            ProjectileEntity projectile,
            float tickDelta,
            MatrixStack matrices
    ) {
        ProjectilePresentation presentation = PRESENTATIONS.getOrDefault(
                projectile.getUuid(), ProjectilePresentation.normal());
        if (!presentation.render)
            return false;
        if (presentation.position == null)
            return true;

        Vec3d current = new Vec3d(
                MathHelper.lerp(tickDelta, projectile.prevX, projectile.getX()),
                MathHelper.lerp(tickDelta, projectile.prevY, projectile.getY()),
                MathHelper.lerp(tickDelta, projectile.prevZ, projectile.getZ())
        );
        matrices.push();
        matrices.translate(
                presentation.position.x - current.x,
                presentation.position.y - current.y,
                presentation.position.z - current.z
        );
        presentation.matrixPushed = true;
        return true;
    }

    public static void endMainRender(ProjectileEntity projectile, MatrixStack matrices) {
        ProjectilePresentation presentation = PRESENTATIONS.get(projectile.getUuid());
        if (presentation != null && presentation.matrixPushed)
            matrices.pop();
    }

    public static List<RenderEcho> echoesFor(ProjectileEntity projectile, float tickDelta) {
        ProjectilePresentation presentation = PRESENTATIONS.get(projectile.getUuid());
        if (presentation == null || presentation.echoes.isEmpty())
            return Collections.emptyList();

        Vec3d current = new Vec3d(
                MathHelper.lerp(tickDelta, projectile.prevX, projectile.getX()),
                MathHelper.lerp(tickDelta, projectile.prevY, projectile.getY()),
                MathHelper.lerp(tickDelta, projectile.prevZ, projectile.getZ())
        );
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

    public static void clear() {
        HISTORIES.clear();
        PRESENTATIONS.clear();
        clientTick = 0L;
        echoPassDepth = 0;
    }

    private static ProjectilePresentation presentationFor(
            ProjectileEntity projectile,
            float tickDelta
    ) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null)
            return ProjectilePresentation.normal();
        ProjectileHistory history = HISTORIES.get(projectile.getUuid());
        if (history == null || history.samples.size() < 2)
            return ProjectilePresentation.normal();

        RelativeTemporalFactor relative = RelativeTemporalFactor.between(
                playerState(client.player.getUuid()), projectileState(projectile));
        if (relative.relation() != RelativeTemporalFactor.Relation.RUNNING)
            return ProjectilePresentation.normal();

        long ticksSinceMovement = Math.max(0L, clientTick - history.lastMovementTick);
        double settling = ticksSinceMovement > STATIONARY_SETTLE_TICKS
                ? 0.0D
                : 1.0D - ticksSinceMovement / (double) (STATIONARY_SETTLE_TICKS + 1);
        TemporalRenderMode mode = TemporalRenderMode.select(
                relative, history.movementMagnitude * settling);
        if (mode == TemporalRenderMode.TEMPORAL_SKIP
                && !MadeInHeavenPresentationSettings.allowTemporalSkipping())
            mode = TemporalRenderMode.AFTERIMAGE;
        if (mode == TemporalRenderMode.NORMAL)
            return ProjectilePresentation.normal();
        if (mode == TemporalRenderMode.SLOW_MOTION)
            return new ProjectilePresentation(
                    true, Collections.emptyList(), slowPosition(history, tickDelta));

        boolean resolve = mode != TemporalRenderMode.TEMPORAL_SKIP
                || TemporalSkipCadence.shouldResolve(
                        clientTick,
                        projectile.getUuid().hashCode() & Integer.MAX_VALUE,
                        relative.factor()
                );
        List<EchoSample> echoes = client.player.squaredDistanceTo(projectile)
                <= MAX_ECHO_DISTANCE_SQUARED
                ? selectEchoes(history, mode, resolve)
                : Collections.emptyList();
        return new ProjectilePresentation(resolve, echoes, null);
    }

    private static Vec3d slowPosition(ProjectileHistory history, float tickDelta) {
        if (history.samples.size() < 2)
            return null;
        Iterator<PositionSample> iterator = history.samples.descendingIterator();
        PositionSample newest = iterator.next();
        PositionSample previous = iterator.next();
        double progress = ProjectileTemporalPolicy.historicalInterpolationProgress(
                clientTick, newest.tick, previous.tick, tickDelta);
        return previous.position.lerp(newest.position, progress);
    }

    private static TemporalState playerState(UUID playerId) {
        if (!TheWorldClientState.isTimeStopped())
            return TemporalState.running(MadeInHeavenClientState.perceptualScaleFor(playerId));
        return TheWorldClientState.canMove(playerId)
                ? TemporalState.running(1.0D)
                : TemporalState.stopped();
    }

    private static TemporalState projectileState(ProjectileEntity projectile) {
        if (TheWorldClientState.isTimeStopped())
            return TemporalState.stopped();
        Entity owner = projectile.getOwner();
        boolean adaptedOwner = owner instanceof PlayerEntity
                && MadeInHeavenClientState.isActiveUser(owner.getUuid());
        return TemporalState.running(adaptedOwner
                ? 1.0D
                : MadeInHeavenClientState.perceptualScaleFor(projectile.getUuid()));
    }

    private static List<EchoSample> selectEchoes(
            ProjectileHistory history,
            TemporalRenderMode mode,
            boolean resolvingSkipFrame
    ) {
        int count = TemporalEchoPolicy.echoCount(mode, resolvingSkipFrame);
        if (count <= 0)
            return Collections.emptyList();

        PositionSample newest = history.samples.peekLast();
        List<EchoSample> echoes = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            int desiredAge = TemporalEchoPolicy.desiredSampleAgeTicks(index);
            PositionSample selected = null;
            Iterator<PositionSample> descending = history.samples.descendingIterator();
            while (descending.hasNext()) {
                PositionSample candidate = descending.next();
                long age = clientTick - candidate.tick;
                if (age > TemporalEchoPolicy.MAX_SAMPLE_AGE_TICKS)
                    break;
                if (age >= desiredAge) {
                    selected = candidate;
                    break;
                }
            }
            if (selected == null
                    || selected.position.squaredDistanceTo(newest.position)
                        <= MOVEMENT_EPSILON_SQUARED
                    || containsPosition(echoes, selected.position))
                continue;
            echoes.add(new EchoSample(
                    selected.position,
                    TemporalEchoPolicy.alpha(index, mode, resolvingSkipFrame)
            ));
        }
        return List.copyOf(echoes);
    }

    private static boolean containsPosition(List<EchoSample> echoes, Vec3d position) {
        return echoes.stream().anyMatch(
                echo -> echo.position.squaredDistanceTo(position) <= MOVEMENT_EPSILON_SQUARED);
    }

    private static final class ProjectileHistory {
        private final Deque<PositionSample> samples = new ArrayDeque<>();
        private long lastMovementTick = Long.MIN_VALUE / 2L;
        private double movementMagnitude;
        private long lastSeenTick;
    }

    private record PositionSample(Vec3d position, long tick) {}
    private record EchoSample(Vec3d position, float alpha) {}
    public record RenderEcho(Vec3d offset, float alpha) {}

    private static final class ProjectilePresentation {
        private final boolean render;
        private final List<EchoSample> echoes;
        private final Vec3d position;
        private boolean matrixPushed;

        private ProjectilePresentation(boolean render, List<EchoSample> echoes, Vec3d position) {
            this.render = render;
            this.echoes = echoes;
            this.position = position;
        }

        private static ProjectilePresentation normal() {
            return new ProjectilePresentation(true, Collections.emptyList(), null);
        }
    }
}
