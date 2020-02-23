package io.cloudstate.kotlinsupport.tests

import com.example.shoppingcart.ShoppingCartProto
import com.example.shoppingcart.persistence.Domain
import com.google.protobuf.Empty
import io.cloudstate.javasupport.eventsourced.CommandContext
import io.cloudstate.kotlinsupport.api.eventsourced.EventSourcedHandler
import io.cloudstate.kotlinsupport.api.eventsourced.eventSourcedEntityBuilder
import io.cloudstate.kotlinsupport.logger
import org.junit.Test
import java.util.stream.Collectors

class EventSourcedDSLTest {
    private val log = logger()

    @Test
    fun `Create EventSourcedEntity`() {
        val eventSourcedEntityBuilder = eventSourcedEntityBuilder<MutableMap<String, ShoppingCartProto.LineItem?>> {

            // First configure EventSourcedEntity
            withState(initialState = mutableMapOf<String, ShoppingCartProto.LineItem?>()) {
                withPersistenceId("shopping-cart")
                withSnapshotEvery(20)
            }

            // Then write logic
            snapshot {
                initState()

                Domain.Cart.newBuilder()
                        .addAllItems(
                                state!!.values.stream()
                                        .map { item: ShoppingCartProto.LineItem? -> convert(item) }
                                        .collect(Collectors.toList())
                        )
                        .build()
            }

            /** Other form
            snapshot(name = "cartShot", function = Domain.Cart.newBuilder()
                    .addAllItems(
                            state!!.values.stream()
                                    .map { item: ShoppingCartProto.LineItem? -> convert(item) }
                                    .collect(Collectors.toList())
                    )::build)
             */

            snapshotHandler<Domain.Cart> { cart ->
                state!!.clear()
                for (item in cart.itemsList) {
                    state!![item.productId] = convert(item)
                }
            }

            eventHandler<Domain.ItemAdded> { itemAdded ->
                var item = state!![itemAdded.item.productId]

                item = if (item == null) {
                    convert(itemAdded.item)
                } else {
                    item.toBuilder()
                            .setQuantity(item.quantity + itemAdded.item.quantity)
                            .build()
                }
                state!![item!!.productId] = item
            }

            /** Other form
            eventHandler<Domain.ItemAdded>("itemAdded") { itemAdded ->
                var item = state!![itemAdded.item.productId]

                item = if (item == null) {
                    convert(itemAdded.item)
                } else {
                    item!!.toBuilder()
                            .setQuantity(item!!.quantity + itemAdded.item.quantity)
                            .build()
                }
                state!![item!!.productId] = item
            }

             // Or another form
            eventHandler<Domain.ItemAdded>("itemAdded", Any::class.java) { itemAdded ->
                var item = state!![itemAdded.item.productId]

                item = if (item == null) {
                     convert(itemAdded.item)
                } else {
                    item!!.toBuilder()
                    .setQuantity(item!!.quantity + itemAdded.item.quantity)
                    .build()
                }
                state!![item!!.productId] = item
            }
            */

            // Command Handler's
            commandHandler<ShoppingCartProto.Cart>("getCart") {
                ShoppingCartProto.Cart.newBuilder().addAllItems(state!!.values).build()
            }

            commandHandler<ShoppingCartProto.AddLineItem, CommandContext, Empty>("addItem") { item, ctx ->
                if (item.quantity <= 0) {
                    ctx.fail("Cannot add negative quantity of to item" + item.productId)
                }
                ctx.emit(Domain.ItemAdded.newBuilder()
                        .setItem(Domain.LineItem.newBuilder()
                                .setProductId(item.productId)
                                .setName(item.name)
                                .setQuantity(item.quantity)
                                .build())
                        .build())

                Empty.getDefaultInstance()
            }

            commandHandler<ShoppingCartProto.RemoveLineItem, CommandContext, Empty>("removeItem") { item, ctx ->
                if (!state!!.containsKey(item.productId)) ctx.fail("Cannot remove item ${item.productId} because it is not in the cart.")

                with(ctx) {
                    emit(Domain.ItemRemoved.newBuilder()
                            .setProductId(item.productId)
                            .build())
                }

                Empty.getDefaultInstance()
            }

        }

        val entityClazz: Class<*>? = eventSourcedEntityBuilder.build()

    }

    @Test(expected = IllegalArgumentException::class)
    fun `Throw  if the state has not been initialized`() {
        eventSourcedEntityBuilder<MutableMap<String, ShoppingCartProto.LineItem?>> {

            snapshot {
                initState()

                Domain.Cart.newBuilder()
                        .addAllItems(
                                state!!.values.stream()
                                        .map { item: ShoppingCartProto.LineItem? -> convert(item) }
                                        .collect(Collectors.toList())
                        )
                        .build()
            }

        }

    }

    private fun EventSourcedHandler<MutableMap<String, ShoppingCartProto.LineItem?>>.initState() {
        state!!["ProductId"] = ShoppingCartProto.LineItem.newBuilder()
                .setName("Product")
                .setProductId("ProductId")
                .setQuantity(1)
                .build()
    }

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