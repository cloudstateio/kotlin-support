package com.example.shoppingcart

import com.example.shoppingcart.persistence.Domain
import com.google.protobuf.Empty
import io.cloudstate.kotlinsupport.annotations.CommandHandler
import io.cloudstate.kotlinsupport.annotations.EventHandler
import io.cloudstate.kotlinsupport.annotations.Snapshot
import io.cloudstate.kotlinsupport.annotations.SnapshotHandler
import io.cloudstate.kotlinsupport.cloudstate
import io.cloudstate.kotlinsupport.services.eventsourced.AnnotationEventSourcedEntity
import io.cloudstate.kotlinsupport.services.eventsourced.FunctionalEventSourcedEntity
import java.util.stream.Collectors
import com.example.shoppingcart.persistence.Domain.ItemRemoved as ItemRemoved

class Main {

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {

            cloudstate {

                serviceName = "shopping-cart"
                serviceVersion = "1.0.0"

                //host = "0.0.0.0"
                //port = 8088

                registerEventSourcedEntity {
                    entityService = ShoppingCartAnnotationEntity::class.java

                    descriptor = Shoppingcart.getDescriptor().findServiceByName("ShoppingCart")
                    additionalDescriptors = arrayOf(
                            com.example.shoppingcart.persistence.Domain.getDescriptor())

                    snapshotEvery = 1
                    persistenceId = "shopping-cart"
                }

                // registerCrdtEntity {  }

            }.start()
                .toCompletableFuture()
                .get()

        }
    }

    class ShoppingCartAnnotationEntity(entityId: String) : AnnotationEventSourcedEntity(entityId) {
        private var state: MutableMap<String, Shoppingcart.LineItem> = mutableMapOf()

        @Snapshot
        fun snapshot(): Domain.Cart? = Domain.Cart.newBuilder()
                .addAllItems(state.values.stream().map(this::convert).collect(Collectors.toList()))
                .build()

        @SnapshotHandler
        fun handleSnapshot(cart: Domain.Cart) {
            state.clear()
            for (item in cart.itemsList) convert(item)?.let { state.put(item.productId, it) }
        }

        @EventHandler
        fun itemAdded(itemAdded: Domain.ItemAdded) {
            var item: Shoppingcart.LineItem? = state[itemAdded.item.productId]
            item = if (item == null) {
                convert(itemAdded.item)
            } else {
                item.toBuilder()
                        .setQuantity(item.quantity + itemAdded.item.quantity)
                        .build()
            }
            state[item!!.productId] = item
        }

        @EventHandler
        fun itemRemoved(itemRemoved: ItemRemoved) {
            state.remove(itemRemoved.productId)
        }

        @CommandHandler
        fun getCart(): Shoppingcart.Cart? {
            return Shoppingcart.Cart.newBuilder().addAllItems(state.values).build()
        }

        @CommandHandler
        fun addItem(item: Shoppingcart.AddLineItem): Empty? {
            if (item.quantity <= 0) fail("Cannot add negative quantity of to item ${item.productId}")

            emit(
                    Domain.ItemAdded.newBuilder()
                            .setItem(
                                    Domain.LineItem.newBuilder()
                                            .setProductId(item.productId)
                                            .setName(item.name)
                                            .setQuantity(item.quantity)
                                            .build())
                            .build())
            return Empty.getDefaultInstance()
        }

        @CommandHandler
        fun removeItem(item: Shoppingcart.RemoveLineItem): Empty? {
            if (!state.containsKey(item.productId)) fail("Cannot remove item ${item.productId} because it is not in the cart.")

            emit(ItemRemoved.newBuilder().setProductId(item.productId).build())
            return Empty.getDefaultInstance()
        }

        private fun convert(item: Domain.LineItem): Shoppingcart.LineItem? {
            return Shoppingcart.LineItem.newBuilder()
                    .setProductId(item.productId)
                    .setName(item.name)
                    .setQuantity(item.quantity)
                    .build()
        }

        private fun convert(item: Shoppingcart.LineItem): Domain.LineItem? {
            return Domain.LineItem.newBuilder()
                    .setProductId(item.productId)
                    .setName(item.name)
                    .setQuantity(item.quantity)
                    .build()
        }

    }

    class ShoppingCartFunctionalEntity(entityId: String) : FunctionalEventSourcedEntity(entityId) {
        private var state: MutableMap<String, Shoppingcart.LineItem> = mutableMapOf()

        override fun create(): Handler {
            //Simulate initial data -> state[entityId] = Shoppingcart.LineItem.newBuilder().setName("item").build()

            val handler = Handler(entityId)
            handler.snapshot = {
                snapshot<Domain.Cart> {
                    Domain.Cart.newBuilder()
                            .addAllItems(
                                    state.values.stream()
                                            .map{ convert(it) }
                                            .collect(Collectors.toList<Domain.LineItem>()))
                            .build()
                }
            }

            handler.handleSnapshot = {
                handleSnapshot<Domain.Cart> { cart ->
                    state.clear()
                    for (item in cart.itemsList) {
                        convert(item)?.let { it1 -> state[item.productId] = it1  }
                    }
                }
            }

            handler.eventHandlers = listOf<(Any) -> Unit> {

                eventHandler<Domain.ItemAdded> { itemAdded ->
                    var item = state[itemAdded.item.productId]
                    item = if (item == null) {
                        convert(itemAdded.item)
                    } else {
                        item.toBuilder()
                                .setQuantity(item.quantity + itemAdded.item.quantity)
                                .build()
                    }
                    state[item!!.productId] = item
                }

                eventHandler<ItemRemoved> { itemRemoved ->
                    state.remove(itemRemoved.productId)
                }

            }

            handler.commandResultHandlers = listOf() {

                commandResultHandler<Shoppingcart.Cart> {
                    Shoppingcart.Cart.newBuilder().addAllItems(state.values).build()
                }
            }

            handler.commandActionHandlers = listOf<(Any) -> Any>() {

                commandActionHandler<Shoppingcart.AddLineItem> { item ->
                    if (item.quantity <= 0) {
                        this fail "Cannot add negative quantity of to item" + item.productId
                    }

                    this emit Domain.ItemAdded.newBuilder()
                            .setItem(
                                    Domain.LineItem.newBuilder()
                                            .setProductId(item.productId)
                                            .setName(item.name)
                                            .setQuantity(item.quantity)
                                            .build())
                            .build()

                    Empty.getDefaultInstance()
                }

            }

            return handler
        }

        private fun convert(item: Domain.LineItem): Shoppingcart.LineItem? {
            return Shoppingcart.LineItem.newBuilder()
                    .setProductId(item.productId)
                    .setName(item.name)
                    .setQuantity(item.quantity)
                    .build()
        }

        private fun convert(item: Shoppingcart.LineItem): Domain.LineItem? {
            return Domain.LineItem.newBuilder()
                    .setProductId(item.productId)
                    .setName(item.name)
                    .setQuantity(item.quantity)
                    .build()
        }


    }


}
