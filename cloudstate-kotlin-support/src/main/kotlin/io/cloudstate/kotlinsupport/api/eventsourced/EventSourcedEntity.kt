package io.cloudstate.kotlinsupport.api.eventsourced

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/** An event sourced entity.  */
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
@Retention(RetentionPolicy.RUNTIME)
annotation class EventSourcedEntity(
        /**
         * The name of the persistence id.
         *
         *
         * If not specifed, defaults to the entities unqualified classname. It's strongly recommended
         * that you specify it explicitly.
         */
        val persistenceId: String = "",
        /**
         * Specifies how snapshots of the entity state should be made: Zero means use default from
         * configuration file. (Default) Any negative value means never snapshot. Any positive value means
         * snapshot at-or-after that number of events.
         */
        val snapshotEvery: Int = 0)
