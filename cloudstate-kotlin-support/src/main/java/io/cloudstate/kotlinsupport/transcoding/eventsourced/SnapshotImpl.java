package io.cloudstate.kotlinsupport.transcoding.eventsourced;

import io.cloudstate.javasupport.eventsourced.Snapshot;

import java.lang.annotation.Annotation;

public final class SnapshotImpl implements Snapshot {

    public Class<? extends Annotation> annotationType() {
        return Snapshot.class;
    }
}
