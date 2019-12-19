package io.cloudstate.kotlinsupport.protocol

import akka.event.Logging
import akka.stream.ActorMaterializer
import akka.stream.Attributes
import akka.stream.javadsl.Sink
import akka.stream.javadsl.Source
import com.google.protobuf.DescriptorProtos
import com.google.protobuf.Descriptors
import com.google.protobuf.Empty
import io.cloudstate.EntityKey
import io.cloudstate.kotlinsupport.EntityType
import io.cloudstate.kotlinsupport.getProjectVersion
import io.cloudstate.kotlinsupport.initializers.CloudStateInitializer
import io.cloudstate.kotlinsupport.initializers.CrdtEntityInitializer
import io.cloudstate.kotlinsupport.initializers.eventsourced.EventSourcedEntityInitializer
import io.cloudstate.kotlinsupport.logger
import io.cloudstate.protocol.EntityDiscovery
import io.cloudstate.protocol.EntityProto
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.stream.Collectors

class EntityDiscoveryService(
        private val initializer: CloudStateInitializer,
        private val services: MutableMap<String, CloudStateInitializer.EntityFunction>,
        private val materializer: ActorMaterializer): EntityDiscovery {

    private val log = logger()

    override fun discover(proxyInfo: EntityProto.ProxyInfo?): CompletionStage<EntityProto.EntitySpec> =
            Source.single(proxyInfo)
                    .log("CloudState-User")
                    .withAttributes(
                            Attributes.createLogLevels(
                                    Logging.InfoLevel(), // onElement
                                    Logging.WarningLevel(), // onFinish
                                    Logging.DebugLevel() // onFailure
                            ))
                    .map{
                        log.info("Received discovery call from sidecar [{} {}] supporting CloudState {}.{}",
                                it?.proxyName, it?.proxyVersion, it?.protocolMajorVersion, it?.protocolMinorVersion);
                        log.info("Supported sidecar entity types: {}", it?.supportedEntityTypesList);

                        val unsupportedServices = services.entries
                                .stream()
                                .filter{service -> !it?.supportedEntityTypesList?.contains(service.value.entityType)!! }
                                .collect(Collectors.toList())

                        if (unsupportedServices.isNotEmpty()) {
                            log.error(
                                    "Proxy doesn't support the entity types for the following services: {}", unsupportedServices
                                            .map { entry -> String.format("%s: %s",entry.value.entityName, entry.value.entityType)}
                            )
                        }

                        val builder = DescriptorProtos.FileDescriptorSet.newBuilder()

                        // Set dependencies descriptors
                        builder.addFile(Empty.getDescriptor().file.toProto())
                        builder.addFile(EntityKey.getDescriptor().toProto())
                        builder.addFile(DescriptorProtos.getDescriptor().toProto())

                        val entitySpecBuilder = EntityProto.EntitySpec.newBuilder()
                                .setServiceInfo(
                                        EntityProto.ServiceInfo.newBuilder()
                                                .setSupportLibraryName("cloudstate-kotlin-support")
                                                .setSupportLibraryVersion(getProjectVersion())
                                                .setServiceRuntime(KotlinVersion.CURRENT.toString())
                                                .setServiceName(initializer.serviceName)
                                                .setServiceVersion(initializer.serviceVersion)
                                                .build())

                        if (false) // TODO verify compatibility with in.protocolMajorVersion & in.protocolMinorVersion
                            //Future.failed(new Exception("Proxy version not compatible with library protocol support version"))
                        else {
                            log.debug("Registering {} services", services.entries.size)
                            services.entries.stream().forEach { item ->
                                log.info("EntityType: {}", item.value.entityType)
                                if (EntityType.EventSourced.typeStr == item.value.entityType) {
                                    var eventSourcedInitializer = item.value.initializer as EventSourcedEntityInitializer
                                    builder.addFile(eventSourcedInitializer.descriptor?.file?.toProto())
                                    eventSourcedInitializer.additionalDescriptors?.forEach { fileDescriptor -> builder.addFile(fileDescriptor.toProto()) }

                                    entitySpecBuilder.addEntities(
                                            EntityProto.Entity.newBuilder()
                                                    .setEntityType(item.value.entityType)
                                                    .setPersistenceId(eventSourcedInitializer.persistenceId)
                                                    .setServiceName(eventSourcedInitializer.descriptor?.fullName)
                                                    .setPersistenceId(eventSourcedInitializer.persistenceId)
                                                    .build())
                                }

                                if (EntityType.Crdt.typeStr == item.value.entityType) {
                                    var crdtInitializer = item.value.initializer as CrdtEntityInitializer
                                    builder.addFile(crdtInitializer.descriptor?.file?.toProto())
                                    crdtInitializer.additionalDescriptors?.forEach { fileDescriptor -> builder.addFile(fileDescriptor.toProto()) }

                                    entitySpecBuilder.addEntities(
                                            EntityProto.Entity.newBuilder()
                                                    .setEntityType(item.value.entityType)
                                                    .setPersistenceId(crdtInitializer.persistenceId)
                                                    .setServiceName(crdtInitializer.descriptor?.fullName)
                                                    .setPersistenceId(crdtInitializer.persistenceId)
                                                    .build())

                                }
                            }

                        }

                        val fileDescriptorSet = builder.build().toByteString()

                        entitySpecBuilder
                                .setProto(fileDescriptorSet)
                                .build()
                    }
                    .runWith(Sink.head(), materializer)

    override fun reportError(userFunctionError: EntityProto.UserFunctionError?): CompletionStage<Empty> {
        log.error("Error reported from sidecar: {}", userFunctionError?.message)
        return CompletableFuture.completedFuture(Empty.getDefaultInstance())
    }
}