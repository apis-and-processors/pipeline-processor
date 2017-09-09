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

import com.github.aap.processor.tools.domain.Null;
import com.github.pipeline.processor.annotations.Cache;
import com.github.pipeline.processor.exceptions.CheckTimeCacheException;
import com.github.pipeline.processor.exceptions.CheckTimeTypeMismatchException;
import java.util.Map;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.assertj.core.util.Maps;
import org.testng.annotations.Test;

/**
 * Tests to exercise caching within PipelineProcessor.
 * 
 * @author cdancy
 */
public class PipelineProcessorCachingTest {

    private final String cacheReferenceKey = "CacheReference";
    private final String cacheSetKey = "CacheSet";

    class HandlerWithCacheSetString implements Function<String, String> {
        
        @Override
        @Cache(cacheSetKey)
        public String apply(final String object) {
            return object;
        }
    }
    
    class HandlerWithCacheSetObject implements Function<Object, Object> {
        
        @Override
        @Cache(cacheSetKey)
        public Object apply(final Object object) {
            return object;
        }
    }
 
    class HandlerForStrings implements Function<String, String> {
        
        @Override
        public String apply(final String object) {
            return object;
        }
    }
        
    class HandlerForNullVoid implements Function<Null, Void> {
        
        @Override
        public Void apply(final Null object) {
            return null;
        }
    }

    class HandlerWithCacheSetStringReference implements Function<String, AtomicReference<String>> {
        
        @Override
        @Cache(cacheReferenceKey)
        public AtomicReference<String> apply(final String object) {
            final AtomicReference<String> reference = new AtomicReference<String>(object);
            return reference;
        }
    }
        
    class HandlerWithCacheGetStringReference implements Function<AtomicReference<String>, Void> {
        
        @Override
        public Void apply(@Cache(cacheReferenceKey) final AtomicReference<String> object) {
            object.set(object.get() + "-1");
            return null;
        }
    }
    
    class HandlerWithCacheGetStringReferenceAndReturnValue implements Function<AtomicReference<String>, String> {
        
        @Override
        public String apply(@Cache(cacheReferenceKey) final AtomicReference<String> object) {
            object.set(object.get() + "-2");
            return object.get();
        }
    }
    
    class HandlerWithCacheGetIntegerReferenceAndReturnValue implements Function<AtomicReference<Integer>, String> {
        
        @Override
        public String apply(@Cache(cacheReferenceKey) final AtomicReference<Integer> object) {
            return String.valueOf(object.get());
        }
    }
        
    class HandlerWithCacheGetString implements Function<String, Object> {
        
        @Override
        public Object apply(@Cache(cacheSetKey) final String object) {
            return object;
        }
    }
    
    class HandlerWithCacheSetNull implements Function<String, Character> {
        
        @Override
        @Cache("HandlerWithNullCacheSet")
        @Nullable
        public Character apply(final String object) {
            return null;
        }
    }
    
    class HandlerWithCacheGetNull implements Function<Character, String> {
        
        @Override
        @Nullable
        public String apply(@Cache("HandlerWithNullCacheSet") @Nullable final Character object) {
            return null;
        }
    }
    
    private static final String inputValue = UUID.randomUUID().toString();
        
    @Test
    public void testCanSetAndGetFromCache() {
        final Object output = PipelineProcessor.builder()
                .handler(HandlerWithCacheSetString.class)
                .handler(HandlerForNullVoid.class)
                .handler(HandlerWithCacheGetString.class)
                .build()
                .output(inputValue);
        assertThat(output).isNotNull();
        assertThat((String)output).isEqualTo(inputValue);
    }
    
    @Test
    public void testCanSetNullCache() {
        final Object output = PipelineProcessor.builder()
                .handler(HandlerWithCacheSetNull.class)
                .handler(HandlerWithCacheGetNull.class)
                .build()
                .output(inputValue);
        assertThat(output).isNull();
    }
    
    @Test (expectedExceptions = CheckTimeCacheException.class)
    public void testCannotSetSameCacheKeyMultipleTimes() {
        PipelineProcessor.builder()
                .handler(HandlerWithCacheSetString.class)
                .handler(HandlerWithCacheSetObject.class)
                .build();
    }
    
    @Test (expectedExceptions = CheckTimeCacheException.class)
    public void testCannotSetSameCacheKeyMultipleTimesPastFirstHandler() {
        PipelineProcessor.builder()
                .handler(HandlerForStrings.class)
                .handler(HandlerWithCacheSetString.class)
                .handler(HandlerWithCacheSetObject.class)
                .build();
    }
    
    @Test (expectedExceptions = CheckTimeTypeMismatchException.class)
    public void testCanSetCacheAndFailOnTypeMismatch() {
        PipelineProcessor.builder()
                .handler(HandlerWithCacheSetStringReference.class)
                .handler(HandlerWithCacheGetStringReference.class)
                .handler(HandlerWithCacheGetIntegerReferenceAndReturnValue.class)
                .build()
                .output(inputValue);
    }
    
    @Test (expectedExceptions = CheckTimeCacheException.class)
    public void testFailOnNotSettingCacheOnNonNullOrVoidOutput() {
        PipelineProcessor.builder()
                .handler(HandlerForStrings.class)
                .handler(HandlerWithCacheGetString.class)
                .build()
                .output(inputValue);
    }
    
    @Test (expectedExceptions = CheckTimeCacheException.class)
    public void testEnsureAllCacheSetsAreUsed() {
        PipelineProcessor.builder()
                .handler(HandlerWithCacheSetString.class)
                .handler(HandlerForStrings.class)
                .build()
                .output(inputValue);
    }
    
    @Test
    public void testCanSetCacheAndGetMultipleTimes() {
        final Object output = PipelineProcessor.builder()
                .handler(HandlerWithCacheSetStringReference.class)
                .handler(HandlerWithCacheGetStringReference.class)
                .handler(HandlerWithCacheGetStringReferenceAndReturnValue.class)
                .build()
                .output(inputValue);
        assertThat(output).isNotNull();
        assertThat((String)output).isEqualTo(inputValue + "-1-2");
    }
    
    @Test (expectedExceptions = CheckTimeCacheException.class)
    public void testFailIfOutputsAlreadyAvailableWithinGlobalResources() {
   
        final Map<String, Object> globalResources = Maps.newHashMap();
        globalResources.put(cacheSetKey, inputValue);
        
        final Object output = PipelineProcessor.builder()
                .handler(HandlerWithCacheSetString.class)
                .resources(globalResources)
                .build()
                .output();
        assertThat(output).isNotNull();
        assertThat((String)output).isEqualTo(inputValue);
    }
    
    @Test (expectedExceptions = CheckTimeTypeMismatchException.class)
    public void testFailIfWrongInputsAvailableWithinGlobalResources() {
   
        final Map<String, Object> globalResources = Maps.newHashMap();
        globalResources.put(cacheSetKey, 123);
        
        PipelineProcessor.builder()
                .handler(HandlerWithCacheGetString.class)
                .resources(globalResources)
                .build()
                .output();
    }
    
    @Test (expectedExceptions = CheckTimeCacheException.class)
    public void testFailIfNoInputsAvailableWithinGlobalResources() {
   
        final Map<String, Object> globalResources = Maps.newHashMap();
        globalResources.put("Hello World", 123);
        
        PipelineProcessor.builder()
                .handler(HandlerWithCacheGetString.class)
                .resources(globalResources)
                .build()
                .output();
    }
}
