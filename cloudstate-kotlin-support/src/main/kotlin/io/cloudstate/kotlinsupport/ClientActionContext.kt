package io.cloudstate.kotlinsupport

interface ClientActionContext: Context {

    /**
     * Fail the command with the given message.
     *
     * @param errorMessage The error message to send to the client.
     */
    fun fail(errorMessage: String?): RuntimeException?

    /**
     * Instruct the proxy to forward handling of this command to another entity served by this
     * stateful function.
     *
     *
     * @param to The object call to forward command processing to.
     */
    fun forward(to: Any)
}