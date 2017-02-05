/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.pipeline.processor;

import com.github.type.utils.domain.Null;
import com.google.common.base.Function;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.testng.annotations.Test;

/**
 *
 * @author cdancy
 */
public class PipelineProcessorTest {
   
    class Handler1 implements java.util.function.Function<String, Object> {
        
        @Override
        @Nullable
        public Object apply(String object) {
            System.out.println("Input: " + object);
            return null;
        }
    }
    
    class Handler2 implements Function<Null, Boolean> {
        @Override
        public Boolean apply(Null object) {
            System.out.println("Input: " + object);
            return Boolean.FALSE;
        }
    }
        
    class Handler3 implements Comparable, Function<Boolean, String> {
        @Override
        public String apply(Boolean object) {
            System.out.println("Input: " + object);
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
        
        PipelineProcessor processor = builder2.handler(Handler1.class).handler(Handler2.class).handler(Handler3.class).outputType(String.class).build();
        Object objInput = "Hello World";
        Object obj = processor.input(objInput).output();

        System.out.println("output: " + obj);
    }
}
