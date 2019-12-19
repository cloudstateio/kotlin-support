package io.cloudstate.kotlinsupport.protocol.handlers

import akka.actor.AbstractActor
import io.cloudstate.kotlinsupport.CloudStateRunner
import io.cloudstate.kotlinsupport.logger
import io.cloudstate.protocol.EntityProto
import io.cloudstate.protocol.EventSourcedProto

class EventSourcedRouterHandler(val entityRefs: List<CloudStateRunner.EntityWrapper>): AbstractActor() {
    private val log = logger()

    override fun createReceive(): Receive =
        receiveBuilder()
                .match(EventSourcedProto.EventSourcedInit::class.java) {
                    log.info("Receive EventSourcedInit \n{}", it)
                    //entityRefs.filter { it.entityFunction.entityType }
                    sender.tell(EventSourcedProto.EventSourcedStreamOut.newBuilder().build() ,self)
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