package nh.graphql.beeradvisor.graphql.resolver;

import graphql.kickstart.tools.GraphQLResolver;
import graphql.schema.DataFetcher;
import nh.graphql.beeradvisor.domain.Beer;
import nh.graphql.beeradvisor.domain.BeerRepository;
import nh.graphql.beeradvisor.domain.Shop;
import nh.graphql.beeradvisor.graphql.fetchers.AddressField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ShopResolver implements GraphQLResolver<Shop> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private BeerRepository beerRepository;

    public List<Beer> beers(Shop shop) {
        final List<String> beerIds = shop.getBeers();
        return beerRepository.findWithIds(beerIds);
    }

    public AddressField address(Shop shop) {
        return new AddressField(shop);
    }

}
