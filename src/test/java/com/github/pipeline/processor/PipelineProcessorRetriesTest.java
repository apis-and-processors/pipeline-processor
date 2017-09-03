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

import com.github.pipeline.processor.exceptions.NullNotAllowedException;
import com.github.pipeline.processor.exceptions.ProcessTimeTypeMismatchException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import net.jodah.failsafe.RetryPolicy;
import org.testng.annotations.Test;

/**
 * Tests for exercising PipelineProcessor with retries.
 * 
 * @author cdancy
 */
public class PipelineProcessorRetriesTest {

    private static final AtomicInteger INCREMENTER = new AtomicInteger(0);

    class IncrementOnFailure implements Function<Boolean, Object> {
        
        @Override
        public Object apply(final Boolean object) {
            final int value = INCREMENTER.incrementAndGet();
            if (value < 5) {
                throw new RuntimeException("1");
            }
            return new Object();
        }
    }
    
    class NullNotAllowed implements Function<Boolean, Object> {
        
        @Override
        public Object apply(final Boolean object) {
            INCREMENTER.incrementAndGet();
            throw new NullNotAllowedException("2");
        }
    }
    
    class ProcessTimeTypeMismatch implements Function<Boolean, Object> {
        
        @Override
        public Object apply(final Boolean object) {
            INCREMENTER.incrementAndGet();
            throw new ProcessTimeTypeMismatchException("3");
        }
    }
        
    @Test
    public void testNumberOfRetriesIsReached() {
        final RetryPolicy retry = new RetryPolicy().withMaxRetries(5);
        PipelineProcessor.builder().handler(IncrementOnFailure.class).retryPolicy(retry).build().output(true);
        assertThat(INCREMENTER.get()).isEqualTo(5);
    }
    
    @Test (dependsOnMethods = "testNumberOfRetriesIsReached")
    public void testNoRetriesOnNullNotAllowedException() {
        INCREMENTER.set(0);
        final RetryPolicy retry = new RetryPolicy().withMaxRetries(5);
        try {
            PipelineProcessor.builder().handler(NullNotAllowed.class).retryPolicy(retry).build().output(true);
        } catch (NullNotAllowedException nnae) {
            // ignore
        }
        assertThat(INCREMENTER.get()).isEqualTo(1);
    }
    
    @Test (dependsOnMethods = "testNoRetriesOnNullNotAllowedException")
    public void testNoRetriesOnProcessTimeTypeMismatchException() {
        INCREMENTER.set(0);
        final RetryPolicy retry = new RetryPolicy().withMaxRetries(5);
        try {
            PipelineProcessor.builder().handler(ProcessTimeTypeMismatch.class).retryPolicy(retry).build().output(true);
        } catch (ProcessTimeTypeMismatchException nnae) {
            // ignore
        }
        assertThat(INCREMENTER.get()).isEqualTo(1);
    }
}
