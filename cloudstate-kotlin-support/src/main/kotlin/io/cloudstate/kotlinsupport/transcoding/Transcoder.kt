package io.cloudstate.kotlinsupport.transcoding

interface Transcoder {

    fun transcode(): Class<*>?

}