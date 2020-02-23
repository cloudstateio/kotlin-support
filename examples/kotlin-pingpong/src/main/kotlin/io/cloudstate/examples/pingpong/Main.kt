package io.cloudstate.examples.pingpong

import io.cloudstate.javasupport.CloudState
import io.cloudstate.pingpong.Pingpong

object Main {
    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        CloudState()
                .registerEventSourcedEntity(
                        PingPongEntity::class.java,
                        Pingpong.getDescriptor().findServiceByName("PingPongService"),
                        Pingpong.getDescriptor())
                .start()
                .toCompletableFuture()
                .get()
    }
}