package io.cloudstate.kotlinsupport.api.eventsourced

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Marks a method as a snapshot method.
 *
 *
 * An event sourced behavior may have at most one of these. When provided, it will be
 * periodically (every *n* events emitted) be invoked to retrieve a snapshot of the current
 * state, to be persisted, so that the event log can be loaded without replaying the entire history.
 *
 *
 * The method must return the current state of the entity.
 *
 *
 * The method may accept a [SnapshotContext] parameter.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(RetentionPolicy.RUNTIME)
annotation class Snapshot