/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.pipeline.processor;

import com.github.type.utils.ClassType;
import com.github.type.utils.TypeUtils;
import static com.google.common.base.Preconditions.checkNotNull;
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
 * 
 * @param <T>
 * @param <V>
 */
public class PipelineHandler <T, V> {

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
    
    public boolean inputNullable() {
        return this.inputNullable;
    }
    
    public boolean outputNullable() {
        return this.outputNullable;
    }
    
    public ClassType classType() {
        return this.classType;
    }
    
    public String id() {
        return function.getClass().getName();
    }
    
    public V process(T input) {
        
        AtomicReference<V> responseReference = new AtomicReference<>();
        
        Failsafe.with(retryPolicy)
            .onFailedAttempt(attempt -> LOGGER.log(Level.WARNING, RETRY_ATTEMPT_MESSAGE, attempt.getMessage()))
            .onFailure(failure -> LOGGER.log(Level.SEVERE, RETRY_FAILED_MESSAGE, failure.getMessage()))
            .run((ctx) -> { 
                
                Object [] loggerParams = {ctx.getExecutions() + 1, function.toString()};
                LOGGER.log(Level.FINE, RETRY_RUN_MESSAGE, loggerParams);
                
                if (function instanceof com.google.common.base.Function) {
                    com.google.common.base.Function worker = (com.google.common.base.Function)function;
                    responseReference.set((V) worker.apply(input));
                } else if (function instanceof java.util.function.Function) {
                    java.util.function.Function worker = (java.util.function.Function)function;
                    responseReference.set((V) worker.apply(input));
                } else {
                    throw new ClassCastException("Cannot cast '" + function + "' to either an instance of com.google.common.base.Function or java.util.function.Function");
                }                
            });
                
        return responseReference.get();
    }
    
    public static <T, V> PipelineHandler newInstance(com.google.common.base.Function<T, V> function, @Nullable RetryPolicy retryPolicy) {
        checkNotNull(function, "function cannot be null");
        boolean [] pair = inputOutputNullables(function);
        return new PipelineHandler(function, retryPolicy, pair[0], pair[1]);
    }
    
    public static <T, V> PipelineHandler newInstance(java.util.function.Function<T, V> function, @Nullable RetryPolicy retryPolicy) {
        checkNotNull(function, "function cannot be null");
        boolean [] pair = inputOutputNullables(function);
        return new PipelineHandler(function, retryPolicy, pair[0], pair[1]);
    }
    
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
