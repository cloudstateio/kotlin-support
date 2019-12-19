package io.cloudstate.kotlinsupport;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.grpc.javadsl.ServiceHandler;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.japi.Function;
import akka.stream.ActorMaterializer;
import io.cloudstate.kotlinsupport.initializers.CloudStateInitializer;
import io.cloudstate.kotlinsupport.protocol.EntityDiscoveryService;
import io.cloudstate.kotlinsupport.protocol.EventSourcedService;
import io.cloudstate.kotlinsupport.protocol.handlers.EventSourcedEntityHandler;
import io.cloudstate.kotlinsupport.protocol.handlers.EventSourcedRouterHandler;
import io.cloudstate.kotlinsupport.services.eventsourced.EventSourcedEntity;
import io.cloudstate.protocol.EntityDiscoveryHandlerFactory;
import io.cloudstate.protocol.EventSourcedHandlerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

public class CloudStateRunner {
    private static Logger log = LoggerFactory.getLogger(CloudStateRunner.class);

    private CloudStateInitializer initializer;
    private Map<String, CloudStateInitializer.EntityFunction> services;
    private ActorSystem system;
    private ActorMaterializer materializer;

    public CloudStateRunner(CloudStateInitializer initializer, Map<String, CloudStateInitializer.EntityFunction> services, ActorSystem system, ActorMaterializer materializer) {
        this.initializer = initializer;
        this.services = services;
        this.system = system;
        this.materializer = materializer;
    }

    public CloudStateInitializer getInitializer() {
        return initializer;
    }

    public void setInitializer(CloudStateInitializer initializer) {
        this.initializer = initializer;
    }

    public ActorSystem getSystem() {
        return system;
    }

    public void setSystem(ActorSystem system) {
        this.system = system;
    }

    public ActorMaterializer getMaterializer() {
        return materializer;
    }

    public void setMaterializer(ActorMaterializer materializer) {
        this.materializer = materializer;
    }

    public CompletionStage<ServerBinding> start() throws Exception {
        log.debug("Found {} services for register", services.size());

        final List<EntityWrapper> eventSourcedServices = services.entrySet()
                .stream().filter(e -> EntityType.EventSourced.equals(e.getValue().getInitializer().getEntityType()))
                .map(e -> e.getValue())
                .map(func -> {
                    ActorRef ref = system.actorOf(Props.create(EventSourcedEntityHandler.class, func), func.getEntityName());
                    return new EntityWrapper(func, ref);
                })
                .collect(Collectors.toList());

        final ActorRef handlerActor = system.actorOf(Props.create(EventSourcedRouterHandler.class, eventSourcedServices));

        Function<HttpRequest, CompletionStage<HttpResponse>> serviceHandlers = getHttpRequestCompletionStageFunction(handlerActor);

        CompletionStage<ServerBinding> bound = Http.get(system).bindAndHandleAsync(
                serviceHandlers,
                ConnectHttp.toHost(initializer.getHost(), initializer.getPort()),
                materializer);

        bound.thenAccept(binding -> log.info("gRPC server bound to: {}", binding.localAddress()) );

        return bound;
    }

    private Function<HttpRequest, CompletionStage<HttpResponse>> getHttpRequestCompletionStageFunction(ActorRef handlerActor) {
        Function<HttpRequest, CompletionStage<HttpResponse>> entityDiscoveryService =
                EntityDiscoveryHandlerFactory.create(new EntityDiscoveryService(initializer, services, materializer), materializer, system);

        Function<HttpRequest, CompletionStage<HttpResponse>> eventSourcedService =
                EventSourcedHandlerFactory.create(new EventSourcedService(initializer, handlerActor), materializer, system);

        return ServiceHandler.concatOrNotFound(entityDiscoveryService, eventSourcedService);
    }

    public class EntityWrapper{
        private CloudStateInitializer.EntityFunction entityFunction;
        private ActorRef ref;

        public EntityWrapper(CloudStateInitializer.EntityFunction entityFunction, ActorRef ref) {
            this.entityFunction = entityFunction;
            this.ref = ref;
        }

        public CloudStateInitializer.EntityFunction getEntityFunction() {
            return entityFunction;
        }

        public void setEntityFunction(CloudStateInitializer.EntityFunction entityFunction) {
            this.entityFunction = entityFunction;
        }

        public ActorRef getRef() {
            return ref;
        }

        public void setRef(ActorRef ref) {
            this.ref = ref;
        }
    }

}
