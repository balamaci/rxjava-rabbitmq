package com.balamaci.rx.configuration;

import com.balamaci.rx.Receiver;
import com.balamaci.rx.util.Json;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import rx.Observable;
import rx.Observer;
import rx.subjects.PublishSubject;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

import static com.balamaci.rx.util.SleepUtil.sleepMillis;

/**
 * @author sbalamaci
 */
@Configuration
@Profile("hardcoded-events")
public class JsonFileEmitterSourceConfiguration {

    private static final Logger log = LoggerFactory.getLogger(JsonFileEmitterSourceConfiguration.class);

    @Bean
    public Receiver receiver() {
        return new Receiver();
    }


    @Bean(name = "events")
    public Observable<JsonObject> emitEvents(Receiver receiver) {
        startEmitting(receiver);

        return receiver.getPublishSubject();
    }

    private void startEmitting(Receiver receiver) {
        PublishSubject<JsonObject> publishSubject = receiver.getPublishSubject();

        Supplier<Integer> waitForMillis = () -> 200;
//        Supplier<Integer> waitForMillis = randomMillisWait(0,500);

        new SimpleAsyncTaskExecutor("json-hardcoded-file")
                .submit(() -> produceEventsFromJsonFile(publishSubject, waitForMillis));
    }


/*
    @Bean(name = "events")
    public Observable<JsonObject> emitEventsColdSubscriber() {
        return Observable.<JsonObject>create(this::produceEventsFromJson);
    }
*/

    private void produceEventsFromJsonFile(Observer<JsonObject> subscriber, Supplier<Integer> waitTimeMillis) {
        JsonArray events = Json.readJsonArrayFromFile("events.json");
        events.forEach(ev -> {
            sleepMillis(waitTimeMillis.get());

            JsonObject jsonObject = (JsonObject) ev;
            log.info("Emitting {}", Json.propertyStringValue("message").call(jsonObject));

            subscriber.onNext(jsonObject);
        });
    }

    private Supplier<Integer> randomMillisWait(int minMillis, int maxMillis) {
        return () -> ThreadLocalRandom.current().nextInt(minMillis, maxMillis + 1);
    }

}
