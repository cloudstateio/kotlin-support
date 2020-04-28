# kotlin-support

User Language Support for the Kotlin Programming Language

## Install the Kotlin Support Library to the local Maven repository

`./gradlew build publishToMavenLocal`

## Examples: build and push container images to a container registry

`./gradlew :examples:kotlin-chat:jib`

`./gradlew :examples:kotlin-pingpong:jib`

`./gradlew :examples:shopping-cart:jib`

## EventSourcing example of use

### Define your proto

@@snip [shoppingcart.proto](/examples/shopping-cart/src/main/proto/shoppingcart.proto) { #example-shopping-cart-proto }

### Write your business logic

@@snip [ShoppingCartEntity.kt](/examples/shopping-cart/src/main/kotlin/com/example/shoppingcart/ShoppingCartEntity.kt) { #example-shopping-cart-kotlin }

### Register your Entity

@@snip [Main.kt](/examples/shopping-cart/src/main/kotlin/com/example/shoppingcart/Main.kt) { #example-shopping-cart-main }
