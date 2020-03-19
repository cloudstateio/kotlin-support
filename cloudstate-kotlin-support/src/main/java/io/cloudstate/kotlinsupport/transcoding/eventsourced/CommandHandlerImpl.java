package io.cloudstate.kotlinsupport.transcoding.eventsourced;

import io.cloudstate.javasupport.eventsourced.CommandHandler;

import java.lang.annotation.Annotation;

public final class CommandHandlerImpl implements CommandHandler {

    private final String name;

    public CommandHandlerImpl(final String name) {
        this.name = name;
    }

    public Class<? extends Annotation> annotationType() {
        return CommandHandler.class;
    }

    @Override
    public String name() {
        return this.name;
    }
}
