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
import java.util.function.Function;
import javax.annotation.Nullable;
import org.testng.annotations.Test;

/**
 * Tests for invoking PipelineProcessor.
 * 
 * @author cdancy
 */
public class PipelineProcessorTest {

    class VoidInput implements Function<Void, Object> {
        
        @Override
        public Object apply(final Void object) {
            return new Object();
        }
    }
    
    class NullInput implements Function<Null, Object> {
        
        @Override
        public Object apply(final Null object) {
            return new Object();
        }
    }
    
    class VoidOutput implements Function<Object, Void> {
        
        @Override
        public Void apply(final Object object) {
            return null;
        }
    }
    
    class NullOutput implements Function<Object, Null> {
        
        @Override
        public Null apply(final Object object) {
            return null;
        }
    }
    
    class NullableInput implements Function<Boolean, Object> {
        
        @Override
        public Object apply(@Nullable final Boolean object) {
            return new Object();
        }
    }
    
    class NullableOutput implements Function<Boolean, Object> {
        
        @Override
        @Nullable
        public Object apply(@Nullable final Boolean object) {
            return object;
        }
    }
        
    @Test
    public void testNullInputOnVoid() {            
        PipelineProcessor.builder().handler(VoidInput.class).build().output(null);
    }
    
    @Test
    public void testNullInputOnNull() {            
        PipelineProcessor.builder().handler(NullInput.class).build().output(null);
    }
    
    @Test
    public void testNullOutput() {            
        final Object obj = PipelineProcessor.builder().handler(NullOutput.class).build().output(123);
        assertThat(obj).isNull();
    }
    
    @Test
    public void testVoidOutput() {            
        final Object obj = PipelineProcessor.builder().handler(VoidOutput.class).build().output(123);
        assertThat(obj).isNull();
    }    
    
    @Test
    public void testNullInputOnNullableInput() {            
        PipelineProcessor.builder().handler(NullableInput.class).build().output(null);
    }
    
    @Test
    public void testNullInputOnNullableOutput() {            
        final Object obj = PipelineProcessor.builder().handler(NullableOutput.class).build().output(null);
        assertThat(obj).isNull();
    }
}
