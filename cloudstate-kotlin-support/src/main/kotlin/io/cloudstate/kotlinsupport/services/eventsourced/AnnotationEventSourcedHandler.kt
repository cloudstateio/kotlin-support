package io.cloudstate.kotlinsupport.services.eventsourced

import io.cloudstate.kotlinsupport.annotations.CommandHandler
import io.cloudstate.kotlinsupport.annotations.EventHandler
import io.cloudstate.kotlinsupport.annotations.Snapshot
import io.cloudstate.kotlinsupport.annotations.SnapshotHandler
import java.lang.reflect.Method

class AnnotationEventSourcedHandler(private val eventSourcedEntity: AnnotationEventSourcedEntity): EventSourcedEntityHandlerContext {
    private val eventHandlers: MutableMap<String, MethodInvocable> = mutableMapOf()
    private val commandHandlers: MutableMap<String, MethodInvocable> = mutableMapOf()
    private val snapshotHandlers: MutableMap<String, MethodInvocable> = mutableMapOf()
    private val snapshotsHandlers: MutableMap<String, MethodInvocable> = mutableMapOf()

    val annotations: List<Annotation> = eventSourcedEntity.javaClass.annotations.filter {
        it is CommandHandler || it is EventHandler || it is Snapshot || it is SnapshotHandler
    }

    init {
        // process annotations in eventSourcedEntity
        cachedMethods()
    }

    override fun handleEvent(event: com.google.protobuf.Any?) {
        val typeUrl = event?.typeUrl
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun handleCommand(command: com.google.protobuf.Any?): com.google.protobuf.Any? {
        val typeUrl = command?.typeUrl
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun handleSnapshot(snapshot: com.google.protobuf.Any?) {
        val typeUrl = snapshot?.typeUrl
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

    private fun cachedMethods() {
        this.eventSourcedEntity.javaClass.methods.forEach { method ->
            when {
                method.isAnnotationPresent(CommandHandler::class.java) -> {
                    val parameterType: Class<*> = method.parameterTypes[0]
                    val parameterTypeName: String = parameterType.simpleName
                    commandHandlers[parameterTypeName] = MethodInvocable(parameterTypeName, method, parameterType)
                }
            }

            when {
                method.isAnnotationPresent(EventHandler::class.java) -> {
                    val parameterType: Class<*> = method.parameterTypes[0]
                    val parameterTypeName: String = parameterType.simpleName
                    eventHandlers[parameterTypeName] = MethodInvocable(parameterTypeName, method, parameterType)
                }
            }

            when {
                method.isAnnotationPresent(SnapshotHandler::class.java) -> {
                    val parameterType: Class<*> = method.parameterTypes[0]
                    val parameterTypeName: String = parameterType.simpleName
                    snapshotHandlers[parameterTypeName] = MethodInvocable(parameterTypeName, method, parameterType)
                }
            }

            when {
                method.isAnnotationPresent(Snapshot::class.java) -> {
                    val parameterType: Class<*> = method.parameterTypes[0]
                    val parameterTypeName: String = parameterType.simpleName
                    snapshotsHandlers[parameterTypeName] = MethodInvocable(parameterTypeName, method, parameterType)
                }
            }
        }
    }
}

data class MethodInvocable(val name: String, val method: Method, val parameterType: Class<*>)