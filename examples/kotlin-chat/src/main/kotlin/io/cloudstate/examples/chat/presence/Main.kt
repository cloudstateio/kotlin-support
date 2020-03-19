package io.cloudstate.examples.chat.presence

import io.cloudstate.kotlinsupport.cloudstate

fun main() {
    cloudstate {

        config {
            port = 8080
        }

        crdt {
            entityService = PresenceEntity::class
            descriptor =  PresenceProtos.getDescriptor().findServiceByName("Presence")
        }
    }.start()
            .toCompletableFuture()
            .get()
}
