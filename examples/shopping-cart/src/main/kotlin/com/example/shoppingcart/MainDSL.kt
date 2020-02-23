package com.example.shoppingcart

import com.example.shoppingcart.persistence.Domain
import com.google.protobuf.Empty
import io.cloudstate.javasupport.eventsourced.CommandContext
import io.cloudstate.kotlinsupport.api.eventsourced.EventSourcedBuilder
import io.cloudstate.kotlinsupport.api.eventsourced.eventSourcedEntityBuilder
import io.cloudstate.kotlinsupport.cloudstate
import java.util.stream.Collectors

fun main(args: Array<String>) {

    val build = createEntityBuilder().build()
    println("Class name -> ${build?.name}")
    build?.methods?.forEach { method ->
        println("Found method -> ${method.name} with return type ${method.returnType.simpleName} ")
        method.annotations.forEach { annotation -> println("With annotations $annotation") }
    }

    cloudstate {
        registerEventSourcedEntity {
            entityService = build
            descriptor = Shoppingcart.getDescriptor().findServiceByName("ShoppingCart")
            additionalDescriptors = arrayOf( Domain.getDescriptor() )
        }

    }.start()
            .toCompletableFuture()
            .get()
}

private fun createEntityBuilder(): EventSourcedBuilder<MutableMap<String, ShoppingCartProto.LineItem?>> {
    return eventSourcedEntityBuilder<MutableMap<String, ShoppingCartProto.LineItem?>> {

        // First configure EventSourcedEntity
        withState(initialState = mutableMapOf<String, ShoppingCartProto.LineItem?>()) {
            withPersistenceId("shopping-cart")
            withSnapshotEvery(20)
        }

        // Then write logic
        snapshot {
            Domain.Cart.newBuilder()
                    .addAllItems(
                            state!!.values.stream()
                                    .map { item: ShoppingCartProto.LineItem? -> convert(item) }
                                    .collect(Collectors.toList())
                    )
                    .build()
        }

        snapshotHandler<Domain.Cart> { cart ->
            state!!.clear()
            for (item in cart.itemsList) {
                state!![item.productId] = convert(item)
            }
        }

        eventHandler<Domain.ItemAdded>(name = "itemAdded") { itemAdded ->
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

        eventHandler<Domain.ItemRemoved>(name = "itemRemoved") { itemRemoved -> state!!.remove(itemRemoved.productId) }

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