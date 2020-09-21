package nh.graphql.beeradvisor.graphql.resolver;

import graphql.kickstart.tools.GraphQLResolver;
import graphql.schema.DataFetcher;
import nh.graphql.beeradvisor.domain.Beer;
import nh.graphql.beeradvisor.domain.Rating;
import nh.graphql.beeradvisor.domain.Shop;
import nh.graphql.beeradvisor.domain.ShopRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class BeerResolver implements GraphQLResolver<Beer> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ShopRepository shopRepository;

    public BeerResolver(ShopRepository shopRepository) {
        this.shopRepository = shopRepository;
    }

    public List<Shop> shops(Beer beer) {
        final String beerId = beer.getId();
        return shopRepository.findShopsWithBeer(beerId);
    }

    public Integer averageStars(Beer beer) {
        return (int) Math.round(beer.getRatings().stream().mapToDouble(Rating::getStars).average().getAsDouble());
    }

    public List<Rating> ratingsWithStars(Beer beer, int stars) {
        return beer.getRatings().stream().filter(r -> r.getStars() == stars).collect(Collectors.toList());
    }
}
