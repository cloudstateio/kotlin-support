package com.example.shoppingcart

import com.example.shoppingcart.persistence.Domain
import com.google.protobuf.Empty
import io.cloudstate.kotlinsupport.annotations.CommandHandler
import io.cloudstate.kotlinsupport.annotations.EventHandler

import io.cloudstate.kotlinsupport.cloudstate
import io.cloudstate.kotlinsupport.services.eventsourced.EventSourcedEntity
import java.util.stream.Collectors

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
                    entityService = ShoppingCartEntityService()

                    descriptor = Shoppingcart.getDescriptor().findServiceByName("ShoppingCart")
                    additionalDescriptors = arrayOf(
                            com.example.shoppingcart.persistence.Domain.getDescriptor())

                    //snapshotEvery = 1
                    persistenceId = "shopping-cart"
                }

                // registerCrdtEntity {  }

            }.start()
                .toCompletableFuture()
                .get()

        }
    }

    class ShoppingCartEntityService : EventSourcedEntity() {
        private var state: MutableMap<String, Shoppingcart.LineItem> = mutableMapOf()

        override fun create(): Handler {
            val handler = Handler()
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

                @EventHandler("itemAdded")
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

                @EventHandler("itemRemoved")
                eventHandler<Domain.ItemRemoved> { itemRemoved ->
                    state.remove(itemRemoved.productId)
                }

            }

            handler.commandResultHandlers = listOf() {

                @CommandHandler("getCart")
                commandResultHandler<Shoppingcart.Cart> {
                    Shoppingcart.Cart.newBuilder().addAllItems(state.values).build()
                }
            }

            handler.commandActionHandlers = listOf<(Any) -> Any>() {

                @CommandHandler("addItem")
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
