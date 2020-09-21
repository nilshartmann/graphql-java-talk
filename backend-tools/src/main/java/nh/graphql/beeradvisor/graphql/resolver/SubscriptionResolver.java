package nh.graphql.beeradvisor.graphql.resolver;

import graphql.kickstart.tools.GraphQLSubscriptionResolver;
import graphql.schema.DataFetcher;
import nh.graphql.beeradvisor.domain.Rating;
import nh.graphql.beeradvisor.graphql.subscription.RatingPublisher;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionResolver implements GraphQLSubscriptionResolver {
    private final Logger logger = LoggerFactory.getLogger(SubscriptionResolver.class);

    private final RatingPublisher ratingPublisher;

    public SubscriptionResolver(RatingPublisher ratingPublisher) {
        this.ratingPublisher = ratingPublisher;
    }

    public Publisher<Rating> onNewRating() {
        return environment -> this.ratingPublisher.getPublisher();
    }

    public Publisher<Rating> newRatings(String beerId) {
        logger.info("Subscription for 'newRatings' (" + beerId + ") received");
        return this.ratingPublisher.getPublisher(beerId);
    }

}
