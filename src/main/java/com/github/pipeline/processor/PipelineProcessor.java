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

import static com.github.pipeline.processor.PipelineConstants.INDEX_STRING;
import static com.github.pipeline.processor.PipelineConstants.FUNCTION_REGEX;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;

import com.github.aap.processor.tools.ReflectionUtils;
import com.github.aap.processor.tools.TypeUtils;
import com.github.aap.processor.tools.domain.ClassType;
import com.github.aap.processor.tools.domain.Null;
import com.github.aap.processor.tools.exceptions.TypeMismatchException;
import com.github.pipeline.processor.exceptions.NullNotAllowedException;
import com.github.pipeline.processor.exceptions.ProcessTimeTypeMismatchException;
import com.github.pipeline.processor.utils.PipelineUtils;
import com.google.common.collect.ImmutableList;

import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.reactivestreams.Subscriber;

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
    private final List<Subscriber> subscribers;
    private final Map<Integer, ClassType> runtimePipelineChecks;

    private volatile V initialInput;
            
    /**
     * Create PipelineProcessor from pre-built pipeline with optional Subscribers.
     * 
     * @param pipeline pipeline to process
     * @param subscribers optional subscribers to get callback notifications
     */
    private PipelineProcessor(final List<? extends PipelineHandler> pipeline, final List<Subscriber> subscribers) {
        this.pipeline = pipeline;
        this.subscribers = ImmutableList.copyOf(subscribers);
        this.runtimePipelineChecks = PipelineUtils.typeCheckPipeline(pipeline);        
    }

    /**
     * Optionally provide some input into the first handler of this processor.
     * 
     * @param initialInput optional input to pass to first handler.
     * @return this PipelineProcessor.
     */
    public PipelineProcessor input(@Nullable final V initialInput) {
        this.initialInput = initialInput;
        return this;
    }

    /**
     * Get list of Subscribers.
     * 
     * @return list of Subscribers
     */
    public List<Subscriber> subscribers() {
        return this.subscribers;
    }
    
    /**
     * Get the output of this PipelineProcessor. This is analogous to starting 
     * the pipeline.
     * 
     * @return Optional which encapsulates the potentially NULL value.
     */
    public Optional<R> output() {

        Object lastOutput = initialInput;
        for (int i = 0; i < pipeline.size(); i++) {
            final PipelineHandler handle = pipeline.get(i);
                        
            // ensure null inputs are allowed if applicable
            if (lastOutput == null && !handle.inputNullable()) {
                final ClassType inputType = handle.classType().firstSubTypeMatching(FUNCTION_REGEX).subTypeAtIndex(0);
                if (!(inputType.toString().equals(Void.class.getName()) 
                        || inputType.toString().equals(Null.class.getName()))) {
                    throw new NullNotAllowedException("PipelineHandler (" + handle.id() + INDEX_STRING + i + " does not permit NULL inputs");
                }                
            }
            
            // ensure runtime check passes
            ClassType expectedClassType = runtimePipelineChecks.get(i);
            if (expectedClassType == null && i == 0) {
                expectedClassType = pipeline.get(0).classType().firstSubTypeMatching(FUNCTION_REGEX).subTypeAtIndex(0);
            }
            
            if (expectedClassType != null) {
                final ClassType handlerClassType = TypeUtils.parseClassType(lastOutput);
                
                // ignore Null class as we would have not been able 
                // to reach this point if they weren't allowed
                if (!handlerClassType.toString().equals(Null.class.getName())) {
                    try {
                        handlerClassType.compare(expectedClassType);
                    } catch (TypeMismatchException tme) {
                        String message;
                        if (i == 0) {
                            message = "Initial input to " + PipelineProcessor.class.getSimpleName() + " does ";
                        } else {
                            final int index = i - 1;
                            message = "Handler (" + pipeline.get(index).id() + INDEX_STRING + index + " outputs do ";
                        }
                        throw new ProcessTimeTypeMismatchException(message 
                                + "not match Handler (" 
                                + handle.id() + INDEX_STRING + i + " inputs.", tme);
                    } 
                }
            }
                    
            // process function
            lastOutput = handle.process(lastOutput);
            
            // ensure null outputs are allowed if applicable
            if (lastOutput == null && !handle.outputNullable()) {
                throw new NullNotAllowedException("PipelineHandler (" + handle.id() + INDEX_STRING + i + " does not permit NULL outputs");
            }
        }
        
        if (lastOutput instanceof Optional) {
            return Optional.class.cast(lastOutput);
        } else {
            return Optional.ofNullable((R)lastOutput);
        }
    }
    
    public static <T, V> Builder<T, V> builder() {
        return new Builder();
    }
    
    public static class Builder<V, R> {
    
        private final Logger logger = Logger.getLogger(PipelineProcessor.class.getName());
        private final List<PipelineHandler> pipelineHandlers = Lists.newArrayList();
        private final List<Subscriber> subscribers = Lists.newArrayList();
        
        /**
         * Add class of java.util.function.Function to this pipeline
         * 
         * @param pipelineHandler handler to append to the end of this pipeline.
         * @return this Builder.
         */
        public Builder handler(final Class<? extends Function> pipelineHandler) {
            checkNotNull(pipelineHandler, "pipelineHandler cannot be null");
            return handler(ReflectionUtils.newInstance(pipelineHandler));
        }
        
        /**
         * Add instance of java.util.function.Function to this pipeline
         * 
         * @param pipelineHandler handler to append to the end of this pipeline.
         * @return this Builder.
         */
        public Builder handler(final Function<?, ?> pipelineHandler) {
            checkNotNull(pipelineHandler, "pipelineHandler cannot be null");
            final PipelineHandler handler = PipelineHandler.newInstance(pipelineHandler);
            return handler(handler);
        }
        
        /**
         * Add PipelineHandler to this pipeline.
         * 
         * @param pipelineHandler handler to append to the end of this pipeline.
         * @return this Builder.
         */
        public Builder handler(final PipelineHandler pipelineHandler) {
            checkNotNull(pipelineHandler, "pipelineHandler cannot be null");
            pipelineHandlers.add(pipelineHandler);
            logger.log(Level.CONFIG, "Handler '{0}' added to pipeline", pipelineHandler.getClass().getName());
            return this;
        }
        
        /**
         * Add subscriber to this handler to be notified of updates.
         * 
         * @param subscriber the Subscriber to add to this handler
         * @return this Builder.
         */
        public Builder subscribe(final Subscriber subscriber) {
            subscribers.add(checkNotNull(subscriber, "subscriber cannot be null"));
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
            return new PipelineProcessor<>(Collections.unmodifiableList(pipelineHandlers), subscribers);
        }
    }
}
