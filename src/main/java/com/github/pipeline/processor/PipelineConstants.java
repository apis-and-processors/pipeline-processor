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

/**
 * Constants used globally within the PipelineProcessor.
 * 
 * @author cdancy
 */
public class PipelineConstants {
   
    public static final String FUNCTION_REGEX = "^java.util.function.Function$";
    
    public static final String NULL_ALLOWED_TYPE_REGEX = "^(com.github.aap.processor.tools.domain.Null|java.lang.Void)$";

    // to satisfy PMD
    public static final String INDEX_STRING = ") at index ";
    
    // to satisfy PMD
    public static final String HANDLER_STRING = "Handler (";

    private PipelineConstants() {
        throw new UnsupportedOperationException("intentionally unimplemented");
    }
}
