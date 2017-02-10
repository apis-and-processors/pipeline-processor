/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.pipeline.processor.exceptions;

/**
 * Throw when a null is either passed in or returned from a given Function
 * but is not allowed.
 * 
 * @author cdancy
 */
public class NullNotAllowedException extends RuntimeException {
    
    public NullNotAllowedException() {
        super();
    }
    
    public NullNotAllowedException(String s) {
        super(s);
    }
    
    public NullNotAllowedException(String s, Throwable throwable) {
        super(s, throwable);
    }
    
    public NullNotAllowedException(Throwable throwable) {
        super(throwable);
    }
}
