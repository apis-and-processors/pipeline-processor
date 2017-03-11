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

import com.github.aap.processor.tools.TypeUtils;
import com.github.aap.processor.tools.domain.Null;
import com.google.common.reflect.TypeToken;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.testng.annotations.Test;

/**
 * Tests for invoking AbstractPipelineProcessor.
 * 
 * @author cdancy
 */
public class PipelineProcessorTest {
   
    private static final AtomicInteger bears = new AtomicInteger(0);
    
    class Handler1 implements Function<String, Object> {
        
        @Override
        @Nullable
        public Object apply(final String object) {
            System.out.println("Input1: " + object);
            return null;
        }
    }
    
    class Handler2 implements Function<Null, Optional<Boolean>> {
        @Override
        public Optional<Boolean> apply(final Null object) {

            if (bears.get() == 0) {
                bears.incrementAndGet();
                System.out.println("inside failure pop");
                throw new RuntimeException("FAILURE FROM BEARS");
            } else {
                System.out.println("we can keep going");
            }
            System.out.println("Input2: " + object);
            return Optional.empty();
        }
    }
        
    class Handler3 implements Comparable, Function<Optional<Boolean>, String> {
        @Override
        @Nullable
        public String apply(final Optional<Boolean> object) {
            System.out.println("Input3: " + object);
            return "123";
        }

        @Override
        public int compareTo(final Object object) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }
        
    @Test
    public void testSomeLibraryMethod() {
            
        final PipelineProcessor.Builder builder2 = PipelineProcessor.builder();
        
        
        System.out.println("Found: " + TypeToken.of(builder2.getClass()));
        //final Optional<String> fish = Optional.<String>empty();
        //System.out.println(TypeToken.of(fish.getClass().getGenericSuperclass()));
        final PipelineProcessor processor = builder2.handler(Handler1.class).handler(Handler2.class).handler(Handler3.class).build();
        
        
        final Object obj = processor.output("bears: " + TypeUtils.parseClassType(processor));

        System.out.println("output: " + obj);
    }
}
