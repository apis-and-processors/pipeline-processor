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

package com.github.pipeline.processor.utils;

import static org.testng.Assert.assertTrue;

import com.github.aap.processor.tools.domain.ClassType;
import com.github.aap.processor.tools.domain.Null;
import com.github.pipeline.processor.PipelineHandler;
import com.github.pipeline.processor.exceptions.CheckTimeTypeMismatchException;
import com.github.pipeline.processor.exceptions.NullNotAllowedException;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.testng.annotations.Test;

/**
 * Tests for using PipelineUtils.
 * 
 * @author cdancy
 */
public class PipelineUtilsTest {
        
    class Handler1 implements Function<String, Object> {
        
        @Override
        public Object apply(final String object) {
            return null;
        }
    }
    
    class Handler2 implements Function<Null, Character> {
        
        @Override
        public Character apply(final Null object) {
            return null;
        }
    }
        
    class Handler3 implements Function<Boolean, String> {
        
        @Override
        public String apply(final Boolean object) {
            return null;
        }
    }
    
    class Handler4 implements Function<String, String> {
        
        @Override
        public String apply(final String object) {
            return null;
        }
    }
    
    @Test (expectedExceptions = NullPointerException.class)
    public void testNPEOnNullPipeline() {
        PipelineUtils.checkTimeScan(null);
    }
    
    @Test
    public void testEmptyMapOnEmptyPipeline() {
        assertTrue(PipelineUtils.checkTimeScan(Lists.newArrayList()).isEmpty());
    }

    @Test
    public void testNoRequiredChecksOnSanePipeline() {
        final List<PipelineHandler> pipeLine = Lists.newArrayList();
        pipeLine.add(PipelineHandler.newInstance(new Handler3()));
        pipeLine.add(PipelineHandler.newInstance(new Handler4()));
        final Map<Integer, ClassType> typeChecks = PipelineUtils.checkTimeScan(pipeLine);
        assertTrue(typeChecks.isEmpty());
    }
    
    @Test
    public void testRequiredCheckOnSanePipeline() {
        final List<PipelineHandler> pipeLine = Lists.newArrayList();
        pipeLine.add(PipelineHandler.newInstance(new Handler1()));
        pipeLine.add(PipelineHandler.newInstance(new Handler2()));
        final Map<Integer, ClassType> typeChecks = PipelineUtils.checkTimeScan(pipeLine);
        assertTrue(typeChecks.size() == 1);
    }
    
    @Test (expectedExceptions = CheckTimeTypeMismatchException.class)
    public void testCheckTimeTypeMismatchExceptionOnPipeline() {
        final List<PipelineHandler> pipeLine = Lists.newArrayList();
        pipeLine.add(PipelineHandler.newInstance(new Handler2()));
        pipeLine.add(PipelineHandler.newInstance(new Handler1()));
        PipelineUtils.checkTimeScan(pipeLine);
    }
    
    @Test (expectedExceptions = NullNotAllowedException.class)
    public void testNullPointerExceptionOnPipeline() {
        final List<PipelineHandler> pipeLine = Lists.newArrayList();
        pipeLine.add(PipelineHandler.newInstance(new Handler2()));
        pipeLine.add(null);
        PipelineUtils.checkTimeScan(pipeLine);
    }
}
