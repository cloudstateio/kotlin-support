package io.cloudstate.kotlinsupport.api.crdt

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Marks a method as a command handler.
 *
 *
 * This method will be invoked whenever the service call with name that matches this command
 * handlers name is invoked.
 *
 *
 * The method may take the command object as a parameter, its type must match the gRPC service
 * input type.
 *
 *
 * The return type of the method must match the gRPC services output type.
 *
 *
 * The method may also take a [CommandContext], and/or a [ ] annotated [String] parameter.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(RetentionPolicy.RUNTIME)
annotation class CommandHandler(
        /**
         * The name of the command to handle.
         *
         *
         * If not specified, the name of the method will be used as the command name, with the first
         * letter capitalized to match the gRPC convention of capitalizing rpc method names.
         *
         * @return The command name.
         */
        val name: String = "")