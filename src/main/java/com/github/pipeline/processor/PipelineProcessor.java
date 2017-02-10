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

import static com.github.pipeline.processor.PipelineConstants.FUNCTION_REGEX;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;

import com.github.pipeline.processor.exceptions.NullNotAllowedException;
import com.github.pipeline.processor.exceptions.ProcessTimeTypeMismatchException;

import com.github.pipeline.processor.utils.PipelineUtils;
import com.github.aap.type.utils.ClassType;
import com.github.aap.type.utils.ReflectionUtils;
import com.github.aap.type.utils.TypeUtils;
import com.github.aap.type.utils.domain.Null;
import com.github.aap.type.utils.exceptions.TypeMismatchException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;

/**
 * PipelineProcessor acts as an assembly line to process functions. The output 
 * of one function is the input to the next and so on and so forth. The return 
 * value of this processor is the output of the last function executed.
 * 
 * @param <V> optional input value to processor.
 * @param <R> expected return value of pipeline. Defaults to Object.
 * @author github.
 */
public class PipelineProcessor<V, R> {
    
    private final List<? extends PipelineHandler> pipeline;
    private final Map<Integer, ClassType> runtimePipelineChecks;

    private volatile V initialInput;
            
    public PipelineProcessor(List<? extends PipelineHandler> pipeline) {
        this.pipeline = pipeline;
        this.runtimePipelineChecks = PipelineUtils.typeCheckPipeline(pipeline);        
    }
      
    public PipelineProcessor input(@Nullable V initialInput) {
        this.initialInput = initialInput;
        return this;
    }
       
    public Optional<R> output() {

        Object lastOutput = initialInput;
        for(int i = 0; i < pipeline.size(); i++) {
            PipelineHandler handle = pipeline.get(i);
                        
            // ensure null inputs are allowed if applicable
            if (lastOutput == null && !handle.inputNullable()) {
                ClassType inputType = handle.classType().firstSubTypeMatching(FUNCTION_REGEX).subTypeAtIndex(0);
                if (!(inputType.toString().equals(Void.class.getName()) 
                        || inputType.toString().equals(Null.class.getName()))) {
                    throw new NullNotAllowedException("PipelineHandler (" + handle.id() + ") at index " + i + " does not permit NULL inputs");
                }                
            }
            
            // ensure runtime check passes
            ClassType expectedClassType = runtimePipelineChecks.get(i);
            if (expectedClassType == null && i == 0) {
                expectedClassType = pipeline.get(0).classType().firstSubTypeMatching(FUNCTION_REGEX).subTypeAtIndex(0);
            }
            
            if (expectedClassType != null) {
                ClassType handlerClassType = TypeUtils.parseClassType(lastOutput);
                
                // ignore Null class as we would have not been able 
                // to reach this point if they weren't allowed
                if (!handlerClassType.toString().equals(com.github.aap.type.utils.domain.Null.class.getName())) {
                    try {
                        handlerClassType.compare(expectedClassType);
                    } catch (TypeMismatchException tme) {
                        String message;
                        if (i == 0) {
                            message = "Initial input to " + PipelineProcessor.class.getSimpleName() + " does ";
                        } else {
                            int index = i - 1;
                            message = "Handler (" + pipeline.get(index).id() + ") at index " + index + " outputs do ";
                        }
                        throw new ProcessTimeTypeMismatchException(message 
                                + "not match Handler (" 
                                + handle.id() + ") at index " + i + " inputs.", tme);
                    } 
                }
            }
                    
            // process function
            lastOutput = handle.process(lastOutput);
            
            // ensure null outputs are allowed if applicable
            if (lastOutput == null && !handle.outputNullable()) {
                throw new NullNotAllowedException("PipelineHandler (" + handle.id() + ") at index " + i + " does not permit NULL outputs");
            }
        }
        
        if (lastOutput instanceof Optional) {
            return Optional.class.cast(lastOutput);
        } else {
            return Optional.ofNullable((R)lastOutput);
        }
    }
    
    public static <V, R> Builder<V, R> builder() {
        return new Builder();
    }
    
    public static class Builder<V, R> {
    
        private final Logger LOGGER = Logger.getLogger(PipelineProcessor.class.getName());
        private final List<PipelineHandler> pipelineHandlers = new ArrayList<>();
        
        /**
         * Add handler to this pipeline. pipelineHandler must be an instance of com.google.common.base.Function
         * or java.util.function.Function.
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
         * Add instance of com.google.common.base.Function to this pipeline
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
         * Add instance of java.util.function.Function to this pipeline
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
         * Add PipelineHandler to this pipeline
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
         * @param <V> optional Typed input value to processor.
         * @param <R> expected return value of pipeline. Defaults to Object.
         * @return newly created PipelineProcessor.
         */
        public <V, R> PipelineProcessor <V, R> build() {
            checkArgument(!pipelineHandlers.isEmpty(), "Cannot build processor with no handlers");
            return new PipelineProcessor<>(Collections.unmodifiableList(pipelineHandlers));
        }
    }
}
