package nh.graphql.beeradvisor.graphql;

import graphql.ExecutionResult;
import graphql.ExecutionResultImpl;
import graphql.GraphQLError;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.SimpleInstrumentation;
import graphql.execution.instrumentation.SimpleInstrumentationContext;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters;
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters;
import graphql.execution.instrumentation.tracing.TracingSupport;
import graphql.kickstart.execution.BatchedDataLoaderGraphQLBuilder;
import graphql.kickstart.execution.GraphQLInvoker;
import graphql.kickstart.execution.GraphQLObjectMapper;
import graphql.kickstart.execution.GraphQLQueryInvoker;
import graphql.kickstart.execution.config.GraphQLBuilder;
import graphql.kickstart.execution.context.GraphQLContext;
import graphql.kickstart.execution.error.DefaultGraphQLErrorHandler;
import graphql.kickstart.servlet.GraphQLConfiguration;
import graphql.kickstart.servlet.GraphQLHttpServlet;
import graphql.kickstart.servlet.GraphQLWebsocketServlet;
import graphql.kickstart.servlet.context.DefaultGraphQLServletContextBuilder;
import graphql.kickstart.servlet.input.GraphQLInvocationInputFactory;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLSchema;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.Lifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;
import org.springframework.web.socket.server.standard.ServerEndpointRegistration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.HandshakeResponse;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import static graphql.execution.instrumentation.SimpleInstrumentationContext.whenCompleted;

/**
 * This configuration requires graphql-java-servlet
 *
 * @author Nils Hartmann (nils@nilshartmann.net)
 */
@ConditionalOnWebApplication
@Configuration
public class GraphQLEndpointConfiguration {

    class BeerAdvisorContextBuilder extends DefaultGraphQLServletContextBuilder {

        private final BeerAdvisorDataLoaderConfigurer dataLoaderConfigurer;

        private BeerAdvisorContextBuilder(BeerAdvisorDataLoaderConfigurer dataLoaderConfigurer) {
            this.dataLoaderConfigurer = dataLoaderConfigurer;
        }


        private GraphQLContext setupDataLoaderRegistry(final GraphQLContext context) {
            final var dataLoaderRegistry = context.getDataLoaderRegistry()
                .orElseThrow(() -> new IllegalStateException("DataLoaderRegistry must be set!"));

            dataLoaderConfigurer.configureDataLoader(dataLoaderRegistry);

            return context;
        }


        @Override
        public GraphQLContext build(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
            GraphQLContext context = super.build(httpServletRequest, httpServletResponse);
            return setupDataLoaderRegistry(context);
        }

        @Override
        public GraphQLContext build(Session session, HandshakeRequest handshakeRequest) {
            GraphQLContext context = super.build(session, handshakeRequest);
            return setupDataLoaderRegistry(context);
        }

        @Override
        public GraphQLContext build() {
            GraphQLContext context = super.build();
            return setupDataLoaderRegistry(context);
        }
    }

    // --- SERVLET --------------------------------------------------------------------------------------------------
    @Bean
    ServletRegistrationBean<GraphQLHttpServlet> graphQLServletRegistrationBean(GraphQLSchema schema, BeerAdvisorDataLoaderConfigurer dataLoaderConfigurer) {

        GraphQLConfiguration config = GraphQLConfiguration
            .with(schema)
            .with(GraphQLObjectMapper.newBuilder()
                .withGraphQLErrorHandler(new MyDefaultGraphQLErrorHandler())
                .build())
            .with(new BeerAdvisorContextBuilder(dataLoaderConfigurer))
            .with(GraphQLQueryInvoker.newBuilder()
                .withInstrumentation(new CustomInstrumentation())
                .build())
            .build();

        return new ServletRegistrationBean<>(GraphQLHttpServlet.with(config), "/graphql");
    }

    // --- WEB SOCKET (for Subscriptions) ---------------------------------------------------------------------------
    @Bean
    public ServerEndpointRegistration serverEndpointRegistration(GraphQLSchema schema, BeerAdvisorDataLoaderConfigurer dataLoaderConfigurer) {
        final GraphQLQueryInvoker queryInvoker = GraphQLQueryInvoker.newBuilder().build();

        final GraphQLInvocationInputFactory graphQLInvocationInputFactory = GraphQLInvocationInputFactory
            .newBuilder(schema)
            .withGraphQLContextBuilder(new BeerAdvisorContextBuilder(dataLoaderConfigurer))
            .build();


        final GraphQLWebsocketServlet websocketServlet = new GraphQLWebsocketServlet(
            new GraphQLInvoker(new GraphQLBuilder(), new BatchedDataLoaderGraphQLBuilder(null)),
            graphQLInvocationInputFactory,
            GraphQLObjectMapper.newBuilder()
                .withGraphQLErrorHandler(new MyDefaultGraphQLErrorHandler())
                .build()
        );
        return new GraphQLWsServerEndpointRegistration("/subscriptions", websocketServlet);
    }

    @Bean
    @ConditionalOnMissingBean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }

    class GraphQLWsServerEndpointRegistration extends ServerEndpointRegistration implements Lifecycle {

        private final GraphQLWebsocketServlet servlet;

        public GraphQLWsServerEndpointRegistration(String path, GraphQLWebsocketServlet servlet) {
            super(path, servlet);
            this.servlet = servlet;
        }

        @Override
        public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
            super.modifyHandshake(sec, request, response);
            servlet.modifyHandshake(sec, request, response);
        }

        @Override
        public void start() {
        }

        @Override
        public void stop() {
            servlet.beginShutDown();
        }

        @Override
        public boolean isRunning() {
            return !servlet.isShutDown();
        }
    }

    class MyDefaultGraphQLErrorHandler extends DefaultGraphQLErrorHandler {
        @Override
        protected boolean isClientError(GraphQLError error) {
            // For demonstration: include ALL errors in 'errors' field
            // (graphql-servlet otherwise filters some out)
            return true;
        }
    }

    class ExecutionTimeMeasureInstrumentation implements InstrumentationState {
        private Map<String, Long> measuredTimes = new HashMap<>();

        void recordTiming(String key, long time) {
            measuredTimes.put(key, time);
        }

        public Map<String, Long> snapshotTracingData() {
//            long ms = measuredTimes.values()
//                .stream()
//                .reduce(Long::sum).
//                    get();
//
//            Map<String, Long> durations = new HashMap<>();
//            durations.put("ms", ms);
//            durations.put("sec", ms / 1000);
//            return durations;
            return measuredTimes;
         }
    }

    class CustomInstrumentation extends SimpleInstrumentation {
        @Override
        public InstrumentationState createState() {
            //
            // instrumentation state is passed during each invocation of an Instrumentation method
            // and allows you to put stateful data away and reference it during the query execution
            //
            return new ExecutionTimeMeasureInstrumentation();
        }

        @Override
        public InstrumentationContext<ExecutionResult> beginExecution(InstrumentationExecutionParameters parameters) {
            long startNanos = System.currentTimeMillis();
            return new SimpleInstrumentationContext<ExecutionResult>() {
                @Override
                public void onCompleted(ExecutionResult result, Throwable t) {
                    ExecutionTimeMeasureInstrumentation state = parameters.getInstrumentationState();
                    state.recordTiming("OVER_ALL", System.currentTimeMillis() - startNanos);
                }
            };
        }

        @Override
        public InstrumentationContext<Object> beginFieldFetch(InstrumentationFieldFetchParameters parameters) {
            final ExecutionTimeMeasureInstrumentation state = parameters.getInstrumentationState();
            if (parameters.getExecutionStepInfo().getPath().getLevel() != 1) {
                return whenCompleted((result, t) -> {});
            }
            final long l = System.currentTimeMillis();
            final String field = parameters.getEnvironment().getField().getName();
            return whenCompleted((result, t) -> {
                final long duration = System.currentTimeMillis() - l;
                state.recordTiming(field, duration);
            });
        }

        @Override
        public CompletableFuture<ExecutionResult> instrumentExecutionResult(ExecutionResult executionResult, InstrumentationExecutionParameters parameters) {
            ExecutionTimeMeasureInstrumentation state = parameters.getInstrumentationState();

            Map<Object, Object> tracingMap = new LinkedHashMap<>();
            tracingMap.put("duration_ms", state.snapshotTracingData());

            return CompletableFuture.completedFuture(new ExecutionResultImpl(executionResult.getData(), executionResult.getErrors(), tracingMap));
        }
    }
}
