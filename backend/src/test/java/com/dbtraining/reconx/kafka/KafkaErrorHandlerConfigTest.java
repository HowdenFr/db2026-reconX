package com.dbtraining.reconx.kafka;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.backoff.BackOffExecution;
import org.springframework.util.backoff.ExponentialBackOff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KafkaErrorHandlerConfigTest {

    @Mock
    private KafkaTemplate<String, Object> template;

    @Mock
    private ConcurrentKafkaListenerContainerFactoryConfigurer configurer;

    @Mock
    private ConsumerFactory<Object, Object> consumerFactory;

    @Test
    void kafkaErrorHandlerUsesOneTwoFourSecondExponentialBackoffBeforeStopping() {
        DefaultErrorHandler errorHandler = new KafkaErrorHandlerConfig().kafkaErrorHandler(template);

        Object failureTracker = ReflectionTestUtils.getField(errorHandler, "failureTracker");
        ExponentialBackOff backOff = (ExponentialBackOff) ReflectionTestUtils.getField(failureTracker, "backOff");

        assertThat(backOff).isNotNull();
        assertThat(backOff.getInitialInterval()).isEqualTo(1_000L);
        assertThat(backOff.getMultiplier()).isEqualTo(2.0);
        assertThat(backOff.getMaxElapsedTime()).isEqualTo(8_000L);
        assertThat(backOff.getMaxAttempts()).isEqualTo(3);

        BackOffExecution execution = backOff.start();
        assertThat(execution.nextBackOff()).isEqualTo(1_000L);
        assertThat(execution.nextBackOff()).isEqualTo(2_000L);
        assertThat(execution.nextBackOff()).isEqualTo(4_000L);
        assertThat(execution.nextBackOff()).isEqualTo(BackOffExecution.STOP);
    }

    @Test
    void kafkaErrorHandlerMarksPoisonPillExceptionsAsNotRetryable() {
        DefaultErrorHandler errorHandler = new KafkaErrorHandlerConfig().kafkaErrorHandler(template);
        Object classifier = ReflectionTestUtils.invokeMethod(errorHandler, "getClassifier");

        DeserializationException deserializationException =
                new DeserializationException("bad payload", "this-is-not-json".getBytes(), false, new RuntimeException("boom"));

        Boolean deserializationRetryable =
                ReflectionTestUtils.invokeMethod(classifier, "classify", deserializationException);
        Boolean illegalArgumentRetryable =
                ReflectionTestUtils.invokeMethod(classifier, "classify", new IllegalArgumentException("invalid event"));
        Boolean runtimeRetryable =
                ReflectionTestUtils.invokeMethod(classifier, "classify", new RuntimeException("transient failure"));

        assertThat(deserializationRetryable).isFalse();
        assertThat(illegalArgumentRetryable).isFalse();
        assertThat(runtimeRetryable).isTrue();
    }

    @Test
    void tradeEventsContainerFactoryIsWiredToTheConfiguredErrorHandler() {
        KafkaErrorHandlerConfig config = new KafkaErrorHandlerConfig();
        DefaultErrorHandler errorHandler = config.kafkaErrorHandler(template);

        ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
                config.tradeEventsKafkaListenerContainerFactory(configurer, consumerFactory, errorHandler);

        verify(configurer).configure(factory, consumerFactory);
        assertThat(ReflectionTestUtils.getField(factory, "commonErrorHandler")).isSameAs(errorHandler);
    }
}
