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

import com.github.type.utils.ClassType;
import com.github.type.utils.ReflectionUtils;
import com.github.type.utils.TypeUtils;
import com.google.common.base.Function;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.collect.ImmutableList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author github.
 */
public class PipelineProcessor extends PipelineHandler<Object, Object> {
    
    private final ImmutableList<? extends PipelineHandler> pipeline;
            
    public PipelineProcessor(ImmutableList<? extends PipelineHandler> pipeline) {
        super(null, null);
        this.pipeline = pipeline;        
    }
    
    @Override
    public Object process(Object obj) {
        
        // 1.) 
        for(PipelineHandler foundFunction : pipeline) {
            
            System.out.println("Found function: " + foundFunction.getClass());
            ClassType clazzType = TypeUtils.parseClassType(foundFunction.function()).firstSubTypeMatching(".*Function.*");
            if (clazzType != null) {
                System.out.println("Found ClassType: " + clazzType + ", subTypes: " + clazzType.subTypes().size());
            }
        }
        return null;
    }
    
    public static <T, V> Builder<T, V> builder() {
        return new Builder();
    }
    
    public static class Builder<T, V> {
    
        private final Logger LOGGER = Logger.getLogger(PipelineProcessor.class.getName());
        private final ImmutableList.Builder<PipelineHandler> pipelineHandlers = ImmutableList.builder();
        private Object input;

        /**
         * Optional initial input to the pipeline
         * 
         * @param input Optional initial input to first handler of pipeline
         * @return this Builder.
         */
        public Builder input(Object input) {
            this.input = checkNotNull(input, "input cannot be null");
            return this;
        }
        
        /**
         * Add handler to this pipeline
         * 
         * @param pipelineHandler handler to append to the end of this pipeline.
         * @return this Builder.
         */
        public Builder handler(Class<? extends Function> pipelineHandler) {
            checkNotNull(pipelineHandler, "pipelineHandler cannot be null");
            final Function<?, ?> function = ReflectionUtils.newInstance(pipelineHandler);
            return handler(function);
        }
        
        /**
         * Add handler to this pipeline
         * 
         * @param pipelineHandler handler to append to the end of this pipeline.
         * @return this Builder.
         */
        public Builder handler(final Function<?, ?> pipelineHandler) {
            checkNotNull(pipelineHandler, "pipelineHandler cannot be null");
            PipelineHandler handler = PipelineHandler.newInstance(pipelineHandler, null);
            return handler(handler);
        }
        
        /**
         * Add handler to this pipeline
         * 
         * @param pipelineHandler handler to append to the end of this pipeline.
         * @return this Builder.
         */
        public Builder handler(final PipelineHandler pipelineHandler) {
            checkNotNull(pipelineHandler, "pipelineHandler cannot be null");
            pipelineHandlers.add(pipelineHandler);
            LOGGER.log(Level.CONFIG, "Handler '{0}' added to pipeline", pipelineHandler.getClass().getName());
            return this;
        }
        
        /**
         * Build a PipelineProcessor from passed build parameters.
         * 
         * @param <T>
         * @param <V>
         * @return newly created PipelineProcessor.
         */
        public <T, V> PipelineProcessor build() {
            return new PipelineProcessor(pipelineHandlers.build());
        }
        
        /**
         * Convenience method for getting the output of the PipelineProcessor
         * 
         * @return output of PipelineProcessor
         */
        public Object output() {
            return build().process(input);
        }
    }
}
