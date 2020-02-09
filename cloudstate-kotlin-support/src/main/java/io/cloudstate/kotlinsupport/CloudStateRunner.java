package io.cloudstate.kotlinsupport;

import akka.Done;

import io.cloudstate.kotlinsupport.initializers.CloudStateInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletionStage;

public class CloudStateRunner {
    private static Logger log = LoggerFactory.getLogger(CloudStateRunner.class);

    private CloudStateInitializer initializer;

    public CloudStateRunner(CloudStateInitializer initializer) {
        this.initializer = initializer;
    }

    public CloudStateInitializer getInitializer() {
        return initializer;
    }

    public CompletionStage<Done> start() {
        return initializer.getEngine().start();
    }

}
