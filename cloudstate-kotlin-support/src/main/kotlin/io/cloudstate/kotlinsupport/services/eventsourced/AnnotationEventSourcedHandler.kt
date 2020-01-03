package io.cloudstate.kotlinsupport.services.eventsourced

import com.google.protobuf.Message
import io.cloudstate.kotlinsupport.annotations.CommandHandler
import io.cloudstate.kotlinsupport.annotations.EventHandler
import io.cloudstate.kotlinsupport.annotations.Snapshot
import io.cloudstate.kotlinsupport.annotations.SnapshotHandler
import io.cloudstate.kotlinsupport.logger
import java.lang.IllegalStateException
import java.lang.reflect.Method

class AnnotationEventSourcedHandler(private val eventSourcedEntity: AnnotationEventSourcedEntity): EventSourcedEntityHandlerContext {
    private val log = logger()

    private val eventHandlers: MutableMap<String, MethodHandler> = mutableMapOf()
    private val commandHandlers: MutableMap<String, MethodHandler> = mutableMapOf()
    private val snapshotHandlers: MutableMap<String, MethodHandler> = mutableMapOf()
    private val snapshotsHandlers: MutableMap<String, MethodHandler> = mutableMapOf()

    private val annotations: List<Annotation> = eventSourcedEntity.javaClass.annotations.filter {
        it is CommandHandler || it is EventHandler || it is Snapshot || it is SnapshotHandler
    }

    init {
        // process annotations in eventSourcedEntity
        if (this.annotations.isEmpty()) throw IllegalStateException(
                "no method annotated with @CommandHandler or @EventHandler or @Snapshot or @SnapshotHandler was found in class " +
                        "${eventSourcedEntity.javaClass.simpleName}" )

        cachedMethods()
    }

    override fun handleCommand(command: com.google.protobuf.Any?): com.google.protobuf.Any? {
        val typeUrl = command?.typeUrl
        val key = typeUrl?.let { extractClassNameFrom(it) }

        if (!commandHandlers.containsKey(key)) {
            throw IllegalArgumentException("No CommandHandler found for this type $key")
        }

        val handler = commandHandlers[key]

        val message: Message? = handler?.invoke(command!!.unpack(handler.parameterType))
        return com.google.protobuf.Any.pack(message)
    }

    override fun handleEvent(event: com.google.protobuf.Any?) {
        val typeUrl = event?.typeUrl

        val key = typeUrl?.let { extractClassNameFrom(it) }

        if (!eventHandlers.containsKey(key)) {
            throw IllegalArgumentException("No EventHandler found for this type $key")
        }

        val handler = eventHandlers[key]

        handler?.invoke(event!!.unpack(handler.parameterType))
    }

    override fun handleSnapshot(snapshot: com.google.protobuf.Any?) {
        val typeUrl = snapshot?.typeUrl

        val key = typeUrl?.let { extractClassNameFrom(it) }

        if (!snapshotHandlers.containsKey(key)) {
            throw IllegalArgumentException("No SnapshotHandler found for this type $key")
        }

        val handler = snapshotHandlers[key]

        handler?.invoke(snapshot!!.unpack(handler.parameterType))
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
                    val parameterType: Class<Message> = method.parameterTypes[0] as Class<Message>
                    val parameterTypeName: String = parameterType.canonicalName

                    val returnType: Class<Message> = method.returnType as Class<Message>
                    commandHandlers[parameterTypeName] = MethodHandler(parameterTypeName, method, parameterType, returnType, eventSourcedEntity)
                }
            }

            when {
                method.isAnnotationPresent(EventHandler::class.java) -> {
                    val parameterType: Class<Message> = method.parameterTypes[0] as Class<Message>
                    val parameterTypeName: String = parameterType.canonicalName

                    val returnType: Class<Message> = method.returnType as Class<Message>
                    eventHandlers[parameterTypeName] = MethodHandler(parameterTypeName, method, parameterType, returnType, eventSourcedEntity)
                }
            }

            when {
                method.isAnnotationPresent(SnapshotHandler::class.java) -> {
                    val parameterType: Class<Message> = method.parameterTypes[0] as Class<Message>
                    val parameterTypeName: String = parameterType.canonicalName

                    val returnType: Class<Message> = method.returnType as Class<Message>
                    snapshotHandlers[parameterTypeName] = MethodHandler(parameterTypeName, method, parameterType, returnType, eventSourcedEntity)
                }
            }

            when {
                method.isAnnotationPresent(Snapshot::class.java) -> {
                    val parameterType: Class<Message> =  method.returnType as Class<Message>
                    val parameterTypeName: String = parameterType.canonicalName

                    val returnType: Class<Message> = method.returnType as Class<Message>
                    snapshotsHandlers[parameterTypeName] = MethodHandler(parameterTypeName, method, parameterType, returnType, eventSourcedEntity)
                }
            }
        }
    }

    private fun extractClassNameFrom(typeUrl: String) = typeUrl.replace("type.googleapis.com/", "", true)
}

data class MethodHandler(
        val name: String,
        val method: Method,
        val parameterType: Class<Message>,
        val returnType: Class<Message>,
        val eventSourcedEntity: AnnotationEventSourcedEntity) {

    private val log = logger()

    fun invoke(obj: Any): Message? {
        val accessible = this.method.trySetAccessible()
        log.debug("Invoking method {} with type {} in {}. Accessible {}", method.name, parameterType, name, accessible)
        return this.method.invoke(this.eventSourcedEntity, obj) as Message?
    }

    fun invoke(): Any? {
        val accessible = this.method.trySetAccessible()
        log.debug("Invoking method {} with type {} in {}. Accessible {}", method.name, parameterType, name, accessible)
        return this.method.invoke(this.eventSourcedEntity)
    }
}