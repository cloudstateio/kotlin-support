package io.cloudstate.kotlinsupport.protocol

import akka.NotUsed
import akka.actor.ActorRef
import akka.event.Logging
import akka.stream.ActorMaterializer
import akka.stream.Attributes
import akka.stream.javadsl.*
import akka.japi.Pair as AkkaPair

import io.cloudstate.kotlinsupport.initializers.CloudStateInitializer
import io.cloudstate.protocol.EventSourced
import io.cloudstate.protocol.EventSourcedProto
import java.util.concurrent.CompletableFuture

class EventSourcedService(
        private val initializer: CloudStateInitializer,
        private val handler: ActorRef,
        private val materializer: ActorMaterializer) : EventSourced {

    private var inboundHub: Sink<EventSourcedProto.EventSourcedStreamIn, NotUsed>
    private var outboundHub: Source<EventSourcedProto.EventSourcedStreamOut, NotUsed>

    init {
        val hubInAndOut: AkkaPair<Sink<EventSourcedProto.EventSourcedStreamIn, NotUsed>, Source<EventSourcedProto.EventSourcedStreamOut, NotUsed>> =
        MergeHub.of(EventSourcedProto.EventSourcedStreamIn::class.java)
                .log("Cloudstate-User")
                .withAttributes(
                        Attributes.createLogLevels(
                                Logging.InfoLevel(), // onElement
                                Logging.WarningLevel(), // onFinish
                                Logging.DebugLevel() // onFailure
                        ))
                .mapAsync(1){ request ->
                    CompletableFuture.supplyAsync() {

                        // First create specific handler [AnnotationEventSourcedHandler/FunctionalEventSourcedEntityHandler]

                        // Second handler specific message types
                        if (request.hasInit()) {
                            //return@map it.init
                        }

                        if (request.hasCommand()) {
                            //return@map it.command
                        }

                        if (request.hasEvent()) {

                        }
                        // have return EventSourcedStreamOut
                        EventSourcedProto.EventSourcedStreamOut.newBuilder().build()
                    }
                }
                .toMat(BroadcastHub.of(EventSourcedProto.EventSourcedStreamOut::class.java), Keep.both())
                .run(materializer)

        inboundHub = hubInAndOut.first()
        outboundHub = hubInAndOut.second()

    }

    /*override fun handle(streamIn: Source<EventSourcedProto.EventSourcedStreamIn, NotUsed>?): Source<EventSourcedProto.EventSourcedStreamOut, NotUsed> =
        streamIn!!.log("Cloudstate-User")
                .withAttributes(
                        Attributes.createLogLevels(
                                Logging.InfoLevel(), // onElement
                                Logging.WarningLevel(), // onFinish
                                Logging.DebugLevel() // onFailure
                        ))
                .map {
                    if (it.hasInit()) {
                        return@map it.init
                    }

                    if (it.hasCommand()) {
                        return@map it.command
                    }

                    if (!it.hasEvent()) {
                    } else {
                        return@map it.event
                    }

                }
                .ask( handler, EventSourcedProto.EventSourcedStreamOut::class.java, Timeout.create(Duration.ofMillis(initializer.functionTimeout)) )!!*/

    override fun handle(streamIn: Source<EventSourcedProto.EventSourcedStreamIn, NotUsed>?): Source<EventSourcedProto.EventSourcedStreamOut, NotUsed> {
        streamIn?.runWith(inboundHub, materializer)
        return outboundHub
    }

}