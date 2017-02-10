/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.pipeline.processor;

/**
 * Constants used globally within the PipelineProcessor
 * 
 * @author cdancy
 */
public class PipelineConstants {
   
    public static final String FUNCTION_REGEX = "^(com.google.common.base.|java.util.function.)Function$";

    private PipelineConstants() {
        throw new UnsupportedOperationException("intentionally unimplemented");
    }
}
