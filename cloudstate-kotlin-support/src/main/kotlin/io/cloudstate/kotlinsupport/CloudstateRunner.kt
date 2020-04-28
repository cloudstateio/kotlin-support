package io.cloudstate.kotlinsupport

import akka.Done
import com.google.protobuf.Descriptors
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.cloudstate.javasupport.CloudState
import io.cloudstate.javasupport.impl.AnySupport
import io.cloudstate.javasupport.impl.crdt.AnnotationBasedCrdtSupport
import io.cloudstate.kotlinsupport.api.eventsourced.KotlinAnnotationBasedEventSourced
import io.cloudstate.kotlinsupport.initializers.CloudStateInitializer
import java.util.*
import java.util.concurrent.CompletionStage

class CloudStateRunner(private val initializer: CloudStateInitializer) {
    private val engine = CloudState()
    private val prefer: AnySupport.Prefer = AnySupport.PREFER_JAVA()
    private val typeUrlPrefix: String = AnySupport.DefaultTypeUrlPrefix()
    private lateinit var conf: Config

    fun withAllRegisters(): CloudStateRunner {

        conf = getConfig()

        initializer.statefulServiceDescriptors.forEach{ descriptor ->

            when(descriptor.entityType) {

                EntityType.EventSourced -> {
                    val anySupport = descriptor.additionalDescriptors?.let { newAnySupport(it) }

                    engine.registerEventSourcedEntity(
                            KotlinAnnotationBasedEventSourced(descriptor!!.serviceClass!!, anySupport!!, descriptor.descriptor!!),
                            descriptor.descriptor,
                            descriptor.persistenceId,
                            descriptor.snapshotEvery,
                            *descriptor.additionalDescriptors.toTypedArray())
                }

                EntityType.Crdt -> {
                    val anySupport = descriptor.additionalDescriptors?.let { newAnySupport(it) }
                    val clazz: Class<*>? = descriptor.transcoder!!.transcode()

                    engine.registerCrdtEntity(
                            AnnotationBasedCrdtSupport(descriptor.serviceClass!!::class.java, anySupport!!, descriptor.descriptor!!),
                            descriptor.descriptor,
                            *descriptor.additionalDescriptors.toTypedArray())
                }

            }

        }
        return this
    }

    fun start(): CompletionStage<Done> = engine.start()

    //fun start(): CompletionStage<Done> = engine.start(conf)

    private fun getConfig(): Config {
        setEnv(
                mapOf("SUPPORT_LIBRARY_NAME" to "cloudstate-kotlin-support",
                        "SUPPORT_LIBRARY_VERSION" to getProjectVersion()))

        val properties: Properties = Properties()
        properties.setProperty("cloudstate.system.akka.loglevel", initializer.configInit.loglevel)
        properties.setProperty("cloudstate.user-function-interface", initializer.configInit.host)
        properties.setProperty("cloudstate.user-function-port", initializer.configInit.port.toString())

        return ConfigFactory.parseProperties(properties)
                .withFallback(ConfigFactory.defaultApplication())
                .resolve()
    }

    private fun newAnySupport(descriptors: List<Descriptors.FileDescriptor>): AnySupport? =
            AnySupport(descriptors.toTypedArray(), this.javaClass.classLoader, typeUrlPrefix, prefer)

}
