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
   
    class Handler1 implements java.util.function.Function<Integer, Integer> {
        @Override
        public Integer apply(Integer object) {
            return 123;
        }
    }
    
    class Handler2 implements Function<Object, Boolean> {
        @Override
        public Boolean apply(Object object) {
            return Boolean.FALSE;
        }
    }
        
    class Handler3 implements Comparable, Function<Boolean, String> {
        @Override
        public String apply(Boolean object) {
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
        
        //Object obj = builder2.handler(Handler1.class).handler(Handler2.class).handler(Handler3.class).handler(new Handler3()).output("hello");
        Object obj = builder2.handler(Handler1.class).handler(Handler2.class).handler(Handler3.class).build().process(123, int.class);
        System.out.println("output: " + obj);
    }
}
