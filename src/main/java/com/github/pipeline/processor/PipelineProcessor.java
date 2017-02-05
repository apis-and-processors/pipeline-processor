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

import com.github.pipeline.processor.exceptions.NullNotAllowedException;
import com.github.pipeline.processor.exceptions.ProcessTimeTypeMismatchException;
import static com.google.common.base.Preconditions.checkNotNull;

import com.github.pipeline.processor.utils.PipelineUtils;
import com.github.type.utils.ClassType;
import com.github.type.utils.ReflectionUtils;
import com.github.type.utils.TypeUtils;
import com.github.type.utils.exceptions.TypeMismatchException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;

/**
 *
 * @author github.
 */
public class PipelineProcessor {
    
    private final List<? extends PipelineHandler> pipeline;
    private volatile Object initialInput;
    private volatile Class<?> outputType;
            
    public PipelineProcessor(List<? extends PipelineHandler> pipeline, @Nullable Class<?> outputType) {
        this.pipeline = pipeline;
        this.outputType = outputType;
    }
    
    public PipelineProcessor input(@Nullable Object initialInput) {
        this.initialInput = initialInput;
        return this;
    }
       
    public Optional<Object> output() {
        
        System.out.println("InputType: " + TypeUtils.parseClassType(initialInput));
        System.out.println("OutputType: " + TypeUtils.parseClassType(outputType));
        
        // 1.) pre-execution check for type sanity
        Map<Integer, ClassType> runtimePiplineChecks = PipelineUtils.typeCheckPipeline(pipeline, initialInput, outputType);
            
        System.out.println("Checks on the following are needed: " + runtimePiplineChecks);
        Object lastOutput = initialInput;
        for(int i = 0; i < pipeline.size(); i++) {
            PipelineHandler handle = pipeline.get(i);
            
            // ensure null inputs are allowed if applicable
            if (lastOutput == null && !handle.inputNullable()) {
                throw new NullNotAllowedException("PipelineHandler (" + handle.id() + ") at index " + i + " does not permit NULL inputs");
            }
            
            // ensure runtime check passes
            ClassType expectedClassType = runtimePiplineChecks.get(i);
            if (expectedClassType != null) {
                ClassType inputClassType = TypeUtils.parseClassType(lastOutput);
                
                // ignore Null class as we would have not been able 
                // to reach this point if they weren't allowed
                if (!inputClassType.toString().equals(com.github.type.utils.domain.Null.class.getName())) {
                    try {
                        inputClassType.compare(expectedClassType);
                    } catch (TypeMismatchException tme) {
                        String message;
                        if (i == 0) {
                            message = "Initial input to " + PipelineProcessor.class.getSimpleName() + " ";
                        } else {
                            int index = i - 1;
                            message = "Handler (" + pipeline.get(index).id() + ") at index " + index + " ";
                        }
                        throw new ProcessTimeTypeMismatchException(message 
                                + "outputs do not match Handler (" 
                                + handle.id() + ") at index " + i + " inputs.", tme);
                    } 
                }
            }
                    
            // process function
            lastOutput = handle.process(lastOutput);
            
            // ensure null outputs are allowed in applicable
            if (lastOutput == null && !handle.outputNullable()) {
                throw new NullNotAllowedException("PipelineHandler (" + handle.id() + ") at index " + i + " does not permit NULL outputs");
            }
        }        
        
        return Optional.ofNullable(lastOutput);
    }
    
    public static <T, V> Builder<T, V> builder() {
        return new Builder();
    }
    
    public static class Builder<T, V> {
    
        private final Logger LOGGER = Logger.getLogger(PipelineProcessor.class.getName());
        private final List<PipelineHandler> pipelineHandlers = new ArrayList<>();
        private Class<?> outputType = null;
        
        /**
         * Add handler to this pipeline
         * 
         * @param pipelineHandler handler to append to the end of this pipeline.
         * @return this Builder.
         */
        public Builder handler(Class pipelineHandler) {
            checkNotNull(pipelineHandler, "pipelineHandler cannot be null");
            
            if (com.google.common.base.Function.class.isAssignableFrom(pipelineHandler)) {
                com.google.common.base.Function function = (com.google.common.base.Function)ReflectionUtils.newInstance(pipelineHandler);
                return handler(function);
            } else if (java.util.function.Function.class.isAssignableFrom(pipelineHandler)) {
                java.util.function.Function function = (java.util.function.Function)ReflectionUtils.newInstance(pipelineHandler);
                return handler(function);
            } else {
                throw new ClassCastException("pipelineHandler must be either an instance of com.google.common.base.Function or java.util.function.Function");
            }
        }

        /**
         * Add handler to this pipeline
         * 
         * @param pipelineHandler handler to append to the end of this pipeline.
         * @return this Builder.
         */
        public Builder handler(final com.google.common.base.Function<?, ?> pipelineHandler) {
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
        public Builder handler(final java.util.function.Function<?, ?> pipelineHandler) {
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
         * The outputType to check against output of pipeline. Useful when the output 
         * type is an Object but we want to ensure a specific type.
         * 
         * @param outputType optional outputType we expect from pipeline. 
         * @return this Builder.
         */
        public Builder outputType(final Class<?> outputType) {
            checkNotNull(outputType, "pipelineHandler cannot be null");
            this.outputType = outputType;
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
            return new PipelineProcessor(Collections.unmodifiableList(pipelineHandlers), this.outputType);
        }
    }
}
