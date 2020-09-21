package nh.graphql.beeradvisor.graphql.resolver;

import graphql.kickstart.tools.GraphQLMutationResolver;
import nh.graphql.beeradvisor.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MutationResolver implements GraphQLMutationResolver {
    private final Logger logger = LoggerFactory.getLogger(MutationResolver.class);

    private final BeerService beerService;
    private final RatingService ratingService;

    public MutationResolver(BeerService beerService, RatingService ratingService) {
        this.beerService = beerService;
        this.ratingService = ratingService;
    }

    public Rating addRating(AddRatingInput addRatingInput) {
        logger.debug("Rating Input {}", addRatingInput);
        return ratingService.addRating(addRatingInput);
    }


    public Beer updateBeerName(String beerId, String newName) {
        return beerService.updateBeer(beerId, newName);
    }


}
