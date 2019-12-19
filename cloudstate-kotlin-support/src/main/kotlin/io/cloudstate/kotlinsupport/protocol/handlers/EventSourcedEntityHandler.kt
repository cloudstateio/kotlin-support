package io.cloudstate.kotlinsupport.protocol.handlers

import akka.actor.AbstractActor
import io.cloudstate.kotlinsupport.initializers.CloudStateInitializer

class EventSourcedEntityHandler(val entityFunction: CloudStateInitializer.EntityFunction): AbstractActor() {

    override fun createReceive(): Receive {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}