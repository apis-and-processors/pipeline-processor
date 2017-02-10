/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.pipeline.processor.exceptions;

/**
 * Thrown when any inputs/outputs do not match while processing (i.e. at runtime) 
 * a given PipelineProcessor.
 * 
 * @author cdancy
 */
public class ProcessTimeTypeMismatchException extends RuntimeException {
    
    public ProcessTimeTypeMismatchException() {
        super();
    }
    
    public ProcessTimeTypeMismatchException(String s) {
        super(s);
    }
    
    public ProcessTimeTypeMismatchException(String s, Throwable throwable) {
        super(s, throwable);
    }
    
    public ProcessTimeTypeMismatchException(Throwable throwable) {
        super(throwable);
    }
}
