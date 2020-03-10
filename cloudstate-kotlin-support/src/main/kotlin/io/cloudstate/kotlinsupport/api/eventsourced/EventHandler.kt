package io.cloudstate.kotlinsupport.api.eventsourced

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import kotlin.reflect.KClass

/**
 * Marks a method as an event handler.
 *
 *
 * This method will be invoked whenever an event matching this event handlers event class is
 * either replayed on entity recovery, by a command handler.
 *
 *
 * The method may take the event object as a parameter.
 *
 *
 * Methods annotated with this may take an [EventContext].
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(RetentionPolicy.RUNTIME)
annotation class EventHandler
