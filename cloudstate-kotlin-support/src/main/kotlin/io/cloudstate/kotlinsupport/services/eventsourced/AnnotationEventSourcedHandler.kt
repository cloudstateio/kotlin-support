package io.cloudstate.kotlinsupport.services.eventsourced

import io.cloudstate.kotlinsupport.annotations.CommandHandler
import io.cloudstate.kotlinsupport.annotations.EventHandler
import io.cloudstate.kotlinsupport.annotations.Snapshot
import io.cloudstate.kotlinsupport.annotations.SnapshotHandler

class AnnotationEventSourcedHandler(private val eventSourcedEntity: AnnotationEventSourcedEntity): EventSourcedEntityHandlerContext {

    val annotations: List<Annotation> = eventSourcedEntity.javaClass.annotations.filter {
        it is CommandHandler || it is EventHandler || it is Snapshot || it is SnapshotHandler
    }

    init {
        // process annotations in eventSourcedEntity
    }

    override fun handleEvent(event: com.google.protobuf.Any?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun handleCommand(command: com.google.protobuf.Any?): com.google.protobuf.Any? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun handleSnapshot(snapshot: com.google.protobuf.Any?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun snapshot(): com.google.protobuf.Any? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun fail(errorMessage: String?): RuntimeException? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun forward(to: Any) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}