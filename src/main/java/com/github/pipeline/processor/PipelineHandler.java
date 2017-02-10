/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.pipeline.processor;

import static com.google.common.base.Preconditions.checkNotNull;

import com.github.aap.type.utils.ClassType;
import com.github.aap.type.utils.TypeUtils;
import com.google.common.base.Throwables;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

/**
 * Represents a single handler within a PipelineProcessor. It's responsible for 
 * executing either an instance of com.google.common.base.Function or java.util.function.Function.
 * 
 * The handler takes in a single input and returns a single output. This may be 
 * the input of some previously executed PipelineHandler and the output in turn may 
 * be serving as the input for the next PipelineHandler.
 * 
 * PipelineHandler's can be optionally re-run by passing in a RetryPolicy. Default is to 
 * execute the handler once and return whatever value was produced.
 * 
 * @param <V> input value to handler
 * @param <R> return value of handler
 */
public class PipelineHandler <V, R> {

    private static final Logger LOGGER = Logger.getLogger(PipelineHandler.class.getName());

    private static final RetryPolicy DEFAULT_RETRY_POLICY = new RetryPolicy().withMaxRetries(0).abortOn(ClassCastException.class);
    
    private static final String RETRY_ATTEMPT_MESSAGE = "Execution attempt failed due to: {0}";
    private static final String RETRY_FAILED_MESSAGE = "Execution failed due to: {0}";
    private static final String RETRY_RUN_MESSAGE = "Execution attempt {0} on {1}";
    
    private final Object function;
    private final RetryPolicy retryPolicy;
    private final boolean inputNullable;
    private final boolean outputNullable;
    private final ClassType classType;

    private PipelineHandler(Object function, @Nullable RetryPolicy retryPolicy, boolean inputNullable, boolean outputNullable) {
        this.function = function;
        this.retryPolicy = retryPolicy != null ? retryPolicy : DEFAULT_RETRY_POLICY;
        this.inputNullable = inputNullable;
        this.outputNullable = outputNullable;
        this.classType = TypeUtils.parseClassType(function);
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
     * ClassType instance of the backing com.google.common.base.Function or java.util.function.Function. 
     * Allows us to know what needs to be passed in and what needs to be returned at checktime and runtime.
     * 
     * @return ClassType instance for backing com.google.common.base.Function or java.util.function.Function
     */
    public ClassType classType() {
        return this.classType;
    }
    
    /**
     * The implementing class name of the backing com.google.common.base.Function or java.util.function.Function.
     * 
     * @return id of this handler. Not guaranteed to be unique.
     */
    public String id() {
        return function.getClass().getName();
    }
    
    public R process(V input) {
        
        AtomicReference<R> responseReference = new AtomicReference<>();
        
        Failsafe.with(retryPolicy)
            .onFailedAttempt(attempt -> LOGGER.log(Level.WARNING, RETRY_ATTEMPT_MESSAGE, attempt.getMessage()))
            .onFailure(failure -> LOGGER.log(Level.SEVERE, RETRY_FAILED_MESSAGE, failure.getMessage()))
            .run((ctx) -> { 
                
                Object [] loggerParams = {ctx.getExecutions() + 1, function.toString()};
                LOGGER.log(Level.FINE, RETRY_RUN_MESSAGE, loggerParams);
                
                if (function instanceof com.google.common.base.Function) {
                    com.google.common.base.Function worker = (com.google.common.base.Function)function;
                    responseReference.set((R) worker.apply(input));
                } else if (function instanceof java.util.function.Function) {
                    java.util.function.Function worker = (java.util.function.Function)function;
                    responseReference.set((R) worker.apply(input));
                } else {
                    throw new ClassCastException("Cannot cast '" + function + "' to either an instance of com.google.common.base.Function or java.util.function.Function");
                }                
            });
                
        return responseReference.get();
    }
    
    /**
     * Create new instance of PipelineHandler backed by com.google.common.base.Function 
     * 
     * @param <V> input value to function
     * @param <R> return value of function
     * @param function instance of com.google.common.base.Function
     * @param retryPolicy optional RetryPolicy to re-run function upon failure.
     * @return newly created PipelineHandler
     */
    public static <V, R> PipelineHandler newInstance(com.google.common.base.Function<V, R> function, @Nullable RetryPolicy retryPolicy) {
        checkNotNull(function, "function cannot be null");
        boolean [] pair = inputOutputNullables(function);
        return new PipelineHandler(function, retryPolicy, pair[0], pair[1]);
    }
    
    /**
     * Create new instance of PipelineHandler backed by java.util.function.Function 
     * 
     * @param <V> input value to function
     * @param <R> return value of function
     * @param function instance of java.util.function.Function
     * @param retryPolicy optional RetryPolicy to re-run function upon failure.
     * @return newly created PipelineHandler
     */
    public static <V, R> PipelineHandler newInstance(java.util.function.Function<V, R> function, @Nullable RetryPolicy retryPolicy) {
        checkNotNull(function, "function cannot be null");
        boolean [] pair = inputOutputNullables(function);
        return new PipelineHandler(function, retryPolicy, pair[0], pair[1]);
    }
    
    /**
     * Helper method to determine if a given function accepts null inputs and is 
     * permitted to return null outputs.
     * 
     * @param function instance of either com.google.common.base.Function or java.util.function.Function
     * @return boolean array of exactly size 2 where index 0 denotes if input is nullable and index 1 
     *         denotes if output is nullable,
     */
    private static boolean[] inputOutputNullables(Object function) {
        try {
            
            Method applyMethod = function.getClass().getDeclaredMethod("apply", Object.class);
            boolean outputNullable = applyMethod.getDeclaredAnnotation(Nullable.class) != null;
            boolean inputNullable = false;
            
            Annotation[] parameterAnnotations = applyMethod.getParameterAnnotations()[0];
            for (int i = 0; i < parameterAnnotations.length; i++) {
                if (parameterAnnotations[i].annotationType().equals(Nullable.class)) {
                    inputNullable = true;
                    break;
                }
            }
            
            boolean[] pair = {inputNullable, outputNullable};
            return pair;
        } catch (NoSuchMethodException | SecurityException ex) {
            throw Throwables.propagate(ex);
        }
    }
}
