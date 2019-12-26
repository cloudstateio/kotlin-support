package io.cloudstate.kotlinsupport.services.eventsourced

class AnnotationEventSourcedHandler(private val eventSourcedEntity: AnnotationEventSourcedEntity): EventSourcedEntityHandlerContext {

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