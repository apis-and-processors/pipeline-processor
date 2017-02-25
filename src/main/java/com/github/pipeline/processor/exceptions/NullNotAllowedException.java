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
 * Thrown when a null is either passed in or returned from a given Function
 * but is not allowed.
 * 
 * @author cdancy
 */
public class NullNotAllowedException extends RuntimeException {
    
    public NullNotAllowedException() {
        super();
    }
    
    public NullNotAllowedException(final String message) {
        super(message);
    }
    
    public NullNotAllowedException(final String message, final Throwable throwable) {
        super(message, throwable);
    }
    
    public NullNotAllowedException(final Throwable throwable) {
        super(throwable);
    }
}
