package io.cloudstate.kotlinsupport.transcoding.eventsourced;

import io.cloudstate.javasupport.eventsourced.EventHandler;

import java.lang.annotation.Annotation;

public class EventHandlerImpl implements EventHandler {

    private final Class<?> eventClass;

    public EventHandlerImpl(final Class<?> eventClass) {
        this.eventClass = eventClass;
    }

    public Class<? extends Annotation> annotationType() {
        return EventHandler.class;
    }

    @Override
    public Class<?> eventClass() {
        return Object.class;
    }
}
