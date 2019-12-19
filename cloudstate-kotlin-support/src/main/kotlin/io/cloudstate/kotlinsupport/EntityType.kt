package io.cloudstate.kotlinsupport

enum class EntityType(val typeStr: String) {
    Crdt("cloudstate.crdt.Crdt"),
    Crud("cloudstate.crud.Crud"),
    EventSourced("cloudstate.eventsourced.EventSourced"),
}