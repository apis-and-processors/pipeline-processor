/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.pipeline.processor;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.function.Function;
import net.jodah.failsafe.RetryPolicy;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.testng.annotations.Test;

/**
 * Tests for invoking PipelineProcessor with Reactive-Streams.
 * 
 * @author cdancy
 */
public class PipelineProcessorReactiveStreamsTest {

    private final RetryPolicy retries = new RetryPolicy().withMaxRetries(2);
    
    private class MySubscriber implements Subscriber {

        public Subscription subscription;
        public final List<Object> onNextObjects = Lists.newArrayList();
        public final List<Throwable> onErrorThrowables = Lists.newArrayList();
        public int onCompleteCalled;
        
        @Override
        public void onSubscribe(final Subscription instance) {
            this.subscription = instance;
        }

        @Override
        public void onNext(final Object instance) {
            this.onNextObjects.add(instance);
        }

        @Override
        public void onError(final Throwable throwable) {
            this.onErrorThrowables.add(throwable);
        }

        @Override
        public void onComplete() {
            this.onCompleteCalled++;
        }
    }
    
    class Handler1 implements Function<Boolean, String> {
        
        @Override
        public String apply(final Boolean object) {
            return object ? "hello" : "world";
        }
    }
    
    class Handler2 implements Function<String, Integer> {
        
        @Override
        public Integer apply(final String object) {
            if (object.equals("hello")) {
                return 123;
            } else {
                throw new RuntimeException("We need world");
            }
        }
    }
    
    class Handler3 implements Function<Integer, String> {
        
        @Override
        public String apply(final Integer object) {
            return "Hello, World!";
        }
    }
        
    @Test
    public void testOnCompleteIsCalled() {
        final MySubscriber subscriber = new MySubscriber();
        final PipelineProcessor processor = PipelineProcessor.builder()
                .handler(Handler1.class)
                .handler(Handler2.class)
                .handler(Handler3.class)
                .retryPolicy(retries)
                .subscriber(subscriber).build();
        final String output = (String) processor.output(true);
        assertThat(output).isEqualTo("Hello, World!");
        assertThat(subscriber.onCompleteCalled).isEqualTo(1);
        assertThat(subscriber.onErrorThrowables.size()).isEqualTo(0);
    }
    
    @Test
    public void testOnErrorIsCalled() {
        final MySubscriber subscriber = new MySubscriber();
        final PipelineProcessor processor = PipelineProcessor.builder()
                .handler(Handler1.class)
                .handler(Handler2.class)
                .handler(Handler3.class)
                .retryPolicy(retries)
                .subscriber(subscriber).build();
        
        Throwable thrownException = null;
        try {
            processor.output(false);
        } catch (Exception e) {
            thrownException = e;
        }
        
        assertThat(thrownException).isNotNull();
        assertThat(subscriber.onCompleteCalled).isEqualTo(0);
        assertThat(subscriber.onErrorThrowables.size()).isEqualTo(1);
        assertThat(subscriber.onErrorThrowables.get(0).getMessage()).isEqualTo(thrownException.getMessage());
    }
}
