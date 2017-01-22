/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.pipeline.processor;

import com.google.common.base.Function;
import org.testng.annotations.Test;

/**
 *
 * @author cdancy
 */
public class PipelineProcessorTest {
   
    class Handler1 implements Function<String, Integer> {
        @Override
        public Integer apply(String object) {
            return 123;
        }
    }
    
    class Handler2 implements Function<Void, Boolean> {
        @Override
        public Boolean apply(Void object) {
            return false;
        }
    }
        
    class Handler3 implements Comparable, Function<Object, String> {
        @Override
        public String apply(Object object) {
            return "world";
        }

        @Override
        public int compareTo(Object o) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }
        
    @Test
    public void testSomeLibraryMethod() {
        
        PipelineProcessor.Builder<String, String> builder2 = PipelineProcessor.<String, String> builder();
        
        Object obj = builder2.handler(Handler1.class).handler(Handler2.class).handler(Handler3.class).handler(new Handler3()).input("hello").output();
        System.out.println("output: " + obj);
    }
}
