package io.cloudstate.kotlinsupport.services.eventsourced

import io.cloudstate.kotlinsupport.ClientActionContext

interface EventSourcedEntityHandlerContext: ClientActionContext {

    /**
     * Handle the given event.
     *
     * @param event The event to handle.
     * @param context The event context.
     */
    fun handleEvent(event: com.google.protobuf.Any?)

    /**
     * Handle the given command.
     *
     * @param command The command to handle.
     * @param context The command context.
     * @return The reply to the command, if the command isn't being forwarded elsewhere.
     */
    fun handleCommand(command: com.google.protobuf.Any?): com.google.protobuf.Any?

    /**
     * Handle the given snapshot.
     *
     * @param snapshot The snapshot to handle.
     * @param context The snapshot context.
     */
    fun handleSnapshot(snapshot: com.google.protobuf.Any?)

    /**
     * Snapshot the object.
     *
     * @return The current snapshot, if this object supports snapshoting, otherwise empty.
     */
    fun snapshot(): com.google.protobuf.Any?
}