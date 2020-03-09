package io.cloudstate.kotlinsupport.transcoding.crdt;

import io.cloudstate.javasupport.crdt.CrdtEntity;

import java.lang.annotation.Annotation;

public class CrdtEntityImpl implements CrdtEntity {
    public Class<? extends Annotation> annotationType() {
        return CrdtEntity.class;
    }
}
