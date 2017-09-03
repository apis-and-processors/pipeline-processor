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

import static com.github.pipeline.processor.PipelineConstants.FUNCTION_REGEX;
import static com.github.pipeline.processor.PipelineConstants.NULL_ALLOWED_TYPE_REGEX;

import com.github.pipeline.processor.annotations.Cache;

import com.github.aap.processor.tools.ClassTypeParser;
import com.github.aap.processor.tools.domain.ClassType;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.function.Function;
import javax.annotation.Nullable;

/**
 * Represents a single handler within a PipelineProcessor. 
 * 
 * <p>
 * The handler takes in a single input and returns a single output. This may be 
 * the input of some previously executed PipelineHandler and the output in turn may 
 * be serving as the input for the next PipelineHandler.
 * 
 * PipelineHandler's can be optionally re-run by passing in a RetryPolicy. Default is to 
 * execute the handler once and return whatever value was produced.
 * </p>
 * 
 * @param <V> input value to handler
 * @param <R> return value of handler
 */
public class PipelineHandler <V, R> {
    
    private final Function function;
    private final boolean inputNullable;
    private final boolean outputNullable;
    private final String inputCacheKey;
    private final String outputCacheKey;
    private final ClassType classType;

    private PipelineHandler(final Function function, 
            final boolean inputNullable, 
            final boolean outputNullable, 
            final String inputCacheKey, 
            final String outputCacheKey) {
        
        this.function = function;
        this.classType = ClassTypeParser.parse(function);
        this.inputNullable = inputNullable;
        
        // if output is not annotated with @Nullable check if return type
        // accepts a NULL value (e.g. Void or Null class types).
        if (!outputNullable) {
            final ClassType possibleNullAllowedType = this.classType.firstSubTypeMatching(FUNCTION_REGEX).subTypeAtIndex(1);
            this.outputNullable = possibleNullAllowedType.name().matches(NULL_ALLOWED_TYPE_REGEX);
        } else {
            this.outputNullable = outputNullable;
        }
        
        this.inputCacheKey = inputCacheKey;
        this.outputCacheKey = outputCacheKey;
    }
    
    /**
     * Whether NULL inputs are allowed.
     * 
     * @return true if we accept null inputs false otherwise.
     */
    public boolean inputNullable() {
        return this.inputNullable;
    }
    
    /**
     * Whether NULL outputs are allowed.
     * 
     * @return true if we accept null outputs false otherwise.
     */
    public boolean outputNullable() {
        return this.outputNullable;
    }
    
    /**
     * Key used to get input object from cache.
     * 
     * @return cache key or null if N/A.
     */
    public String inputCacheKey() {
        return inputCacheKey;
    }
    
    /**
     * Key used to put output object into cache.
     * 
     * @return cache key or null if N/A.
     */
    public String outputCacheKey() {
        return outputCacheKey;
    }
    
    /**
     * ClassType instance of java.util.function.Function. 
     * Allows us to know what needs to be passed in and 
     * what needs to be returned at checktime and runtime.
     * 
     * @return ClassType instance for backing java.util.function.Function
     */
    public ClassType classType() {
        return this.classType;
    }
    
    /**
     * The implementing class name of the backing java.util.function.Function.
     * 
     * @return id of this handler. Not guaranteed to be unique.
     */
    public String id() {
        return function.getClass().getName();
    }
    
    public R process(final V input) {
        return (R) function.apply(input);
    }
    
    /**
     * Create new instance of PipelineHandler backed by java.util.function.Function 
     * 
     * @param <V> input value to function
     * @param <R> return value of function
     * @param function instance of java.util.function.Function
     * @return newly created PipelineHandler
     */
    public static <V, R> PipelineHandler newInstance(final Function<V, R> function) {
        checkNotNull(function, "function cannot be null");        
        
        try {
            
            final Method applyMethod = function.getClass().getDeclaredMethod("apply", Object.class);
            final boolean outputNullable = applyMethod.getDeclaredAnnotation(Nullable.class) != null;
            final Cache outputCacheAnno = applyMethod.getDeclaredAnnotation(Cache.class);            
            final String outputCacheKey = (outputCacheAnno != null) ? outputCacheAnno.value() : null;

            boolean inputNullable = false;
            
            String inputCacheKey = null;
            final Annotation[] parameterAnnotations = applyMethod.getParameterAnnotations()[0];
            for (int i = 0; i < parameterAnnotations.length; i++) {
                final Class <? extends Annotation> annoType = parameterAnnotations[i].annotationType();
                if (annoType.equals(Nullable.class)) {
                    inputNullable = true;
                } else if (annoType.equals(Cache.class)) {
                    final Cache inputCacheAnno = (Cache)parameterAnnotations[i];
                    inputCacheKey = inputCacheAnno.value();
                }
            }
            
            return new PipelineHandler(function, inputNullable, outputNullable, inputCacheKey, outputCacheKey);

        } catch (NoSuchMethodException | SecurityException ex) {
            throw new RuntimeException(ex);
        }                
    }
}
