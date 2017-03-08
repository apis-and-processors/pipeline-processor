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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
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
    
    private static final Logger LOGGER = Logger.getLogger(PipelineProcessor.class.getName());
    private static final RetryPolicy DEFAULT_RETRY_POLICY = new RetryPolicy().withMaxRetries(5);
    
    private static final String RETRY_ATTEMPT_MESSAGE = "Execution attempt failed due to: {0}";
    private static final String RETRY_FAILED_MESSAGE = "Execution failed due to: {0}";
    private static final String RETRY_RUN_MESSAGE = "Execution attempt on {0}";
    
    private final List<? extends PipelineHandler> pipeline;
    private final List<Subscriber> subscribers;
    private final RetryPolicy retryPolicy;
    private final Map<Integer, ClassType> runtimePipelineChecks;
            
    /**
     * Create PipelineProcessor from pre-built pipeline with optional Subscribers.
     * 
     * @param pipeline pipeline to process
     * @param subscribers optional subscribers to get callback notifications
     */
    private PipelineProcessor(final List<? extends PipelineHandler> pipeline, final List<Subscriber> subscribers, final RetryPolicy retryPolicy) {
        
        this.pipeline = pipeline;
        this.subscribers = ImmutableList.copyOf(subscribers);
        this.retryPolicy = (retryPolicy != null 
                ? retryPolicy
                : DEFAULT_RETRY_POLICY).abortOn(NullNotAllowedException.class, ProcessTimeTypeMismatchException.class);
        this.runtimePipelineChecks = PipelineUtils.typeCheckPipeline(pipeline);        
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
    public R output() {
        return output(null);
    }
    
    /**
     * Get the output of this PipelineProcessor giving it an initial input. 
     * This is analogous to starting the pipeline.
     * 
     * @param input optional initial input into PipelineProcessor.
     * @return Optional which encapsulates the potentially NULL value.
     */
    public R output(@Nullable final V input) {

        // holds the eventual response of this execution
        final AtomicReference<Object> responseReference = new AtomicReference<>(input);
        
        // holds the starting point of next handler to execute
        final AtomicInteger indexReference = new AtomicInteger(0);

        Failsafe.with(retryPolicy)
                .onFailedAttempt(failure ->  {
                    indexReference.decrementAndGet(); 
                    LOGGER.log(Level.WARNING, RETRY_ATTEMPT_MESSAGE, failure.getMessage()); 
                })
                .onFailure(failure -> LOGGER.log(Level.SEVERE, RETRY_FAILED_MESSAGE, failure.getMessage()))
                .run(ctx -> { 

                    for (int i = indexReference.get(); i < pipeline.size(); i++) {
                        indexReference.incrementAndGet();

                        final PipelineHandler handle = pipeline.get(i);
                        LOGGER.log(Level.INFO, RETRY_RUN_MESSAGE, handle.id());

                        // ensure null inputs are allowed if applicable
                        if (responseReference.get() == null && !handle.inputNullable()) {
                            final ClassType inputType = handle.classType().firstSubTypeMatching(FUNCTION_REGEX).subTypeAtIndex(0);
                            if (!(inputType.toString().equals(Void.class.getName()) 
                                    || inputType.toString().equals(Null.class.getName()))) {
                                throw new NullNotAllowedException("PipelineHandler (" 
                                        + handle.id() + INDEX_STRING + i + " does not permit NULL inputs");
                            }                
                        }

                        // ensure runtime check passes
                        ClassType expectedClassType = runtimePipelineChecks.get(i);
                        if (expectedClassType == null && i == 0) {
                            expectedClassType = pipeline.get(0).classType().firstSubTypeMatching(FUNCTION_REGEX).subTypeAtIndex(0);
                        }

                        if (expectedClassType != null) {
                            final ClassType handlerClassType = TypeUtils.parseClassType(responseReference.get());

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
                        final Object handlerOutput = handle.process(responseReference.get());

                        // ensure null outputs are allowed if applicable
                        if (handlerOutput == null && !handle.outputNullable()) {
                            throw new NullNotAllowedException("PipelineHandler (" + handle.id() + INDEX_STRING + i + " does not permit NULL outputs");
                        } else {
                            responseReference.set(handlerOutput);
                        }
                    }
                });
        
        return (R) responseReference.get();
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
    
        private final Logger logger = Logger.getLogger(PipelineProcessor.class.getName());
        private final List<PipelineHandler> pipelineHandlers = Lists.newArrayList();
        private final List<Subscriber> subscribers = Lists.newArrayList();
        private RetryPolicy retryPolicy;
        
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
         * Add a RetryPolicy to this PipelineProcessor
         * 
         * @param retryPolicy the RetryPolicy to add to this PipelineProcessor
         * @return this Builder.
         */
        public Builder subscribe(final RetryPolicy retryPolicy) {
            this.retryPolicy = checkNotNull(retryPolicy, "retryPolicy cannot be null");
            return this;
        }
        
        /**
         * Build a PipelineProcessor from passed build parameters.
         * 
         * @return newly created PipelineProcessor.
         */
        public PipelineProcessor build() {
            checkArgument(!pipelineHandlers.isEmpty(), "Cannot build processor with no handlers");
            return new PipelineProcessor<>(Collections.unmodifiableList(pipelineHandlers), subscribers, this.retryPolicy);
        }
    }
}
