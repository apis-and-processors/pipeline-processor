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

package com.github.pipeline.processor;

import com.github.pipeline.processor.exceptions.KeyAlreadyExistsException;
import com.github.pipeline.processor.exceptions.NullNotAllowedException;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * A bare-bones pseudo-map implementation that allows one to read from
 * potentially multiple backing maps but ONLY write to the localResources.
 * Once a key is written it can't be over-written. One first would have
 * to call the 'clear' method and then write again to get similar functionality.
 */
public class PipelineResources {

    private final Map<String, Object> globalResources;
    
    // we lazily init this Map as a given pipeline may never use
    // the Cache annotation and thus it
    private volatile Map<String, Object> localResources;
    
    /**
     * Create PipelineResources with a potentially null globalResources
     * backing Map. If non-null we ONLY read from the globalResources
     * and never write of any kind.
     * 
     * @param globalResources potentially shared Map.
     */
    public PipelineResources(@Nullable final Map globalResources) {
        this.globalResources = globalResources;
    }

    /**
     * Get a value from these resources. First we will query the
     * local resources map and if not present then query the global
     * resources map.
     * 
     * @param key to query maps for potential value.
     * @return potential value or null if not found.
     */
    public synchronized Object get(final String key) {
        Object obj = localResources != null ? localResources.get(key) : null;
        if (obj == null && globalResources != null) {
            obj = globalResources.get(key);
        }
        return obj;
    }

    /**
     * Add this key, and its potential value, to this resource ensuring first
     * that it does not exist.
     * 
     * @param key to reference value with.
     * @param value potential value to coincide with key lookup.
     */
    public synchronized void put(final String key, final Object value) {

        // we don't allow null keys in this map as it quite
        // literally doesn't make sense in the context of
        // the pipeline-processor.
        if (key == null) {
            throw new NullNotAllowedException("Cannot add to resources with NULL key");    
        }
        
        // 1.) check if key is present in our globalResources (if applicable).
        if (globalResources != null && globalResources.containsKey(key)) {
            throw new KeyAlreadyExistsException("Key " + key + " is already present and can't be added again.");
        }
        
        // 2.) check if key is present in our localResources (if applicable) and if
        // not then create local map and add it.
        if (localResources != null) {
            if (localResources.containsKey(key)) {
                throw new KeyAlreadyExistsException("Key " + key + " is already present and can't be added again.");
            }
        } else {
            localResources = Maps.newHashMap();
        }
        
        localResources.put(key, value);
    }

    /**
     * Combined keySet of all backing resources.
     * 
     * @return keySet of all backing resources. 
     */
    public synchronized Set<Object> keySet() {
        final Set<Object> keys = Sets.newHashSet();
        
        if (localResources != null) {
            keys.addAll(localResources.keySet());
        }
        
        if (globalResources != null) {
            keys.addAll(globalResources.keySet());
        }
        
        return keys;
    }
    
    /**
     * The combined size of all backing resources.
     * 
     * @return size of all backing resources.
     */
    public synchronized int size() {
        int size = 0;
        
        if (localResources != null) {
            size += localResources.size();
        }
        
        if (globalResources != null) {
            size += globalResources.size();
        }
        
        return size;
    }
    
    /**
     * Combined entrySet of all backing resources.
     * 
     * @return entrySet of all backing resources.
     */
    public synchronized Set<Entry<String, Object>> entrySet() {
        final Set<Entry<String, Object>> combinedEntrySets = Sets.newHashSet();
        
        if (localResources != null) {
            combinedEntrySets.addAll(localResources.entrySet());
        }
        
        if (globalResources != null) {
            combinedEntrySets.addAll(globalResources.entrySet());
        }
        
        return combinedEntrySets;
    }
    
    /**
     * Clear the localResources (if it exists) only. We never
     * touch the globalResources as that is potentially shared
     * by N number of pipeline-processors.
     */
    public synchronized void clear() {
        if (localResources != null) {
            localResources.clear();
        }
    }
}
