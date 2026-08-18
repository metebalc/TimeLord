package com.timelord.mih;

/**
 * Observer-relative temporal relationship without dividing through stopped time.
 */
public record RelativeTemporalFactor(Relation relation, double factor) {
    public static RelativeTemporalFactor between(TemporalState viewer, TemporalState entity) {
        if (viewer.isStopped() && entity.isStopped())
            return new RelativeTemporalFactor(Relation.BOTH_STOPPED, 0.0D);
        if (viewer.isStopped())
            return new RelativeTemporalFactor(Relation.VIEWER_STOPPED, 0.0D);
        if (entity.isStopped())
            return new RelativeTemporalFactor(Relation.ENTITY_STOPPED, 0.0D);
        return new RelativeTemporalFactor(Relation.RUNNING, entity.scale() / viewer.scale());
    }

    public enum Relation {
        RUNNING,
        ENTITY_STOPPED,
        VIEWER_STOPPED,
        BOTH_STOPPED
    }
}
