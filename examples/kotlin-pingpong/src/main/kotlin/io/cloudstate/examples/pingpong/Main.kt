package io.cloudstate.examples.pingpong

import io.cloudstate.kotlinsupport.cloudstate

fun main(args: Array<String>) {
    cloudstate {
        registerEventSourcedEntity {
            entityService = PingPongEntity::class.java
            descriptor = Pingpong.getDescriptor().findServiceByName("PingPongService")
            additionalDescriptors = arrayOf( Pingpong.getDescriptor() )
        }
    }.start()
            .toCompletableFuture()
            .get()
}