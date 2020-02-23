package io.cloudstate.kotlinsupport.api.eventsourced

import com.google.protobuf.GeneratedMessageV3
import io.cloudstate.javasupport.Context
import io.cloudstate.kotlinsupport.logger
import org.apache.commons.lang3.builder.ToStringBuilder
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.jvm.reflect

open class EventSourcedHandler<S : Any> {
    private val log = logger()

    private val methodDefinitions = mutableListOf<MethodDefinition>()

    var state: S? = null
    var persistenceId: String? = null
    var snapshotEvery: Int? = 0

    fun getMethodDefinitions() = methodDefinitions

    inline fun withState(initialState: S, block: EventSourcedHandler<S>.() -> EventSourcedHandler<S>) = apply {
        for (field in this::class.java.declaredFields.filter { it.name == "state" }) {
            System.out.printf("%s: %s%n", field.name, field.genericType)
        }
        state = initialState
        block()
    }

    infix fun withPersistenceId(id: String) = apply {
        persistenceId = id
    }

    infix fun withSnapshotEvery(every: Int) = apply {
        snapshotEvery = every
    }

    fun <T : GeneratedMessageV3> snapshot(name: String = "snapshot", function: () -> T) {
        requireNotNull(state, { "State has not been initialized. Try use withState function before call it" })
        val parameters = function.reflect()?.parameters
        val returnType = function.reflect()?.returnType

        val functionDefinition = SnapshotFunction(parameters, returnType, function)
        methodDefinitions.add(
                MethodDefinition(name = name, evtSourcedMethodType = EvtSourcedMethodType.SNAPSHOT, lambdaDefinition = functionDefinition)
        )

        // Only for test purpose
        val result = function()
        log.info("Snapshot user function name is -> $name. State -> $state")
        log.info("Return type is -> $returnType")
    }

    fun <T : GeneratedMessageV3> snapshotHandler(name: String = "handleSnapshot", function: (T) -> Unit) {
        requireNotNull(state, { "State has not been initialized. Try use withState function before call it" })
        log.info("Calling snapshotHandler with name -> $name")
        function.reflect()?.parameters?.forEach {
            log.info("SnapshotHandler Parameter -> ${it.name}. Type -> ${it.type}")
        }

        val parameters = function.reflect()?.parameters
        val returnType = function.reflect()?.returnType
        val functionDefinition = SnapshotHandlerFunction(parameters, returnType, function)
        methodDefinitions.add(
                MethodDefinition(name = name, evtSourcedMethodType = EvtSourcedMethodType.SNAPSHOT, lambdaDefinition = functionDefinition)
        )
    }

    /**
     * Event handlers are differentiated by the type of event they handle. By default, the type of event an event handler
     * handles will be determined by looking for a single non context parameter that the event handler takes.
     * If for any reason this needs to be overridden, or if the event handler method doesn’t take any non context parameter
     * (because the event type may be all that needs to be known to handle the event), the type of event the handler
     * handles can be specified using the eventClass parameter.
     *
     * @param name This is optional and is the name of event handler
     * @param eventClass  The event type class. Generally, this will be determined by looking at the parameter of the event
     *                   handler method, however if the event doesn't need to be passed to the method
     *                   (for example, perhaps it contains no data), then this can be used to indicate which event this
     *                   handler handles.
     * @param function  Lambda for handling event
     * @return <T : GeneratedMessageV3> This returns sum of numA and numB.
     */
    fun <T : GeneratedMessageV3> eventHandler(name: String = "handleEvent", eventClass: Class<Any> = Any::class.java, function: (T) -> Unit) {
        requireNotNull(state, { "State has not been initialized. Try use withState function before call it" })
        log.info("Calling eventHandler with name -> $name")
        val parameters = function.reflect()?.parameters
        val returnType = function.reflect()?.returnType
        val functionDefinition = EventHandlerFunction(parameters, returnType, eventClass, function)
        methodDefinitions.add(
                MethodDefinition(name = name, evtSourcedMethodType = EvtSourcedMethodType.SNAPSHOT, lambdaDefinition = functionDefinition)
        )
    }

    /**
     * The command handler also can take the gRPC service call input type as a parameter, to receive the command message.
     * This is optional, sometimes it’s not needed, for example, our ShoppingCart example the GetCart service call doesn’t need any information
     * from the message, since it’s just returning the current state as is. Meanwhile, the AddItem service call
     * does need information from the message, since it needs to know the product id, description and quantity to add to the cart.
     * The return type of the command handler must be the output type for the gRPC service call, this will be sent as the reply.
     *
     * @param name This is mandatory and refer to name of method for handling command
     * @param function  Lambda for handling command
     * @return The result of the command handler and must be the output type for the gRPC service call, this will be sent as the reply
     *
     * Example:
     *    commandHandler<ShoppingCartProto.Cart>("getCart") {
     *       ShoppingCartProto.Cart.newBuilder().addAllItems(state!!.values).build()
     *    }
     */
    fun <T : GeneratedMessageV3> commandHandler(name: String, function: () -> T) {
        requireNotNull(state, { "State has not been initialized. Try use withState function before call it" })
        log.info("Calling commandHandler with name -> $name")
        val result: T = function()
        log.info("Result type instance -> ${result.parserForType}")
        log.info("Result Object -> ${ToStringBuilder.reflectionToString(result)}")
        val parameters = function.reflect()?.parameters
        val returnType = function.reflect()?.returnType
        val functionDefinition = SimpleCommandHandlerFunction(parameters, returnType, function)
        methodDefinitions.add(
                MethodDefinition(name = name, evtSourcedMethodType = EvtSourcedMethodType.SNAPSHOT, lambdaDefinition = functionDefinition)
        )
    }

    /**
     * The command handler also can take the gRPC service call input type as a parameter, to receive the command message.
     * This is optional, sometimes it’s not needed, for example, our ShoppingCart example the GetCart service call doesn’t need any information
     * from the message, since it’s just returning the current state as is. Meanwhile, the AddItem service call
     * does need information from the message, since it needs to know the product id, description and quantity to add to the cart.
     * The return type of the command handler must be the output type for the gRPC service call, this will be sent as the reply.
     *
     * @param name This is mandatory and refer to name of method for handling command
     * @param function (T,C) Lambda for handling command. Receive request type and return the result of the command handler
     *        and must be the output type for the gRPC service call, this will be sent as the reply
     *
     * Example:
     *    commandHandler<ShoppingCartProto.RemoveLineItem, CommandContext, Empty>("removeItem") { item, ctx ->
     *       if (!state!!.containsKey(item.productId)) ctx.fail("Cannot remove item ${item.productId} because it is not in the cart.")
     *
     *          with(ctx) {
     *              emit(Domain.ItemRemoved.newBuilder()
     *                      .setProductId(item.productId)
     *                      .build())
     *          }
     *
     *       Empty.getDefaultInstance()
     *    }
     */
    fun <T : GeneratedMessageV3, C: Context, R: GeneratedMessageV3> commandHandler(name: String, function: (T, C) -> R) {
        requireNotNull(state, { "State has not been initialized. Try use withState function before call it" })
        log.info("Calling commandHandler with name -> $name")
        val type = function.reflect()?.returnType
        log.info("CommandHandler Return type is -> $type")
        function.reflect()?.parameters?.forEach {
            log.info("CommandHandler Parameter -> ${it.name}. Type -> ${it.type}")
        }

        val parameters = function.reflect()?.parameters
        val returnType = function.reflect()?.returnType
        val functionDefinition = CommandHandlerInCtxOutFunction(parameters, returnType, function)
        methodDefinitions.add(
                MethodDefinition(name = name, evtSourcedMethodType = EvtSourcedMethodType.SNAPSHOT, lambdaDefinition = functionDefinition)
        )
    }

}

enum class EvtSourcedMethodType {
    SNAPSHOT, SNAPSHOT_HANDLER, EVENT_HANDLER, COMMAND_HANDLER
}

data class MethodDefinition(
        val name: String,
        val evtSourcedMethodType: EvtSourcedMethodType,
        val lambdaDefinition: FunctionDefinition)

open class FunctionDefinition(open var parameters: List<KParameter>?, open var returnType: KType?)

data class SimpleCommandHandlerFunction<T : GeneratedMessageV3>(
        override var parameters: List<KParameter>?,
        override var returnType: KType?,
        val function: () -> T): FunctionDefinition(parameters, returnType)


data class CommandHandlerInCtxOutFunction<T : GeneratedMessageV3, C: Context, R: GeneratedMessageV3>(
        override var parameters: List<KParameter>?,
        override var returnType: KType?,
        val function: (T, C) -> R): FunctionDefinition(parameters, returnType)

data class EventHandlerFunction<T : GeneratedMessageV3>(
        override var parameters: List<KParameter>?,
        override var returnType: KType?,
        val eventClass: Class<Any>,
        val function: (T) -> Unit): FunctionDefinition(parameters, returnType)

data class SnapshotHandlerFunction<T : GeneratedMessageV3>(
        override var parameters: List<KParameter>?,
        override var returnType: KType?,
        val function: (T) -> Unit): FunctionDefinition(parameters, returnType)

data class SnapshotFunction<T : GeneratedMessageV3>(
        override var parameters: List<KParameter>?,
        override var returnType: KType?,
        val function: () -> T): FunctionDefinition(parameters, returnType)
