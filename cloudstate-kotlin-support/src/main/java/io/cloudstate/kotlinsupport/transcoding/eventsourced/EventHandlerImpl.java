package io.cloudstate.kotlinsupport.transcoding.eventsourced;

import io.cloudstate.javasupport.eventsourced.EventHandler;

import java.lang.annotation.Annotation;

public final class EventHandlerImpl implements EventHandler {

    public Class<? extends Annotation> annotationType() {
        return EventHandler.class;
    }

    @Override
    public Class<?> eventClass() {
        return Object.class;
    }
}
