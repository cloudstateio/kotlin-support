package io.cloudstate.kotlinsupport.transcoding.eventsourced;

import io.cloudstate.javasupport.eventsourced.SnapshotHandler;

import java.lang.annotation.Annotation;

public class SnapshotHandlerImpl implements SnapshotHandler {

    public Class<? extends Annotation> annotationType() {
        return SnapshotHandler.class;
    }
}
