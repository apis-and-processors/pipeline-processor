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

package com.github.pipeline.processor.exceptions;

/**
 * Meant to be thrown during "check time" (i.e. when pipeline is built and BEFORE execution) 
 * when more than one @Cache annotation with the same key is present for the output of a 
 * PipelineHandler OR when no key exists for the input to a given PipelineHandler.
 * 
 * @author cdancy
 */
public class CheckTimeCacheException extends RuntimeException {
    
    public CheckTimeCacheException() {
        super();
    }
    
    public CheckTimeCacheException(final String message) {
        super(message);
    }
    
    public CheckTimeCacheException(final String message, final Throwable throwable) {
        super(message, throwable);
    }
    
    public CheckTimeCacheException(final Throwable throwable) {
        super(throwable);
    }
}
