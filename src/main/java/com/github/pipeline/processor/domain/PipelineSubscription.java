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

package com.github.pipeline.processor.domain;

import static com.google.common.base.Preconditions.checkNotNull;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * Wrapper for Subscribers and their Subscriptions.
 * 
 * @author cdancy
 */
public class PipelineSubscription implements Subscription {
    
    private final Subscriber subscriber;
    private boolean cancelled;
    
    public PipelineSubscription(final Subscriber subscriber) {
        this.subscriber = checkNotNull(subscriber);
    }
    
    public Subscriber subscriber() {
        return subscriber;
    }
    
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void request(final long elements) {
        // no-op
    }

    @Override
    public void cancel() {
        this.cancelled = true;
    }
    
    /**
     * Helper method to instantiate statically.
     * 
     * @param subscriber Subscriber instance.
     * @return instance of PipelineSubscription.
     */
    public static PipelineSubscription newInstance(final Subscriber subscriber) {
        return new PipelineSubscription(subscriber);
    }
}
