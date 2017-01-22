/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.pipeline.processor.exceptions;

/**
 * Thrown, but only during "process time", when some handler outputs 
 * 'java.lang.Object' and the next handler in the pipeline expects a 
 * specific Type of which the previous output is not of. For example:
 * 
 *      class MyFirstHandler<Integer, Object> {
 * 
 *          public Object apply(Integer myInteger) {
 *              return 123;
 *          }
 *      }
 * 
 * This class outputs a type of Object but we can clearly see that t will be 
 * massaged to type 'java.lang.Integer'. The problem happens when this 
 * output is then fed to a class which looks like:
 * 
 *      class MySecondHandler<String, Boolean> {
 * 
 *          public Boolean apply(String myString) {
 *              return true;
 *          }
 *      }
 * 
 * We can clearly see this expects a 'java.lang.String' but the previous 
 * instance is giving us an 'java.lang.Integer'. This will of course ultimately
 * fail at "process time".
 * 
 * At "check time", which can be thought of as compile-time, we have no way of 
 * knowing what type of Object will be returned in the first instance. So, and 
 * when the first instance returns, we check to ensure we can safely pass the 
 * output of the first handler to the input of the second handler. If not then 
 * this exception is thrown.
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
