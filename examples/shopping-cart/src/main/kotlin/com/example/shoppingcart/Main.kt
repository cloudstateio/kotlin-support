// #example-shopping-cart-main
package com.example.shoppingcart

import ShoppingCartEntity
import com.example.shoppingcart.persistence.Domain
import io.cloudstate.kotlinsupport.cloudstate

class Main {
    companion object {

        @JvmStatic
        fun main(args: Array<String>) {

            cloudstate {

                host = "0.0.0.0"
                port = 8080
                loglevel = "INFO"

                registerEventSourcedEntity {
                    entityService = ShoppingCartEntity::class

                    descriptor = Shoppingcart.getDescriptor().findServiceByName("ShoppingCart")
                    additionalDescriptors = arrayOf( Domain.getDescriptor() )

                    snapshotEvery = 1
                    persistenceId = "shopping-cart"
                }

            }.start()
                    .toCompletableFuture()
                    .get()
        }

    }
}
// #example-shopping-cart-main
