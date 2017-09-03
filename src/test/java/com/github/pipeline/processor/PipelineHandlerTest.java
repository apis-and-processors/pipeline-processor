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

import static com.google.common.base.Preconditions.checkNotNull;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import com.github.aap.processor.tools.domain.ClassType;
import com.github.aap.processor.tools.domain.Null;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.testng.annotations.Test;

/**
 * Tests to exercise general PipelineHandler execution.
 * 
 * @author cdancy
 */
public class PipelineHandlerTest {
       
    class Handler1 implements Function<String, Object> {
        
        @Override
        public Object apply(final String object) {
            return null;
        }
    }
    
    class Handler2 implements Function<Null, Character> {
        
        @Override
        @Nullable
        public Character apply(final Null object) {
            return null;
        }
    }
        
    class Handler3 implements Function<Boolean, String> {
        
        @Override
        public String apply(@Nullable final Boolean object) {
            return null;
        }
    }
    
    class Handler4 implements Function<String, String> {
        
        @Override
        @Nullable
        public String apply(@Nullable final String object) {
            return null;
        }
    }
        
    @Test
    public void testSuccessfulPipelineHandlerCreation() {
        final PipelineHandler instance = PipelineHandler.newInstance(new Handler1());
        assertNotNull(instance);
        assertTrue(instance.id().equals(Handler1.class.getName()));
        final ClassType classType = instance.classType().firstSubTypeMatching(".*Function.*");
        assertTrue(classType.subTypes().size() == 2);
        assertTrue(classType.subTypeAtIndex(0).name().equals(String.class.getName()));
        assertTrue(classType.subTypeAtIndex(1).name().equals(Object.class.getName()));
    }
    
    @Test (expectedExceptions = NullPointerException.class)
    public void testFailedPipelineHandlerCreation() {
        PipelineHandler.newInstance(null);
    }
    
    @Test
    public void testNoNullablesOnInputOrOutput() {
        final PipelineHandler instance = PipelineHandler.newInstance(new Handler1());
        assertNotNull(instance);
        assertFalse(instance.inputNullable());
        assertFalse(instance.outputNullable());
    }
    
    @Test
    public void testNullableOnOutput() {
        final PipelineHandler instance = PipelineHandler.newInstance(new Handler2());
        checkNotNull(instance);
        assertFalse(instance.inputNullable());
        assertTrue(instance.outputNullable());
    }
    
    @Test
    public void testNullableOnInput() {
        final PipelineHandler instance = PipelineHandler.newInstance(new Handler3());
        checkNotNull(instance);
        assertTrue(instance.inputNullable());
        assertFalse(instance.outputNullable());
    }
    
    @Test
    public void testNullablesOnInputAndOutput() {
        final PipelineHandler instance = PipelineHandler.newInstance(new Handler4());
        checkNotNull(instance);
        assertTrue(instance.inputNullable());
        assertTrue(instance.outputNullable());
    }
}
