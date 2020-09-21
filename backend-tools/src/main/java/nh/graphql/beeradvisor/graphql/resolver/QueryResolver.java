package nh.graphql.beeradvisor.graphql.resolver;

import graphql.kickstart.tools.GraphQLQueryResolver;
import graphql.schema.DataFetcher;
import nh.graphql.beeradvisor.domain.*;
import nh.graphql.beeradvisor.graphql.fetchers.BeerAdvisorDataFetcher;
import nh.graphql.beeradvisor.graphql.subscription.RatingPublisher;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class QueryResolver implements GraphQLQueryResolver {
    private final Logger logger = LoggerFactory.getLogger(QueryResolver.class);

    private final BeerRepository beerRepository;
    private final ShopRepository shopRepository;

    public QueryResolver(BeerRepository beerRepository, ShopRepository shopRepository) {
        this.beerRepository = beerRepository;
        this.shopRepository = shopRepository;
    }

    public String ping(String msg) {
        if (msg == null) {
            return "HELLO";
        }

        return "HELLO, " + msg;
    }

    public Beer beer(String beerId) {
        return beerRepository.getBeer(beerId);
    }

    public List<Beer> beers() {
        return beerRepository.findAll();
    }

    public Shop shop(String shopId) {
        return shopRepository.findShop(shopId);
    }

    public List<Shop> shops() {
        return shopRepository.findAll();
    }
}
