package io.cloudstate.kotlinsupport.api.eventsourced

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Marks a method as a snapshot handler.
 *
 *
 * If, when recovering an entity, that entity has a snapshot, the snapshot will be passed to a
 * corresponding snapshot handler method whose argument matches its type. The entity must set its
 * current state to that snapshot.
 *
 *
 * An entity may declare more than one snapshot handler if it wants different handling for
 * different types.
 *
 *
 * The snapshot handler method may additionally accept a [SnapshotContext] parameter,
 * allowing it to access context for the snapshot, if required.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(RetentionPolicy.RUNTIME)
annotation class SnapshotHandler