package com.balamaci.rx.observable.login;

import com.balamaci.rx.domain.UserCreditScoring;
import com.balamaci.rx.domain.UserLocationRating;
import com.balamaci.rx.util.Json;
import javafx.util.Pair;
import javaslang.Tuple3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import rx.Observable;
import rx.Subscription;
import rx.schedulers.Schedulers;


/**
 * @author sbalamaci
 */
@Configuration
public class SuccessfulLoginObservable extends LoginObservables {

    private static final Logger log = LoggerFactory.getLogger(SuccessfulLoginObservable.class);

    private Observable<Tuple3<String, UserCreditScoring, UserLocationRating>> checkUserScoringAndLocationRating(String username, String ip) {
        Observable<UserCreditScoring> scoringObservable = userService.retrieveScoring(username);
        Observable<UserLocationRating> locationObservable = userService.retrieveUserLocationBasedOnIp(username, ip);

        Observable<Tuple3<String, UserCreditScoring, UserLocationRating>> scoringAndLocationObservable =
                scoringObservable
                        .zipWith(locationObservable, (scoring, location) -> new Tuple3<>(username, scoring, location));

        return scoringAndLocationObservable;
    }

    private Observable<Tuple3<String, UserCreditScoring, UserLocationRating>> userScoringAndLocationForSuccessfulLogins() {
        return succesfullLogins()
                .map(jsonObject ->
                        new Pair<>(new Json(jsonObject).propertyStringValue("userName"),
                                new Json(jsonObject).propertyStringValue("remoteIP")))
                .doOnNext(userIpPair -> log.info("After map {}", userIpPair))
                .flatMap(pair ->
                                 checkUserScoringAndLocationRating(pair.getKey(), pair.getValue())
                                 .doOnNext(tuple3 -> log.info("Tupple {}", tuple3))
                        );
    }

    @Bean
    Subscription userScoringAndLocationSubscription() {
        return userScoringAndLocationForSuccessfulLogins()
                .subscribe(userScoringAndLocation -> log.info("** Subscriber got {}", userScoringAndLocation));
    }


}