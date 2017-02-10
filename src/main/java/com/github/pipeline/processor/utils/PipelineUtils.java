/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.pipeline.processor.utils;

import static com.github.pipeline.processor.PipelineConstants.FUNCTION_REGEX;
import com.github.pipeline.processor.PipelineHandler;
import com.github.pipeline.processor.exceptions.CheckTimeTypeMismatchException;
import com.github.aap.type.utils.ClassType;
import com.github.aap.type.utils.exceptions.TypeMismatchException;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;

/**
 *
 * @author cdancy
 */
public class PipelineUtils {
        
    public static Map<Integer, ClassType> typeCheckPipeline(List<? extends PipelineHandler> pipeline) {
        Map<Integer, ClassType> runtimePipelineChecks = Maps.newHashMap();
        
        PipelineHandler previousHandler = null;
        ClassType previousClazzType = null;
        int lastIndex = pipeline.size() -1;
        for(int i = 0; i < pipeline.size(); i++) {
            PipelineHandler currentHandler = pipeline.get(i);
            if (previousHandler == null) {
                previousHandler = currentHandler;
                previousClazzType = previousHandler.classType().firstSubTypeMatching(FUNCTION_REGEX); 
                continue;
            } 

            ClassType currentClazzType = currentHandler.classType().firstSubTypeMatching(FUNCTION_REGEX);
            try {
                int index = previousClazzType.subTypeAtIndex(1).compare(currentClazzType.subTypeAtIndex(0));
                if (index == 1) {
                    runtimePipelineChecks.put(index, currentClazzType.subTypeAtIndex(0));
                }
            } catch (TypeMismatchException tme) {
                throw new CheckTimeTypeMismatchException("Handler (" 
                        + previousHandler.getClass().getName() + ") at index " + i + " " 
                        + "outputs do not match Handler (" 
                        + currentHandler.getClass().getName() + ") inputs.", tme);
            } 

            if (i != lastIndex) {
                previousClazzType = currentClazzType;
                previousHandler = currentHandler;
            }
        }
        
        return runtimePipelineChecks;
    }
}
