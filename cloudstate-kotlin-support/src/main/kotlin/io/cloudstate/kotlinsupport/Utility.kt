package io.cloudstate.kotlinsupport

import org.slf4j.Logger
import org.slf4j.LoggerFactory.getLogger
import java.io.IOException
import java.io.InputStream
import java.lang.reflect.Field
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

@Throws(Exception::class)
fun setEnv(newenv: Map<String, String>?) {
    try {
        val processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment")
        val theEnvironmentField: Field = processEnvironmentClass.getDeclaredField("theEnvironment")
        theEnvironmentField.setAccessible(true)
        val env = theEnvironmentField.get(null) as MutableMap<String, String>
        env.putAll(newenv!!)
        val theCaseInsensitiveEnvironmentField: Field = processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment")
        theCaseInsensitiveEnvironmentField.setAccessible(true)
        val cienv = theCaseInsensitiveEnvironmentField.get(null) as MutableMap<String, String>
        cienv.putAll(newenv)
    } catch (e: NoSuchFieldException) {
        val classes = Collections::class.java.declaredClasses
        val env = System.getenv()
        for (cl in classes) {
            if ("java.util.Collections\$UnmodifiableMap" == cl.name) {
                val field: Field = cl.getDeclaredField("m")
                field.isAccessible = true
                val obj: Any = field.get(env)
                val map = obj as MutableMap<String, String>
                map.clear()
                map.putAll(newenv!!)
            }
        }
    }
}
