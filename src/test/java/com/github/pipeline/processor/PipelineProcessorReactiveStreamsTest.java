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
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import net.jodah.failsafe.RetryPolicy;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.testng.annotations.Test;

/**
 * Tests for exercising PipelineProcessor with Reactive-Streams.
 * 
 * @author cdancy
 */
public class PipelineProcessorReactiveStreamsTest {

    private final String helloWorld = "Hello, World!";
    private final RetryPolicy retries = new RetryPolicy().withMaxRetries(2);
    
    private class Tester implements Subscriber {

        public Subscription subscription;
        public final List<Object> onNextObjects = Lists.newArrayList();
        public final List<Throwable> onErrorThrowables = Lists.newArrayList();
        public int onCompleteCalled;
        
        @Override
        public void onSubscribe(final Subscription subscription) {
            this.subscription = subscription;
        }

        @Override
        public void onNext(final Object instance) {
            this.onNextObjects.add(instance);
        }

        @Override
        public void onError(final Throwable thrwbl) {
            this.onErrorThrowables.add(thrwbl);
        }

        @Override
        public void onComplete() {
            this.onCompleteCalled++;
        }
    }
       
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
            return helloWorld;
        }
    }
    
    class ListReturner implements Function<Boolean, List<String>> {
        
        @Override
        public List<String> apply(final Boolean object) {
            final List<String> temp = Lists.newArrayList("asdf");
            return temp;
        }
    }
    
    class ListAcceptor implements Function<List<String>, List<String>> {
        
        @Override
        public List<String> apply(final List<String> object) {
            if (object == null) {
                throw new RuntimeException("List is NULL");
            } else {
                return object;
            }
        }
    }
    
    @Test
    public void testOnNext() {
        final MySubscriber subscriber = new MySubscriber();
        final PipelineProcessor processor = PipelineProcessor.builder()
                .handler(Handler1.class)
                .handler(Handler2.class)
                .handler(Handler3.class)
                .retryPolicy(retries)
                .subscriber(MySubscriber.class)
                .subscriber(subscriber).build();
        final String output = (String) processor.output(true);
        assertThat(output).isEqualTo(helloWorld);
        assertThat(subscriber.onNextObjects.size()).isEqualTo(3);
        
        assertThat(subscriber.onNextObjects.get(0)).isInstanceOf(String.class);
        assertThat(subscriber.onNextObjects.get(0)).isEqualTo("hello");
        assertThat(subscriber.onNextObjects.get(1)).isInstanceOf(Integer.class);
        assertThat(subscriber.onNextObjects.get(1)).isEqualTo(123);
        assertThat(subscriber.onNextObjects.get(2)).isInstanceOf(String.class);
        assertThat(subscriber.onNextObjects.get(2)).isEqualTo(helloWorld);

        assertThat(subscriber.onCompleteCalled).isEqualTo(1);
        assertThat(subscriber.onErrorThrowables.size()).isEqualTo(0);
    }
    
    @Test
    public void testOnNextAlterValues() {
        final Subscriber subscriber = new Subscriber() {
            
            @Override
            public void onSubscribe(final Subscription instance) {
                // ignore
            }

            @Override
            public void onNext(final Object instance) {
                if (instance instanceof List) {
                    List<String> bears = (List)instance;
                    bears.add(UUID.randomUUID().toString());
                }
            }

            @Override
            public void onError(final Throwable instance) {
                // ignore
            }

            @Override
            public void onComplete() {
                // ignore
            }
        };
        
        final PipelineProcessor processor = PipelineProcessor.builder()
                .handler(ListReturner.class)
                .handler(ListAcceptor.class)
                .retryPolicy(retries)
                .subscriber(subscriber).build();
        final List<String> output = (List) processor.output(true);
        assertThat(output).isNotNull();
        assertThat(output.size()).isEqualTo(3);
    }
    
    @Test
    public void testOnComplete() {
        final MySubscriber subscriber = new MySubscriber();
        final PipelineProcessor processor = PipelineProcessor.builder()
                .handler(Handler1.class)
                .handler(Handler2.class)
                .handler(Handler3.class)
                .retryPolicy(retries)
                .subscriber(subscriber).build();
        final String output = (String) processor.output(true);
        assertThat(output).isEqualTo(helloWorld);
        assertThat(subscriber.onNextObjects.size()).isEqualTo(3);
        assertThat(subscriber.onCompleteCalled).isEqualTo(1);
        assertThat(subscriber.onErrorThrowables.size()).isEqualTo(0);
    }
    
    @Test
    public void testOnError() {
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
        assertThat(subscriber.onNextObjects.size()).isEqualTo(1);
        assertThat(subscriber.onCompleteCalled).isEqualTo(0);
        assertThat(subscriber.onErrorThrowables.size()).isEqualTo(1);
        assertThat(subscriber.onErrorThrowables.get(0).getMessage()).isEqualTo(thrownException.getMessage());
    }
    
    @Test
    public void testOnCancel() {
        
        final AtomicReference<Subscription> subscription = new AtomicReference<>(null);
        final AtomicInteger onNextCalled = new AtomicInteger(0);
        final AtomicReference<Throwable> onErrorCalled = new AtomicReference<>(null);
        final AtomicInteger onCompleteCalled = new AtomicInteger(0);
            
        final Subscriber tester = new Subscriber() {

            @Override
            public void onSubscribe(final Subscription sub) {
                subscription.set(sub);
            }

            @Override
            public void onNext(final Object instance) {
                if (instance instanceof Subscription) {
                    Subscription sub = (Subscription)instance;
                    sub.cancel();
                }
                onNextCalled.incrementAndGet();
            }

            @Override
            public void onError(final Throwable thrwbl) {
                onErrorCalled.set(thrwbl);
            }

            @Override
            public void onComplete() {
                onCompleteCalled.incrementAndGet();
            }
        };
        
        final Function innerFuncOne = new Function<Void, Subscription>() {
            
            @Override
            public Subscription apply(final Void instance) {
                return subscription.get();
            }
        };
        
        final Function innerFuncTwo = new Function<Subscription, Boolean>() {
            
            @Override
            public Boolean apply(final Subscription instance) {
                return true;
            }
        };
                
        final Function innerFuncThree = new Function<Boolean, String>() {
            
            @Override
            public String apply(final Boolean instance) {
                return helloWorld;
            }
        };
        
        final PipelineProcessor processor = PipelineProcessor.builder()
                .handler(innerFuncOne)
                .handler(innerFuncTwo)
                .handler(innerFuncThree)
                .retryPolicy(retries)
                .subscriber(tester).build();
        
        final String output = (String) processor.output();
        assertThat(output).isEqualTo(helloWorld);
        assertThat(onNextCalled.get()).isEqualTo(1);        
        assertThat(onCompleteCalled.get()).isEqualTo(0);
        assertThat(onErrorCalled.get()).isNull();
    }
}
