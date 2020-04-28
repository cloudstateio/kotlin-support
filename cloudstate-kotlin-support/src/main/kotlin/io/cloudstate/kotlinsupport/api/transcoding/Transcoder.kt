package io.cloudstate.kotlinsupport.api.transcoding

interface Transcoder {
    fun transcode(): Class<*>?
}
