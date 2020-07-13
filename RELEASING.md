# Releasing

1. Wait for any running [Travis builds](https://travis-ci.com/github/cloudstateio/kotlin-support/builds) to complete.

2. Create a [release and tag](https://github.com/cloudstateio/kotlin-support/releases) for the next version.
   **Note**: this repository uses tags without a `v` prefix, just use a version tag like `1.2.3`.

3. Travis will start a [build](https://travis-ci.com/github/cloudstateio/kotlin-support/builds) and publish to Bintray, and then sync to Maven Central.
