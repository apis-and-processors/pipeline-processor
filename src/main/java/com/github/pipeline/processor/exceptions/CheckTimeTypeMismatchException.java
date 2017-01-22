/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.pipeline.processor.exceptions;

/**
 * Thrown, but only during "check time" (which can be thought of as compile-time), 
 * when generic types of 2 classes are found to be different but were expected 
 * to be the same. For example:
 * 
 *      class MyFirstHandler<Object, Integer> {
 * 
 *          public Integer apply(Object myObj) {
 *              return 123;
 *          }
 *      }
 *  * 
 *      class MySecondHandler<String, Boolean> {
 * 
 *          public Boolean apply(String myString) {
 *              return true;
 *          }
 *      }
 * 
 * The 'MyFirstHandler' class outputs a type of 'java.lang.Integer' while the 
 * 'MySecondHandler' class takes an input a type of 'java.lang.String'. This 
 * will obviously fail at "process time" as an Integer cannot be a String. When 
 * such a case arises this exception is thrown. 
 * 
 * The ONLY exception to this rule is when the class 'MyFirstHandler' outputs a 
 * type say of 'java.lang.Object'. In this case we obviously have no idea at 
 * "check time" what the eventual returned Object will be and so have to do a 
 * check at "process time" to ensure a proper Object is being returned. Please 
 * refer to 'com.github.api.processor.exceptions.ProcessTimeTypeMismatchException'
 * on documentation for how this works.
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
