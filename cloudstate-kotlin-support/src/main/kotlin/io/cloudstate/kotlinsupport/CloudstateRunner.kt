package io.cloudstate.kotlinsupport

import akka.Done
import com.google.protobuf.Descriptors
import io.cloudstate.javasupport.CloudState
import io.cloudstate.javasupport.impl.AnySupport
import io.cloudstate.javasupport.impl.eventsourced.AnnotationBasedEventSourcedSupport
import io.cloudstate.kotlinsupport.initializers.CloudStateInitializer
import java.util.concurrent.CompletionStage

class CloudStateRunner(private val initializer: CloudStateInitializer) {

    private val engine = CloudState()
    private val prefer: AnySupport.Prefer = AnySupport.PREFER_JAVA()
    private val typeUrlPrefix: String = AnySupport.DefaultTypeUrlPrefix()

    fun withAllRegisters(): CloudStateRunner {
        initializer.statefulServiceDescriptors.forEach{ descriptor ->
            val anySupport = newAnySupport(descriptor.additionalDescriptors)

            engine.registerEventSourcedEntity(
                    AnnotationBasedEventSourcedSupport(descriptor.entityService, anySupport, descriptor.descriptor),
                    descriptor.descriptor,
                    descriptor.persistenceId,
                    descriptor.snapshotEvery,
                    *descriptor.additionalDescriptors)
        }
        return this
    }

    fun start(): CompletionStage<Done> = engine.start()

    private fun newAnySupport(descriptors: Array<Descriptors.FileDescriptor>): AnySupport? =
            AnySupport(descriptors, this.javaClass.classLoader, typeUrlPrefix, prefer)

}