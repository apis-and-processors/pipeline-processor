/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.pipeline.processor.utils;

import com.github.pipeline.processor.PipelineHandler;
import com.github.pipeline.processor.exceptions.CheckTimeTypeMismatchException;
import com.github.type.utils.ClassType;
import com.github.type.utils.TypeUtils;
import com.github.type.utils.exceptions.TypeMismatchException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.List;

/**
 *
 * @author cdancy
 */
public class PipelineUtils {
    
    public static List<Integer> typeCheckPipeline(ImmutableList<? extends PipelineHandler> pipeline, Object initialInput, Object expectedOutputType) {
        List<Integer> executionTimeIndexChecks = Lists.newArrayList();
        
        PipelineHandler previousHandler = null;
        ClassType previousClazzType = null;
        int lastIndex = pipeline.size() -1;
        for(int i = 0; i < pipeline.size(); i++) {
            PipelineHandler currentHandler = pipeline.get(i);
            if (previousHandler == null) {
                previousHandler = currentHandler;
                previousClazzType = TypeUtils.parseClassType(previousHandler.function()).firstSubTypeMatching(".*Function"); 
                if (initialInput != null) {
                    ClassType inputClazzType = TypeUtils.parseClassType(initialInput);
                    
                    try {
                        int index = inputClazzType.compare(previousClazzType.subTypeAtIndex(0));
                        if (index > 0) {
                            executionTimeIndexChecks.add(index);
                        }
                    } catch (TypeMismatchException tme) {
                        throw new CheckTimeTypeMismatchException("Handler (InitialInput to PipelineProcessor) " 
                                + "outputs do not match Handler (" 
                                + previousHandler.getClass().getName() + ") inputs.", tme);
                    } 
                }
                continue;
            } 

            ClassType currentClazzType = TypeUtils.parseClassType(currentHandler.function()).firstSubTypeMatching(".*Function");
            try {
                int index = previousClazzType.subTypeAtIndex(1).compare(currentClazzType.subTypeAtIndex(0));
                if (index > 0) {
                    executionTimeIndexChecks.add(index);
                }
            } catch (TypeMismatchException tme) {
                throw new CheckTimeTypeMismatchException("Handler (" 
                        + previousHandler.getClass().getName() + ") " 
                        + "outputs do not match Handler (" 
                        + currentHandler.getClass().getName() + ") inputs.", tme);
            } 

            if (i != lastIndex) {
                previousClazzType = currentClazzType;
                previousHandler = currentHandler;
            } else {
                if (expectedOutputType != null) {
                    ClassType outputClazzType = TypeUtils.parseClassType(expectedOutputType);
                    try {
                        int index = currentClazzType.subTypeAtIndex(1).compare(outputClazzType);
                        if (index > 0) {
                            executionTimeIndexChecks.add(index);
                        }
                    } catch (TypeMismatchException tme) {
                        throw new CheckTimeTypeMismatchException("Handler (" 
                                + currentHandler.getClass().getName() + ") " 
                                + "outputs do not match Handler (ExpectedOutput of PipelineProcessor) outputs.", tme);
                    } 
                }
            }
        }
        
        return executionTimeIndexChecks;
    }
}
