package io.cloudstate.kotlinsupport

import akka.Done
import com.google.protobuf.Descriptors
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.cloudstate.javasupport.CloudState
import io.cloudstate.javasupport.impl.AnySupport
import io.cloudstate.javasupport.impl.crdt.AnnotationBasedCrdtSupport
import io.cloudstate.javasupport.impl.eventsourced.AnnotationBasedEventSourcedSupport
import io.cloudstate.kotlinsupport.initializers.CloudStateInitializer
import java.util.*
import java.util.concurrent.CompletionStage

class CloudStateRunner(private val initializer: CloudStateInitializer) {
    private val log = logger()

    private val engine = CloudState()
    private val prefer: AnySupport.Prefer = AnySupport.PREFER_JAVA()
    private val typeUrlPrefix: String = AnySupport.DefaultTypeUrlPrefix()
    private lateinit var conf: Config

    fun withAllRegisters(): CloudStateRunner {

        conf = getConfig()

        initializer.statefulServiceDescriptors.forEach{ descriptor ->
            val anySupport = newAnySupport(descriptor.additionalDescriptors)

            when(descriptor.entityType) {
                EntityType.EventSourced -> {
                    engine.registerEventSourcedEntity(
                            AnnotationBasedEventSourcedSupport(descriptor.entityService, anySupport, descriptor.descriptor),
                            descriptor.descriptor,
                            descriptor.persistenceId,
                            descriptor.snapshotEvery,
                            *descriptor.additionalDescriptors)
                }
                EntityType.Crdt -> {
                    engine.registerCrdtEntity(
                            AnnotationBasedCrdtSupport(descriptor.entityService, anySupport, descriptor.descriptor),
                            descriptor.descriptor,
                            *descriptor.additionalDescriptors)

                }
                else -> {
                    log.warn("Unknown type of Entity")
                    throw IllegalStateException("Unknown type of Entity")
                }

            }


        }
        return this
    }

    fun start(): CompletionStage<Done> = engine.start()

    //fun start(): CompletionStage<Done> = engine.start(conf)

    private fun getConfig(): Config {
        val properties: Properties = Properties()
        properties.setProperty("cloudstate.system.akka.loglevel", initializer.loglevel)
        properties.setProperty("cloudstate.user-function-interface", initializer.host)
        properties.setProperty("cloudstate.user-function-port", initializer.port.toString())

        return ConfigFactory.parseProperties(properties)
                .withFallback(ConfigFactory.defaultApplication())
                .resolve()
    }

    private fun newAnySupport(descriptors: Array<Descriptors.FileDescriptor>): AnySupport? =
            AnySupport(descriptors, this.javaClass.classLoader, typeUrlPrefix, prefer)

}