/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.pipeline.processor;

/**
 * 
 * @param <T>
 * @param <V>
 */
public class PipelineHandler <T, V> {

    private final Object function;
    private final Object retryPolicy;

    public PipelineHandler(Object function, Object retryPolicy) {
        this.function = function;
        this.retryPolicy = retryPolicy;
    }
    
    public Object function() {
        return function;
    }
    
    public Object retryPolicy() {
        return retryPolicy;
    }
    
    public V process(T input) {
        if (function == null) {
            throw new NullPointerException("Cannot have null function");
        } else if (function instanceof com.google.common.base.Function) {
            com.google.common.base.Function worker = (com.google.common.base.Function)function;
            return (V) worker.apply(input);
        } else if (function instanceof java.util.function.Function) {
            java.util.function.Function worker = (java.util.function.Function)function;
            return (V) worker.apply(input);
        } else {
            throw new ClassCastException("Cannot cast '" + function + "' to either an instance of com.google.common.base.Function or java.util.function.Function");
        }
    }
    
    public static <T, V> PipelineHandler newInstance(com.google.common.base.Function<T, V> function, Object retryPolicy) {
        return new PipelineHandler(function, retryPolicy);
    }
    
    public static <T, V> PipelineHandler newInstance(java.util.function.Function<T, V> function, Object retryPolicy) {
        return new PipelineHandler(function, retryPolicy);
    }
}
