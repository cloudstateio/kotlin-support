package io.cloudstate.examples.pingpong

import io.cloudstate.kotlinsupport.cloudstate

fun main() {
    cloudstate {
        eventsourced {
            entityService = PingPongEntity::class
            descriptor = Pingpong.getDescriptor().findServiceByName("PingPongService")
            additionalDescriptors = mutableListOf( Pingpong.getDescriptor() )
        }
    }.start()
            .toCompletableFuture()
            .get()
}
