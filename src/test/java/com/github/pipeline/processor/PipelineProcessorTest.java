/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.pipeline.processor;

import com.github.aap.type.utils.TypeUtils;
import com.github.aap.type.utils.domain.Null;
import com.google.common.base.Function;
import com.google.common.reflect.TypeToken;
import java.util.Optional;
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
    
    class Handler2 implements Function<Null, Optional<Boolean>> {
        @Override
        public Optional<Boolean> apply(Null object) {
            System.out.println("Input: " + object);
            return Optional.empty();
        }
    }
        
    class Handler3 implements Comparable, Function<Optional<Boolean>, String> {
        @Override
        @Nullable
        public String apply(Optional<Boolean> object) {
            System.out.println("Input: " + object);
            return null;
        }

        @Override
        public int compareTo(Object o) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }
        
    @Test
    public void testSomeLibraryMethod() {
            
        PipelineProcessor.Builder<String, String> builder2 = PipelineProcessor.<String, String> builder();
        
        
        Optional<String> fish = Optional.<String>empty();
        System.out.println(TypeToken.of(fish.getClass().getGenericSuperclass()));
        PipelineProcessor<String, String> processor = builder2.handler(Handler1.class).handler(Handler2.class).handler(Handler3.class).build();
        
        
        Optional<String> obj = processor.input("bears").output();

        System.out.println("output: " + obj.isPresent());
    }
}
