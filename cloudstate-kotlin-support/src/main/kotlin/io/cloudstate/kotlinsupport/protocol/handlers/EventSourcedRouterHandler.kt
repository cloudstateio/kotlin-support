package io.cloudstate.kotlinsupport.protocol.handlers

import akka.actor.AbstractActor
import com.google.protobuf.Any
import com.google.protobuf.Message
import io.cloudstate.kotlinsupport.initializers.CloudStateInitializer
import io.cloudstate.kotlinsupport.initializers.eventsourced.EventSourcedEntityInitializer
import io.cloudstate.kotlinsupport.logger
import io.cloudstate.kotlinsupport.protocol.InvalidOperationException
import io.cloudstate.kotlinsupport.services.eventsourced.EventSourcedEntity
import io.cloudstate.protocol.EntityProto
import io.cloudstate.protocol.EventSourcedProto

class EventSourcedRouterHandler(private val entities: Map<String, CloudStateInitializer.EntityFunction>): AbstractActor() {
    private val log = logger()
    private val entityHandlers: MutableMap<String, EventSourcedEntity.Handler> = mutableMapOf()

    override fun createReceive(): Receive =
        receiveBuilder()
                .match(EventSourcedProto.EventSourcedInit::class.java) { init ->
                    log.info("Receive EventSourcedInit \n{}", init)
                    val entityId = init.entityId

                    if (log.isDebugEnabled) entityHandlers.forEach {
                        log.debug("Handlers Active: {}", it.key)
                    }

                    if (!entityHandlers.containsKey(init.serviceName)) {

                        log.debug("Creating instance of Service Handler for handle request")

                        if (!entities.containsKey(init.serviceName) ) {
                            throw InvalidOperationException("Failed to locate service with name ${init.serviceName}");
                        }

                        val entityService = entities[init.serviceName]
                        log.trace("EntityService: {}", entityService)

                        val eventSourcedEntityInitializer = entityService?.initializer as EventSourcedEntityInitializer

                        log.trace("Creating a EntityFunction")
                        val entityFunctionInstance: EventSourcedEntity = eventSourcedEntityInitializer.entityService
                                .getConstructor(String::class.java)
                                .newInstance(entityId)

                        val handler = entityFunctionInstance.create()
                        log.trace("Instance of Handler created: {}", handler)
                        entityHandlers[init.serviceName] = handler
                    }

                    val snapshot: EventSourcedProto.EventSourcedSnapshot
                    if (init.snapshot != null /*hasSnapshot()*/ ){
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
                .match(EventSourcedProto.EventSourcedEvent::class.java) {
                    log.info("Receive EventSourcedEvent \n{}", it)
                    sender.tell(EventSourcedProto.EventSourcedStreamOut.newBuilder().build() ,self)
                }
                .match(EntityProto.Command::class.java) {
                    log.info("Receive Command \n{}", it)
                    sender.tell(EventSourcedProto.EventSourcedStreamOut.newBuilder().build() ,self)
                }
                .build()

}