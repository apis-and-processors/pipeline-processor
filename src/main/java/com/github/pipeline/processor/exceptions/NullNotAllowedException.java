/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.pipeline.processor.exceptions;

/**
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
