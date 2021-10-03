package nh.graphql.beeradvisor.graphql;

//import io.reactivex.BackpressureStrategy;
//import io.reactivex.Flowable;
//import io.reactivex.Observable;
//import io.reactivex.ObservableEmitter;
//import io.reactivex.observables.ConnectableObservable;

import nh.graphql.beeradvisor.domain.Rating;
import nh.graphql.beeradvisor.domain.RatingCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.Observable;

/**
 * RatingPublisher
 */
@Component
public class RatingPublisher {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final Flux<Rating> publisher;
  private FluxSink<Rating> emitter;

  public RatingPublisher() {
    ConnectableFlux<Rating> publish = Flux.<Rating>create(fluxSink -> this.emitter = fluxSink)
      .publish();
    this.publisher = publish.autoConnect();
  }

  @TransactionalEventListener
  public void onNewVisit(RatingCreatedEvent event) {
    logger.info("onNewRating {}", event);
    if (this.emitter != null) {
      this.emitter.next(event.getRating());
    }
  }

  public Flux<Rating> getPublisher() {
    return this.publisher;
  }

  public Flux<Rating> getPublisher(String beerId) {
    return this.publisher.filter(rating -> beerId.equals(rating.getBeer().getId()));
  }

//
//  private ObservableEmitter<Rating> emitter;
//  private final Flowable<Rating> publisher;
//
//  public RatingPublisher() {
//    Observable<Rating> ratingObservable = Observable.create(emitter -> {
//      this.emitter = emitter;
//    });
//    ConnectableObservable<Rating> connectableObservable = ratingObservable.share().publish();
//    connectableObservable.connect();
//
//    this.publisher = connectableObservable.toFlowable(BackpressureStrategy.BUFFER);
//  }
//
//  @TransactionalEventListener
//  public void ratingCreated(RatingCreatedEvent ratingCreatedEvent) {
//    logger.info("Received RatingCreatedEvent " + ratingCreatedEvent);
//    if (this.emitter != null) {
//      this.emitter.onNext(ratingCreatedEvent.getRating());
//    }
//  }
//
//  public Flowable<Rating> getPublisher() {
//    return this.publisher;
//  }
//
//  public Flowable<Rating> getPublisher(String beerId) {
//    return this.publisher.filter(rating -> beerId.equals(rating.getBeer().getId()));
//  }

}