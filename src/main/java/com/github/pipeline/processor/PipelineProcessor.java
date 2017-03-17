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
import static com.google.common.base.Preconditions.checkArgument;

import com.github.aap.processor.tools.ReflectionUtils;

import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import net.jodah.failsafe.RetryPolicy;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

/**
 * Implementation of AbstractPipelineProcessor.
 * 
 * @author github.
 */
public class PipelineProcessor extends AbstractPipelineProcessor {
    
    private PipelineProcessor(final List<? extends PipelineHandler> pipeline, final List<Subscriber> subscribers, final RetryPolicy retryPolicy) {
        super(pipeline, subscribers, retryPolicy);
    }
    
    /**
     * Get the output of this PipelineProcessor. This is analogous to starting 
     * the pipeline.
     * 
     * @return output of invoking pipeline.
     */
    public Object output() {
        return output(null);
    }
    
    /**
     * Get the output of this PipelineProcessor. This is analogous to starting 
     * the pipeline.
     * 
     * @param input optional input to this pipeline.
     * @return output of invoking pipeline.
     */
    public Object output(@Nullable final Object input) {
        return apply(input);
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder implements Publisher {
    
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
         * Add class subscriber to this handler to be notified of updates.
         * 
         * @param subscriber the Subscriber to add to this handler
         * @return this Builder.
         */
        public Builder subscriber(final Class<? extends Subscriber> subscriber) {
            subscribe(ReflectionUtils.newInstance(subscriber));
            return this;
        }
        
        /**
         * Add instance of subscriber to this handler to be notified of updates.
         * 
         * @param subscriber the Subscriber to add to this handler
         * @return this Builder.
         */
        public Builder subscriber(final Subscriber subscriber) {
            subscribe(subscriber);
            return this;
        }
        
        @Override
        public void subscribe(final Subscriber subscriber) {
            subscribers.add(checkNotNull(subscriber, "subscriber cannot be null"));
        }
        
        /**
         * Add a RetryPolicy to this PipelineProcessor
         * 
         * @param retryPolicy the RetryPolicy to add to this PipelineProcessor
         * @return this Builder.
         */
        public Builder retryPolicy(final RetryPolicy retryPolicy) {
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
            return new PipelineProcessor(Collections.unmodifiableList(pipelineHandlers), subscribers, this.retryPolicy);
        }
    }
}
