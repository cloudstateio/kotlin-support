package com.example.shoppingcart

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
                    entityService = ShoppingCartEntity::class.java

                    descriptor = Shoppingcart.getDescriptor().findServiceByName("ShoppingCart")
                    additionalDescriptors = arrayOf( Domain.getDescriptor() )

                    snapshotEvery = 1
                    persistenceId = "shopping-cart"
                }

                // registerCrdtEntity {  }

            }.start()
                    .toCompletableFuture()
                    .get()
        }

    }
}