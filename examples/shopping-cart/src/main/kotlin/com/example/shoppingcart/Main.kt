// #example-shopping-cart-main
package com.example.shoppingcart

import ShoppingCartEntity
import com.example.shoppingcart.persistence.Domain
import io.cloudstate.kotlinsupport.cloudstate

fun main() {

    cloudstate {

        config {
            host = "0.0.0.0"
            port = 8080
            loglevel = "INFO"
        }

        eventsourced {
            entityService = ShoppingCartEntity::class
            descriptor = Shoppingcart.getDescriptor().findServiceByName("ShoppingCart")
            additionalDescriptors = mutableListOf(Shoppingcart.getDescriptor(), Domain.getDescriptor() )
            snapshotEvery = 1
            persistenceId = "shopping-cart"
        }

    }.start()
            .toCompletableFuture()
            .get()
}
// #example-shopping-cart-main
