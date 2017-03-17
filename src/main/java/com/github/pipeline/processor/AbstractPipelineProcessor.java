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
import com.github.aap.processor.tools.TypeUtils;
import com.github.aap.processor.tools.domain.ClassType;
import com.github.aap.processor.tools.domain.Null;
import com.github.aap.processor.tools.exceptions.TypeMismatchException;
import com.github.pipeline.processor.exceptions.NullNotAllowedException;
import com.github.pipeline.processor.exceptions.ProcessTimeTypeMismatchException;
import com.github.pipeline.processor.utils.PipelineUtils;
import com.google.common.collect.ImmutableList;

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
 * AbstractPipelineProcessor acts as an assembly line to process functions. The output 
 * of one function is the input to the next and so on and so forth. The return 
 * value of this processor is the output of the last function executed.
 * 
 * @param <V> optional input value to processor.
 * @param <R> expected return value of pipeline. Defaults to Object.
 * @author github.
 */
public abstract class AbstractPipelineProcessor<V,R> implements Function<V, R> {
    
    public static final Logger LOGGER = Logger.getLogger(AbstractPipelineProcessor.class.getName());
    public static final RetryPolicy DEFAULT_RETRY_POLICY = new RetryPolicy().withMaxRetries(0);
    
    public static final String RETRY_ATTEMPT_MESSAGE = "Execution attempt failed due to: {0}";
    public static final String RETRY_FAILED_MESSAGE = "Execution failed due to: {0}";
    public static final String RETRY_RUN_MESSAGE = "Execution attempt on {0}";
    public static final String SUCCESS_MESSAGE = "Execution was successful on {0}";

    protected final List<? extends PipelineHandler> pipeline;
    protected final List<Subscriber> subscribers;
    protected final RetryPolicy retryPolicy;
    protected final Map<Integer, ClassType> runtimePipelineChecks;
    
    /**
     * Create PipelineProcessor from pre-built pipeline with optional Subscribers.
     * 
     * @param pipeline pipeline to process
     * @param subscribers optional subscribers to get callback notifications
     * @param retryPolicy optional RetryPolicy to use for this pipeline
     */
    public AbstractPipelineProcessor(final List<? extends PipelineHandler> pipeline, 
            final List<Subscriber> subscribers, 
            final RetryPolicy retryPolicy) {
        
        this.pipeline = pipeline;
        this.subscribers = subscribers != null ? ImmutableList.copyOf(subscribers) : ImmutableList.<Subscriber>of();
        this.retryPolicy = (retryPolicy != null 
                ? retryPolicy
                : DEFAULT_RETRY_POLICY).abortOn(NullNotAllowedException.class, ProcessTimeTypeMismatchException.class);
        this.runtimePipelineChecks = PipelineUtils.typeCheckPipeline(pipeline);        
    } 
    
    /**
     * Get the output of this PipelineProcessor giving it an initial input. 
     * This is analogous to starting the pipeline.
     * 
     * @param input optional initial input into PipelineProcessor.
     * @return Optional which encapsulates the potentially NULL value.
     */
    @Override
    public R apply(@Nullable final V input) {

        // holds the eventual response of this execution
        final AtomicReference<Object> responseReference = new AtomicReference<>(input);
        
        // holds the starting point of next handler to execute
        final AtomicInteger indexReference = new AtomicInteger(0);

        Failsafe.with(retryPolicy)
                .onFailedAttempt(failure ->  {
                    LOGGER.log(Level.WARNING, RETRY_ATTEMPT_MESSAGE, failure.getMessage()); 

                    indexReference.decrementAndGet(); 
                })
                .onSuccess(success -> {
                    LOGGER.log(Level.FINE, SUCCESS_MESSAGE, success);
                    
                    // fulfill contract for reactive-streams.onComplete
                    if (this.subscribers().size() > 0) {
                        for (int i = 0; i < this.subscribers().size(); i++) {
                            this.subscribers().get(i).onComplete();
                        }
                    }
                })
                .onFailure(failure -> { 
                    LOGGER.log(Level.SEVERE, RETRY_FAILED_MESSAGE, failure.getMessage());
                    
                    // fulfill contract for reactive-streams.onError
                    if (this.subscribers().size() > 0) {
                        for (int i = 0; i < this.subscribers().size(); i++) {
                            this.subscribers().get(i).onError(failure);
                        }
                    }
                })
                .run(ctx -> {

                    for (int i = indexReference.get(); i < pipeline.size(); i++) {
                        indexReference.incrementAndGet();

                        final PipelineHandler handle = pipeline.get(i);
                        LOGGER.log(Level.FINE, RETRY_RUN_MESSAGE, handle.id());

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
                            throw new NullNotAllowedException("PipelineHandler (" 
                                    + handle.id() + INDEX_STRING + i + " does not permit NULL outputs");
                        }

                        // fulfill contract for reactive-streams.onNext
                        if (this.subscribers().size() > 0) {
                            for (int j = 0; j < this.subscribers().size(); j++) {
                                this.subscribers().get(j).onNext(handlerOutput);
                            }
                        }
                    
                        responseReference.set(handlerOutput);
                    }
                });
        
        return (R) responseReference.get();
    }
    
    /**
     * Get list of Subscribers.
     * 
     * @return list of Subscribers
     */
    public List<Subscriber> subscribers() {
        return this.subscribers;
    }
}
