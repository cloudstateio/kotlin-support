package io.cloudstate.kotlinsupport.tests

import com.example.shoppingcart.ShoppingCartProto
import com.example.shoppingcart.persistence.Domain
import com.google.protobuf.Descriptors
import com.google.protobuf.Empty
import io.cloudstate.javasupport.ServiceCall
import io.cloudstate.javasupport.ServiceCallFactory
import io.cloudstate.javasupport.eventsourced.CommandContext
import io.cloudstate.javasupport.eventsourced.EventContext
import io.cloudstate.javasupport.eventsourced.EventSourcedContext
import io.cloudstate.javasupport.eventsourced.SnapshotContext
import io.cloudstate.javasupport.impl.AnySupport
import io.cloudstate.kotlinsupport.annotations.EntityId
import io.cloudstate.kotlinsupport.annotations.eventsourced.*
import io.cloudstate.kotlinsupport.api.eventsourced.KotlinAnnotationBasedEventSourced
import io.cloudstate.kotlinsupport.logger
import org.junit.Test
import java.util.*
import java.util.stream.Collectors
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class KotlinAnnotationBasedEventSourcedTest {
    private val log = logger()
    private val prefer: AnySupport.Prefer = AnySupport.PREFER_JAVA()
    private val typeUrlPrefix: String = AnySupport.DefaultTypeUrlPrefix()
    private val params: Pair<AnySupport, KotlinAnnotationBasedEventSourced> = kotlinAnnotationBasedEventSourced()

    @Test
    fun `Validate EntityHandler Instance`() {
        val annotationHandler = params.second
        val context = createCreationContext()
        assertNotNull(annotationHandler.create(context))
    }

    @Test
    fun `Validate CommandHandlers Calls`(){
        val annotationHandler = params.second
        val creationContext = createCreationContext()
        val commandContext = createGetCartCommandContext()

        val handler = annotationHandler.create(creationContext)

        val expectResult = Optional.of(
            params.first.encodeJava(
                ShoppingCartProto.Cart.newBuilder()
                .addAllItems(mutableMapOf<String, ShoppingCartProto.LineItem?>().values)
                .build())
            )

        val command = params.first.encodeJava(Empty.getDefaultInstance())

        val result = handler.handleCommand(
                command,
                commandContext
        )
        assertTrue(result.isPresent)
        assertEquals(expectResult.get(), result.get())
        log.info("Result of Call GetCart is: ${result.get()}")
    }

    @Test
    fun `Validate EventHandlers Calls`(){
        val annotationHandler = params.second
        val creationContext = createCreationContext()
        val eventContext = createItemAddedEventContext()
        val commandContext = createGetCartCommandContext()
        val handler = annotationHandler.create(creationContext)
        val lineItem = Domain.LineItem.newBuilder()
                .setName("Foo")
                .setQuantity(10)
                .setProductId("Foo1")
                .build()
        val state = mutableMapOf<String, ShoppingCartProto.LineItem>()
        state["Foo1"] = ShoppingCartProto.LineItem.newBuilder()
                .setName("Foo")
                .setQuantity(10)
                .setProductId("Foo1")
                .build()
        val expectResult = Optional.of(
                params.first.encodeJava(
                    ShoppingCartProto.Cart.newBuilder()
                    .addAllItems(state.values)
                    .build())
                )
        val event = params.first.encodeJava(
                Domain.ItemAdded.newBuilder()
                        .setItem(lineItem)
                        .build())

        handler.handleEvent(event, eventContext)
        val command = params.first.encodeJava(Empty.getDefaultInstance())
        val result = handler.handleCommand(
                command,
                commandContext
        )
        assertTrue(result.isPresent)
        assertEquals(expectResult.get(), result.get())
        log.info("Result of Call GetCart is: ${result.get()}")
    }

    @Test
    fun `Validate SnapshotInvoker Calls`() {
        val annotationHandler = params.second
        val creationContext = createCreationContext()
        val snapshotContext: SnapshotContext = createSnapshotContext()
        val handler = annotationHandler.create(creationContext)

        val expectResult = Optional.of(
            params.first.encodeJava(
                Domain.Cart.newBuilder()
                    .addAllItems(mutableMapOf<String, Domain.LineItem?>().values)
                    .build())
        )

        val result = handler.snapshot(snapshotContext)
        assertTrue(result.isPresent)
        assertEquals(expectResult.get(), result.get())
        log.info("Result of Call Snapshot is: ${result.get()}")
    }

    private fun createSnapshotContext(): SnapshotContext = object: SnapshotContext{
        override fun entityId(): String = "test:1"

        override fun serviceCallFactory(): ServiceCallFactory {
            TODO("Not yet implemented")
        }

        override fun sequenceNumber(): Long {
            TODO("Not yet implemented")
        }

    }

    private fun createItemAddedEventContext(): EventContext? = object: EventContext{
        override fun entityId(): String = "test:1"

        override fun serviceCallFactory(): ServiceCallFactory {
            TODO("Not yet implemented")
        }

        override fun sequenceNumber(): Long {
            TODO("Not yet implemented")
        }

    }

    private fun createCreationContext(): EventSourcedContext? = object: EventSourcedContext{
        override fun entityId(): String = "teste:1"
        override fun serviceCallFactory(): ServiceCallFactory {
            TODO("Not yet implemented")
        }
    }

    private fun createGetCartCommandContext(): CommandContext = object: CommandContext{
        override fun forward(to: ServiceCall?) {
            TODO("Not yet implemented")
        }

        override fun commandId(): Long {
            TODO("Not yet implemented")
        }

        override fun entityId(): String = "test:1"

        override fun fail(errorMessage: String?) {
            TODO("Not yet implemented")
        }

        override fun commandName(): String = "GetCart"

        override fun serviceCallFactory(): ServiceCallFactory {
            TODO("Not yet implemented")
        }

        override fun effect(effect: ServiceCall?, synchronous: Boolean) {
            TODO("Not yet implemented")
        }

        override fun sequenceNumber(): Long {
            TODO("Not yet implemented")
        }

        override fun emit(event: Any?) {
            TODO("Not yet implemented")
        }

    }

    private fun kotlinAnnotationBasedEventSourced(): Pair<AnySupport, KotlinAnnotationBasedEventSourced> {
        val entity = TestEntity::class
        val descriptor = ShoppingCartProto.getDescriptor().findServiceByName("ShoppingCart")
        val additionalDescriptors = arrayOf(
                ShoppingCartProto.getDescriptor(), Domain.getDescriptor() )

        val anySupport = newAnySupport(additionalDescriptors)
        return Pair(anySupport, KotlinAnnotationBasedEventSourced(entity, anySupport, descriptor))
    }

    private fun newAnySupport(descriptors: Array<Descriptors.FileDescriptor>): AnySupport =
            AnySupport(descriptors, this.javaClass.classLoader, typeUrlPrefix, prefer)
}

@EventSourcedEntity
class TestEntity(@EntityId private val entityId: String) {
    private val cart: MutableMap<String, ShoppingCartProto.LineItem?> = mutableMapOf<String, ShoppingCartProto.LineItem?>()

    @Snapshot
    fun snapshot(): Domain.Cart =
        Domain.Cart.newBuilder()
            .addAllItems(
                cart.values.stream()
                    .map { item: ShoppingCartProto.LineItem? -> this.convert(item) }
                    .collect(Collectors.toList())
            )
            .build()

    @SnapshotHandler
    fun handleSnapshot(cart: Domain.Cart) {}

    @EventHandler
    fun itemAdded(itemAdded: Domain.ItemAdded) {
        var item = cart[itemAdded.item.productId]

        item = if (item == null) {
            convert(itemAdded.item)
        } else {
            item.toBuilder()
                .setQuantity(item.quantity + itemAdded.item.quantity)
                .build()
        }
        cart[item!!.productId] = item
    }

    @EventHandler
    fun itemRemoved(itemRemoved: Domain.ItemRemoved){}

    @CommandHandler
    fun getCart() = ShoppingCartProto.Cart.newBuilder().addAllItems(cart.values).build()

    @CommandHandler
    fun addItem() {}

    @CommandHandler
    fun removeItem() {}

    private fun convert(item: Domain.LineItem): ShoppingCartProto.LineItem =
        ShoppingCartProto.LineItem.newBuilder()
            .setProductId(item.productId)
            .setName(item.name)
            .setQuantity(item.quantity)
            .build()

    private fun convert(item: ShoppingCartProto.LineItem?): Domain.LineItem =
        Domain.LineItem.newBuilder()
            .setProductId(item!!.productId)
            .setName(item.name)
            .setQuantity(item.quantity)
            .build()
}