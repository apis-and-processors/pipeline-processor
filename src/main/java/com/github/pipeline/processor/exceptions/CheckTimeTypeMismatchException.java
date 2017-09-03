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
 * when the outputs of one handler don't match the inputs of the successive handler.
 * 
 * @author cdancy
 */
public class CheckTimeTypeMismatchException extends RuntimeException {
    
    public CheckTimeTypeMismatchException() {
        super();
    }
    
    public CheckTimeTypeMismatchException(final String message) {
        super(message);
    }
    
    public CheckTimeTypeMismatchException(final String message, final Throwable throwable) {
        super(message, throwable);
    }
    
    public CheckTimeTypeMismatchException(final Throwable throwable) {
        super(throwable);
    }
}
