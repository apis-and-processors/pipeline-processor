/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.pipeline.processor.exceptions;

/**
 * Thrown when any inputs/outputs do not match during initialization pass 
 * of a given PipelineProcessor.
 * 
 * @author cdancy
 */
public class CheckTimeTypeMismatchException extends RuntimeException {
    
    public CheckTimeTypeMismatchException() {
        super();
    }
    
    public CheckTimeTypeMismatchException(String s) {
        super(s);
    }
    
    public CheckTimeTypeMismatchException(String s, Throwable throwable) {
        super(s, throwable);
    }
    
    public CheckTimeTypeMismatchException(Throwable throwable) {
        super(throwable);
    }
}
