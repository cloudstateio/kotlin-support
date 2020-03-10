package io.cloudstate.kotlinsupport.transcoding.eventsourced;

import io.cloudstate.javasupport.eventsourced.EventSourcedEntity;

import java.lang.annotation.Annotation;

public final class EventSourcedEntityImpl implements EventSourcedEntity {

    private final String persistenceId;
    private final int snapshotEvery;

    public EventSourcedEntityImpl(final String persistenceId, final int snapshotEvery) {
        this.persistenceId = persistenceId;
        this.snapshotEvery = snapshotEvery;
    }

    public Class<? extends Annotation> annotationType() {
        return EventSourcedEntity.class;
    }

    @Override
    public String persistenceId() {
        return this.persistenceId ;
    }

    @Override
    public int snapshotEvery() {
        return this.snapshotEvery;
    }
}
