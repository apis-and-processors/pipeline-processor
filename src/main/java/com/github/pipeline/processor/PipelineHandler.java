/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.pipeline.processor;

import com.google.common.base.Function;

/**
 *
 * @author cdancy
 */
/**
 *
 * @author github.
 * @param <T>
 * @param <V>
 */
public class PipelineHandler<T, V> {

    private final Function<T, V> function;    
    private final Object retryPolicy;

    public PipelineHandler(Function<T, V> function, Object retryPolicy) {
        this.function = function;
        this.retryPolicy = retryPolicy;
    }
    
    public Function function() {
        return function;
    }
    
    public Object retryPolicy() {
        return retryPolicy;
    }
    
    public V process(T t) {
        return function.apply(t);
    }
    
    public static <T, V> PipelineHandler newInstance(Function<T, V> function, Object retryPolicy) {
        return new PipelineHandler(function, retryPolicy);
    }
}
