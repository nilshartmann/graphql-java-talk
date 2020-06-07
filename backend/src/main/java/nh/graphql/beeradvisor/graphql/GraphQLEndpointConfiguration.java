package nh.graphql.beeradvisor.graphql;

import graphql.kickstart.execution.BatchedDataLoaderGraphQLBuilder;
import graphql.kickstart.execution.GraphQLInvoker;
import graphql.kickstart.execution.GraphQLObjectMapper;
import graphql.kickstart.execution.GraphQLQueryInvoker;
import graphql.kickstart.execution.config.GraphQLBuilder;
import graphql.kickstart.execution.context.GraphQLContext;
import graphql.kickstart.servlet.GraphQLConfiguration;
import graphql.kickstart.servlet.GraphQLHttpServlet;
import graphql.kickstart.servlet.GraphQLWebsocketServlet;
import graphql.kickstart.servlet.context.DefaultGraphQLServletContextBuilder;
import graphql.kickstart.servlet.input.GraphQLInvocationInputFactory;
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
import java.util.Optional;

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
            .with(new BeerAdvisorContextBuilder(dataLoaderConfigurer))
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
            GraphQLObjectMapper.newBuilder().build()
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
}
