package io.cloudstate.examples.chat.presence

import com.google.protobuf.Empty

import io.cloudstate.examples.chat.presence.PresenceProtos.OnlineStatus
import io.cloudstate.examples.chat.presence.PresenceProtos.User

import io.cloudstate.javasupport.crdt.*

import io.cloudstate.kotlinsupport.annotations.crdt.CrdtEntity
import io.cloudstate.kotlinsupport.annotations.crdt.CommandHandler

import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A presence entity.
 *
 *
 * There will be at most one of these on each node per user. It holds the CRDT, which is a Vote CRDT, which
 * allows us to register this nodes vote as to whether the user is currently online. The Vote CRDT then tells
 * us how many nodes have voted, and if at least one of them voted true, we know the user is online.
 */
@CrdtEntity
class PresenceEntity(ctx: CrdtCreationContext) {

    /**
     * The vote CRDT.
     */
    private val vote: Vote = ctx.newVote()


    /**
     * The number of users currently connected to this node. Note, this is only the users on this node, not the users
     * on all nodes together.
     */
    private var currentUsers = 0

    /**
     * Handler for the connect command.
     *
     *
     * A user is online as long as there is an active streamed connection for them to at least one node.
     *
     *
     * Note the input for this command is the User to monitor. The proxy has already inspected that, extracted the
     * username as the entity id, and directed the request to this entity, so there's no useful information for us
     * to handle here, hence we don't need to declare the command as a parameter to this method. Also, when a user
     * is connected, we don't send them anything in response, so the output of this command handler is void.
     */
    @CommandHandler
    fun connect(ctx: StreamedCommandContext<Empty?>) {
        currentUsers += 1
        if (currentUsers == 1) {
            // If the number of users on this node when up from zero, then we change our vote.
            vote.vote(true)
        }

        // Register a callback for when the user disconnects.
        ctx.onCancel { _: StreamCancelledContext? ->

            currentUsers -= 1
            if (currentUsers == 0) {
                // If the number of users on this node went down to zero, then we change our vote.
                vote.vote(false)
            }
        }
    }

    /**
     * Handler for the monitor command.
     */
    @CommandHandler
    fun monitor(user: User, ctx: StreamedCommandContext<OnlineStatus>): OnlineStatus {
        val lastStatus = AtomicBoolean(vote.isAtLeastOne)

        // Register a callback so that whenever the vote changes, we can handle it.
        ctx.onChange { _: SubscriptionContext? ->

            // We should check that the status has actually changed, the notification could just be that the
            // user has connected on another node, or another node has come online.
            when {
                lastStatus.get() != vote.isAtLeastOne -> {
                    lastStatus.set(vote.isAtLeastOne)
                    println("monitor: ${user.name} return {${vote.isAtLeastOne}}")
                    Optional.of<OnlineStatus>(statusMessage())
                }
                else -> {
                    println("monitor: ${user.name} status unchanged")
                    Optional.empty<OnlineStatus>()
                }
            }
        }
        return statusMessage()
    }

    /**
     * Convenience method for building the OnlineStatus message for the current status.
     */
    private fun statusMessage(): OnlineStatus = OnlineStatus.newBuilder()
                .setOnline(vote.isAtLeastOne)
                .build()

}