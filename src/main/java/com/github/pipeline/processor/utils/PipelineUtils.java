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
import static com.github.pipeline.processor.PipelineConstants.HANDLER_STRING;
import static com.github.pipeline.processor.PipelineConstants.INDEX_STRING;
import static com.github.pipeline.processor.PipelineConstants.NULL_ALLOWED_TYPE_REGEX;

import static com.google.common.base.Preconditions.checkNotNull;

import com.github.aap.processor.tools.ClassTypeParser;
import com.github.aap.processor.tools.domain.ClassType;
import com.github.aap.processor.tools.exceptions.TypeMismatchException;
import com.github.pipeline.processor.exceptions.NullNotAllowedException;
import com.github.pipeline.processor.exceptions.CheckTimeCacheException;
import com.github.pipeline.processor.exceptions.CheckTimeTypeMismatchException;
import com.github.pipeline.processor.PipelineHandler;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Static methods for use within this PipelineProcessor. 
 * 
 * @author cdancy
 */
@SuppressWarnings("PMD.TooManyStaticImports")
public class PipelineUtils {

    /**
     * Checks each PipelineHandler to its successor (or potentially cache) to 
     * ensure all types match as expected.
     * 
     * @param pipeline to check for type mis-matches.
     * @param globalResources potentially null map (though if not will be unmodifiable) 
     *        containing objects passed in by user to be used within pipeline.
     * @return Map where key is the index, and value is the ClassType, of
     *         further checks that need to be made at runtime.
     */
    public static Map<Integer, ClassType> checkTimeScan(final List<? extends PipelineHandler> pipeline, final Map<String, Object> globalResources) {
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

            // 1.) Various checks surrounding a handlers outputCacheKey.
            final ClassType currentClazzType = currentHandler.classType().firstSubTypeMatching(FUNCTION_REGEX);
            if (currentHandler.outputCacheKey() != null) {

                // 1.1) check local cache for object
                final CacheSet previousCacheSet = cacheSets.get(currentHandler.outputCacheKey());
                if (previousCacheSet == null) {
                    
                    // 1.2) check global resources for object
                    final Object possibleObj = (globalResources != null) 
                            ? globalResources.get(currentHandler.outputCacheKey())
                            : null;
                    if (possibleObj == null) {
                        cacheSets.put(currentHandler.outputCacheKey(), 
                                CacheSet.newInstance(currentHandler.id(), 
                                        i, currentClazzType.subTypeAtIndex(1)));
                    } else {
                        throw new CheckTimeCacheException(HANDLER_STRING 
                            + currentHandler.id() + INDEX_STRING + i + " " 
                            + "is attempting to set cache-key '" + currentHandler.outputCacheKey() + "' "
                            + "previously in use within globalResources");
                    }
                } else {
                    throw new CheckTimeCacheException(HANDLER_STRING 
                        + currentHandler.id() + INDEX_STRING + i + " " 
                        + "is attempting to set cache-key '" + currentHandler.outputCacheKey() + "' "
                        + "previously in use by Handler (" 
                        + previousCacheSet.name + INDEX_STRING + previousCacheSet.index);
                }
            }
            
            // 2.) Various checks surrounding the potential previous handler in relation to this handler.
            final PipelineHandler previousHandler = (i > 0) ? pipeline.get(i - 1) : null;
            ClassType previousSubType = null; // used for comparing against currentSubType
            String previousSubTypeSourceName = null; // used for filling out exception message
            if (previousHandler != null) {
                
                previousSubTypeSourceName = previousHandler.id();
                previousSubType = previousHandler.classType().firstSubTypeMatching(FUNCTION_REGEX).subTypeAtIndex(1);

                // Special case when the previous handler sets a cache and the
                // current handler accepts a NULL/VOID thus comparison need
                // not be performed and we can skip to next iteration of loop.
                if (previousHandler.outputCacheKey() != null 
                        && currentClazzType.subTypeAtIndex(0).toString().matches(NULL_ALLOWED_TYPE_REGEX)) {
                    continue;
                }

                // Special case to test when previous handler outputs a value, but is not cached
                // and is not void/null, but the current handler expects something from the cache.
                // This leads to a handler within the pipeline returning an output that is 
                // essentially gone to the ether with no one using it which is not something
                // we should permit.
                if (previousHandler.outputCacheKey() == null 
                        && currentHandler.inputCacheKey() != null
                        && !previousSubType.toString().matches(NULL_ALLOWED_TYPE_REGEX)) {
                    throw new CheckTimeCacheException(HANDLER_STRING 
                            + previousSubTypeSourceName + INDEX_STRING + (i - 1) + " " 
                            + "declares an output (" + previousSubType + ") not designated for cache while Handler (" 
                            + currentHandler.id() + INDEX_STRING + i 
                            + " expects an input from cache:" 
                            + " only objects of type Null/Void can be returned in this context");
                }
            }

            // 3.) Various checks surrounding a handler's inputCacheKey
            if (currentHandler.inputCacheKey() != null) {
                
                // 1.) check local cache for object
                CacheSet previousCacheSet = cacheSets.get(currentHandler.inputCacheKey());
                if (previousCacheSet == null) {
                    
                    // 2.) check global resources for object
                    final Object possibleObj = (globalResources != null) 
                            ? globalResources.get(currentHandler.inputCacheKey())
                            : null;
                    if (possibleObj != null) {
                        previousSubTypeSourceName = "globalResource";
                        previousCacheSet = CacheSet.newInstance(previousSubTypeSourceName, 
                                        -1, ClassTypeParser.parse(possibleObj));
                        cacheSets.put(currentHandler.inputCacheKey(), previousCacheSet);
                    }
                } else {
                    previousSubTypeSourceName = previousCacheSet.name;
                }
                
                if (previousCacheSet != null) {
                    previousCacheSet.numTimesUsed += 1;
                    previousSubType = previousCacheSet.classType;
                } else {
                    final Set<String> allSets = Sets.newHashSet(cacheSets.keySet());
                    if (globalResources != null) {
                        allSets.addAll(globalResources.keySet());
                    }
                    
                    throw new CheckTimeCacheException(HANDLER_STRING 
                        + currentHandler.id() + INDEX_STRING + i + " " 
                        + "failed attempting to get cache-key '" + currentHandler.inputCacheKey() + "', "
                        + "valid keys at this point are: " + allSets);
                }
            }

            // 4.) If applicable compare previous handler's output (could come from cache)
            //     to the current handler's input (could also have come from cache).
            if (previousSubType != null) {
                try {
                    final ClassType currentSubType = currentClazzType.subTypeAtIndex(0);
                    final int index = previousSubType.compare(currentSubType);
                    if (index == 1) {
                        runtimePipelineChecks.put(index, currentSubType);
                    }
                } catch (TypeMismatchException tme) {
                    throw new CheckTimeTypeMismatchException(HANDLER_STRING 
                            + previousSubTypeSourceName + INDEX_STRING + (i - 1) + " " 
                            + "outputs do not match Handler (" 
                            + currentHandler.id() + INDEX_STRING + i + " inputs.", tme);
                }
            }
        }
        
        // 5.) Final check to ensure all cache-sets are used at least once 
        //     and throw an exception if they are not.
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
