/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.pipeline.processor.utils;

import static com.github.pipeline.processor.PipelineConstants.FUNCTION_REGEX;

import com.github.aap.processor.tools.domain.ClassType;
import com.github.aap.processor.tools.exceptions.TypeMismatchException;
import com.github.pipeline.processor.PipelineHandler;
import com.github.pipeline.processor.exceptions.CheckTimeTypeMismatchException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Random static methods for use within this PipelineProcessor. 
 * 
 * @author cdancy
 */
public class PipelineUtils {

    /**
     * Checks each PipelineHandler to its successor to ensure all types 
     * match as expected.
     * 
     * @param pipeline to check for type mis-matches.
     * @return Map where key is the index, and value is the ClassType, that
     *         needs to be further checked at process time.
     * @throws CheckTimeTypeMismatchException if any 2 types could not 
     *         successfully be matched.
     */
    public static Map<Integer, ClassType> typeCheckPipeline(final List<? extends PipelineHandler> pipeline) {
        final Map<Integer, ClassType> runtimePipelineChecks = new HashMap<>();
        
        PipelineHandler previousHandler = null;
        ClassType previousClazzType = null;
        final int lastIndex = pipeline.size() - 1;
        for (int i = 0; i < pipeline.size(); i++) {
            final PipelineHandler currentHandler = pipeline.get(i);
            if (previousHandler == null) {
                previousHandler = currentHandler;
                previousClazzType = previousHandler.classType().firstSubTypeMatching(FUNCTION_REGEX); 
                continue;
            } 

            final ClassType currentClazzType = currentHandler.classType().firstSubTypeMatching(FUNCTION_REGEX);
            try {
                final int index = previousClazzType.subTypeAtIndex(1).compare(currentClazzType.subTypeAtIndex(0));
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
