package io.cloudstate.kotlinsupport.api.crdt

import com.google.protobuf.Any
import com.google.protobuf.Descriptors
import io.cloudstate.javasupport.crdt.*
import io.cloudstate.javasupport.impl.AnySupport
import io.cloudstate.javasupport.impl.ResolvedEntityFactory
import io.cloudstate.javasupport.impl.ResolvedServiceMethod
import scala.collection.immutable.Map
import java.util.*

class KotlinAnnotationBasedCrdt(
        private val entityClass: Class<*>,
        private val anySupport: AnySupport,
        private val resolvedMethods: Map<String, ResolvedServiceMethod<*, *>>): CrdtEntityFactory, ResolvedEntityFactory {

    constructor(entityClass: Class<*>, anySupport: AnySupport, serviceDescriptor: Descriptors.ServiceDescriptor):
        this(entityClass, anySupport, anySupport.resolveServiceDescriptor(serviceDescriptor))

    override fun create(context: CrdtCreationContext?): CrdtEntityHandler = EntityHandler(entityConstructor(context))

    override fun resolvedMethods(): Map<String, ResolvedServiceMethod<*, *>> {
        TODO("Not yet implemented")
    }

    private fun entityConstructor(context: CrdtCreationContext?): kotlin.Any {
        TODO("Not yet implemented")
    }

    class EntityHandler(private val entity: kotlin.Any) : CrdtEntityHandler {

        override fun handleCommand(command: Any?, context: CommandContext?): Optional<Any> {
            TODO("Not yet implemented")
        }

        override fun handleStreamedCommand(command: Any?, context: StreamedCommandContext<Any>?): Optional<Any> {
            TODO("Not yet implemented")
        }

    }
}