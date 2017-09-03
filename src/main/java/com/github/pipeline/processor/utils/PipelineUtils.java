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
import static com.github.pipeline.processor.PipelineConstants.INDEX_STRING;
import static com.github.pipeline.processor.PipelineConstants.NULL_ALLOWED_TYPE_REGEX;

import static com.google.common.base.Preconditions.checkNotNull;

import com.github.aap.processor.tools.domain.ClassType;
import com.github.aap.processor.tools.exceptions.TypeMismatchException;
import com.github.pipeline.processor.exceptions.NullNotAllowedException;
import com.github.pipeline.processor.exceptions.CheckTimeCacheException;
import com.github.pipeline.processor.exceptions.CheckTimeTypeMismatchException;
import com.github.pipeline.processor.PipelineHandler;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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
     * @return Map where key is the index, and value is the ClassType, of
     *         further checks that need to be made at runtime.
     * @throws CheckTimeTypeMismatchException if any 2 types could not 
     *         successfully be matched.
     */
    public static Map<Integer, ClassType> typeCheckPipeline(final List<? extends PipelineHandler> pipeline) {
        checkNotNull(pipeline, "Cannot pass null pipeline");
        
        // if no pipeline handlers then return empty map.
        final Map<Integer, ClassType> runtimePipelineChecks = Maps.newHashMap();
        if (pipeline.isEmpty()) {
            return runtimePipelineChecks;
        }
        
        final Map<String, CacheSet> cacheSets = Maps.newHashMap();
        for (int i = 0; i < pipeline.size(); i++) {
            final PipelineHandler currentHandler = pipeline.get(i);
                        
            // nulls not allowed in pipeline
            if (currentHandler == null) {
                throw new NullNotAllowedException("Found NULL PipelineHandler at index " + i);
            }

            // check if current handler has an output cache-key and ensure
            // it's unique before continuing.
            final ClassType currentClazzType = currentHandler.classType().firstSubTypeMatching(FUNCTION_REGEX);
            if (currentHandler.outputCacheKey() != null) {

                final CacheSet previousCacheSet = cacheSets.get(currentHandler.outputCacheKey());
                if (previousCacheSet == null) {
                    cacheSets.put(currentHandler.outputCacheKey(), 
                            CacheSet.newInstance(currentHandler.getClass().getName(), 
                                    i, currentClazzType.subTypeAtIndex(1)));
                } else {
                    throw new CheckTimeCacheException("Handler (" 
                        + currentHandler.getClass().getName() + INDEX_STRING + i + " " 
                        + "is attempting to set cache-key '" + currentHandler.outputCacheKey() + "' "
                        + "previously in use by Handler (" 
                        + previousCacheSet.name + INDEX_STRING + previousCacheSet.index);
                }
            }
            
            // we can only compare to a previous handler so continue
            // only if we are past the first index.
            if (i > 0) {
                
                final PipelineHandler previousHandler = pipeline.get(i - 1);

                // special case when the previous handler sets a cache and the
                // current handler accepts a NULL/VOID thus comparison need
                // not be performed and we can skip to next iteration of loop.
                if (previousHandler.outputCacheKey() != null 
                        && currentClazzType.subTypeAtIndex(0).toString().matches(NULL_ALLOWED_TYPE_REGEX)) {
                    continue;
                }
                
                ClassType previousSubType = null;
                if (currentHandler.inputCacheKey() != null) {
                    final CacheSet previousCacheSet = cacheSets.get(currentHandler.inputCacheKey());
                    if (previousCacheSet != null) {
                        previousCacheSet.numTimesUsed += 1;
                        previousSubType = previousCacheSet.classType;
                    } else {
                        throw new CheckTimeCacheException("Handler (" 
                            + currentHandler.getClass().getName() + INDEX_STRING + i + " " 
                            + "failed attempting to get cache-key '" + currentHandler.inputCacheKey() + "', "
                            + "valid keys at this point are: " + cacheSets.keySet());
                    }
                } else {
                    previousSubType = previousHandler.classType().firstSubTypeMatching(FUNCTION_REGEX).subTypeAtIndex(1);
                }

                // do the actual comparison of previous the handler's
                // output to the current handler's inputs.
                try {
                    final ClassType currentSubType = currentClazzType.subTypeAtIndex(0);
                    final int index = previousSubType.compare(currentSubType);
                    if (index == 1) {
                        runtimePipelineChecks.put(index, currentSubType);
                    }
                } catch (TypeMismatchException tme) {
                    throw new CheckTimeTypeMismatchException("Handler (" 
                            + previousHandler.getClass().getName() + INDEX_STRING + (i - 1) + " " 
                            + "outputs do not match Handler (" 
                            + currentHandler.getClass().getName() + INDEX_STRING + i + " inputs.", tme);
                }
            }
        }
        
        // ensure all cache-sets are numTimesUsed and throw exception if at least 1 is not.
        final List<String> unusedCacheKeys = Lists.newArrayListWithCapacity(cacheSets.size());
        for (final Map.Entry<String, CacheSet> entry : cacheSets.entrySet()) {
            if (entry.getValue().numTimesUsed == 0) {
                unusedCacheKeys.add(entry.getKey());
            }
        }
        if (unusedCacheKeys.size() > 0) {
            throw new CheckTimeCacheException("The following cache-keys were not used within pipeline: "
                    + String.join(",", unusedCacheKeys));
        }
        
        return runtimePipelineChecks;
    }
    
    /**
     * Data structure to hold relevant information about a given
     * value that was added to cache during "check time".
     */
    private static class CacheSet {
        public final String name;
        public final int index;
        public final ClassType classType;
        public int numTimesUsed;
        
        private CacheSet(final String name, 
                final int index, 
                final ClassType classType) {
            this.name = name;
            this.index = index;
            this.classType = classType;
        }
        
        public static CacheSet newInstance(final String name, 
                final int index, 
                final ClassType classType) {
            return new CacheSet(name, index, classType);
        }
    }
}
