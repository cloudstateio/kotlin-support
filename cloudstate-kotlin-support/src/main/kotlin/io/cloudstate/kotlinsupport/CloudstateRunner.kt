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
    private val log = logger()
    private val engine = CloudState()
    private val prefer: AnySupport.Prefer = AnySupport.PREFER_JAVA()
    private val typeUrlPrefix: String = AnySupport.DefaultTypeUrlPrefix()
    private lateinit var conf: Config

    fun withAllRegisters(): CloudStateRunner {
        initializer.statefulServiceDescriptors.forEach{ descriptor ->

            when(descriptor.entityType) {

                EntityType.EventSourced -> {
                    val anySupport = descriptor.additionalDescriptors.let { newAnySupport(it) }

                    engine.registerEventSourcedEntity(
                            KotlinAnnotationBasedEventSourced(descriptor.serviceClass!!, anySupport!!, descriptor.descriptor!!),
                            descriptor.descriptor,
                            descriptor.persistenceId,
                            descriptor.snapshotEvery,
                            *descriptor.additionalDescriptors.toTypedArray())
                }

                EntityType.Crdt -> {
                    val anySupport = newAnySupport(descriptor.additionalDescriptors)
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

    fun start(): CompletionStage<Done> = engine.start(getConfig())

    fun start(conf: Config): CompletionStage<Done> = engine.start(conf)

    private fun getConfig(): Config {
        log.info("Loading Config...")
        val properties = Properties()
        properties.setProperty("cloudstate.system.akka.loglevel", initializer.configInit.loglevel)
        properties.setProperty("cloudstate.library.name", "kotlin-support")
        properties.setProperty("cloudstate.library.version", getProjectVersion())
        properties.setProperty("cloudstate.user-function-interface", initializer.configInit.host)
        properties.setProperty("cloudstate.user-function-port", initializer.configInit.port.toString())

        val conf = ConfigFactory.load(ConfigFactory.parseProperties(properties))
        log.debug("Load config library.name: ${conf.getString("cloudstate.library.name")}")
        log.debug("Load config library.version: ${conf.getString("cloudstate.library.version")}")
        log.debug("Load config user-function-port: ${conf.getString("cloudstate.user-function-port")}")
        log.debug("Load config user-function-interface: ${conf.getString("cloudstate.user-function-interface")}")
        return conf.getConfig("cloudstate.system").withFallback(conf)
    }

    private fun newAnySupport(descriptors: List<Descriptors.FileDescriptor>): AnySupport? =
            AnySupport(descriptors.toTypedArray(), this.javaClass.classLoader, typeUrlPrefix, prefer)

}
