package io.cloudstate.kotlinsupport

import org.slf4j.Logger
import org.slf4j.LoggerFactory.getLogger
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.*

val topLevelClass: Class<*> = object : Any() {}.javaClass.enclosingClass

fun <T : Any> T.logger(): Logger = getLogger(javaClass)

fun getProjectVersion(): String {
    val path = "/version.prop"

    var stream: InputStream = topLevelClass.getResourceAsStream(path) //File(path).inputStream() ?: return "UNKNOWN"

    var properties: Properties = Properties();
    return try {
        properties.load(stream);
        stream.close();
        properties.get("version") as String;
    } catch (e: IOException) {
        "UNKNOWN";
    }
}
