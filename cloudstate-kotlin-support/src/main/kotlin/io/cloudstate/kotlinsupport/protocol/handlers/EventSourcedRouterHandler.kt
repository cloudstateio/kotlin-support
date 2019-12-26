package io.cloudstate.kotlinsupport.protocol.handlers

import akka.actor.AbstractActor
import com.google.protobuf.Any
import com.google.protobuf.Message
import io.cloudstate.kotlinsupport.annotations.CommandHandler
import io.cloudstate.kotlinsupport.initializers.CloudStateInitializer
import io.cloudstate.kotlinsupport.initializers.eventsourced.EventSourcedEntityInitializer
import io.cloudstate.kotlinsupport.logger
import io.cloudstate.kotlinsupport.protocol.InvalidOperationException
import io.cloudstate.kotlinsupport.services.eventsourced.FunctionalEventSourcedEntity
import io.cloudstate.protocol.EntityProto
import io.cloudstate.protocol.EventSourcedProto

class EventSourcedRouterHandler(private val entities: Map<String, CloudStateInitializer.EntityFunction>): AbstractActor() {
    private val log = logger()
    private val entityHandlers: MutableMap<String, FunctionalEventSourcedEntity.Handler> = mutableMapOf()

    override fun createReceive(): Receive =
        receiveBuilder()
                .match(EventSourcedProto.EventSourcedInit::class.java) { init ->
                    log.info("Receive EventSourcedInit \n{}", init)
                    val entityId = init.entityId

                    if (log.isDebugEnabled) entityHandlers.forEach {
                        log.debug("Active Handlers: {}", it.key)
                    }

                    if (!entityHandlers.containsKey(init.serviceName)) {

                        setHandlerForEntity(init, entityId)
                    }

                    val snapshot: EventSourcedProto.EventSourcedSnapshot
                    if (init.snapshot != null){
                        val response = handleInitSnapshot(init)

                        sender.tell(response, self)
                    } else {
                        log.debug("Message Init doesn't contain snapshot request")
                        sender.tell(
                                EventSourcedProto.EventSourcedStreamOut.newBuilder()
                                        .setReply(
                                                EventSourcedProto.EventSourcedReply.newBuilder()
                                                        .build())
                                        .build(), self)
                    }

                }
                .match(EntityProto.Command::class.java) { command ->
                    log.info("Receive Command \n{}", command)
                    val handler = getHandlerByEntityId(command.entityId)
                    // TODO: Verify if commandResult or commandAction first
                    val function: () -> kotlin.Any = handler.commandResultHandlers.first { type ->
                        log.debug("Handler type -> {}. Payload type -> {}", type, command.payload.typeUrl)
                        if (type.javaClass.isAnnotationPresent(CommandHandler::class.java)) {
                            log.debug("Annotations found!")
                            val annotationValue: String = type.javaClass.getAnnotation(CommandHandler::class.java).value
                            log.info("Annotation value: {}", annotationValue)
                            command.name.equals(annotationValue, ignoreCase = true)
                        }
                        log.info("No annotation with type class valid")
                        false
                    }

                    val invoke = function.invoke() as Message

                    sender.tell(
                            EventSourcedProto.EventSourcedStreamOut.newBuilder()
                                    .setReply(EventSourcedProto.EventSourcedReply.newBuilder()
                                            .setCommandId(command.id)
                                            .setClientAction(
                                                    EntityProto.ClientAction.newBuilder()
                                                            .setReply(EntityProto.Reply.newBuilder()
                                                                    .setPayload(Any.pack(invoke)).build())
                                                            .build())
                                            .build())
                                    .build() ,self)
                }
                .match(EventSourcedProto.EventSourcedEvent::class.java) { event ->
                    log.info("Receive EventSourcedEvent \n{}", event)
                    sender.tell(EventSourcedProto.EventSourcedStreamOut.newBuilder().build() ,self)
                }
                .build()

    private fun getHandlerByEntityId(entityId: String?):  FunctionalEventSourcedEntity.Handler =
        this.entityHandlers.filter { it.value.entityId == entityId }.map { val value: FunctionalEventSourcedEntity.Handler = it.value
            value
        }.first()


    private fun setHandlerForEntity(init: EventSourcedProto.EventSourcedInit, entityId: String?) {
        log.debug("Creating instance of Service Handler for handle request")

        if (!entities.containsKey(init.serviceName)) {
            throw InvalidOperationException("Failed to locate service with name ${init.serviceName}");
        }

        val entityService = entities[init.serviceName]
        log.trace("EntityService: {}", entityService)

        val eventSourcedEntityInitializer = entityService?.initializer as EventSourcedEntityInitializer

        log.trace("Creating a EntityFunction")
        val entityFunctionInstance: FunctionalEventSourcedEntity = eventSourcedEntityInitializer.entityService
                .getConstructor(String::class.java)
                .newInstance(entityId)

        val handler = entityFunctionInstance.handler!!
        log.trace("Instance of Handler created: {}", handler)
        entityHandlers[init.serviceName] = handler
    }

    private fun handleInitSnapshot(init: EventSourcedProto.EventSourcedInit): EventSourcedProto.EventSourcedStreamOut? {
        log.debug("Snapshot Request!")

        val snapshot = init.snapshot
        val anyRequest: Any = snapshot.snapshot

        log.debug("Snapshot Request. Any typeUrl: {}. Request: {}", anyRequest.typeUrl, snapshot)

        val any = entityHandlers[init.serviceName]?.snapshot?.invoke() as Message

        log.debug("Snapshot invoke return: {}. Size bytes: {}", any.toString(), any.toByteArray().size)
        val snapshotResponse = EventSourcedProto.EventSourcedReply.newBuilder()
                .setSnapshot(Any.pack(any))
                .build()

        val response = EventSourcedProto.EventSourcedStreamOut.newBuilder()
                .setReply(snapshotResponse)
                .build()

        log.debug("Response -> {}", response)
        return response
    }

}