[![Build Status](https://travis-ci.org/apis-and-processors/pipeline-processor.svg?branch=master)](https://travis-ci.org/apis-and-processors/pipeline-processor)
[![Download](https://api.bintray.com/packages/apis-and-processors/java-libraries/pipeline-processor/images/download.svg) ](https://bintray.com/apis-and-processors/java-libraries/pipeline-processor/_latestVersion)

# pipeline-processor
Type checked pipeline for processing java Functions

## Latest release

Can be sourced from jcenter like so:

    <dependency>
      <groupId>com.github.aap</groupId>
      <artifactId>pipeline-processor</artifactId>
      <version>0.0.1</version>
      <classifier>sources|tests|javadoc|all</classifier> (Optional)
    </dependency>
    
## Motivation

While designing the [api-processor](https://github.com/apis-and-processors/api-processor) it became apparent that invoking a given Api 
function was analagous to processing a pipeline (i.e. the output of one function is the input to the next). With that in mind the need 
for a generic way to process java Functions in a pipeline fashion was needed.

## On what "type checked" pipeline means

As there is no sane and programattic way to tell whether any 2 handlers, at compile time, have matching output-to-input types we have 
to wait until runtime to do so. PipelineProcessor has 2 passes it makes to determine Type sanity. 

The first happens when the PipelineProcessor is built. We check the outputs of each handler to the inputs of the subsequent handler 
(i.e. java.lang.Boolean into java.lang.Integer) to ensure all Types match as expected. This is referred to as the __check time__ phase. 
If there is a mismatch, like in the aforementioned java.lang.Boolean having to be the input into a handler which accepts only java.lang.Integer, 
then a `CheckTimeTypeMistmatchException` will be thrown denoting which Types don't match and at what index in the pipeline the exception occurred. 
Any ambiguous inputs/outputs (e.g. java.lang.Object) are further checked in the next phase.

The second check happens at execution time, meaning when the user calls `output`, and is referred to as the __process time__ phase. 
This phase kicks ONLY if there are any ambiguous outputs or inputs between handlers (i.e. java.lang.Object into java.lang.Integer) 
that were leftover from the __check time__ phase. If there is a mismatch, meaning the aforementioned java.lang.Object actually resolves 
to java.util.ArrayList, then a `ProcessTimeTypeMistmatchException` will be thrown denoting which Types don't match and at what index 
in the pipeline the exception occurred.

## Setup and How to use

PipelineProcessor's are created using the Builder pattern and executed by calling `output`:

    PipelineProcessor processor = PipelineProcessor.builder()
        .handler(MyFunction1.class)
        .handler(new MyFunction2())
        .handler(MyFunction3.class).build();
        
    Object obj = processor.output();

PipelineProcessor's are meant to be executed over and over again. As such subsequent calls to `output` are OK and are considered to be thread-safe. 

You can optionally give an input to the first Function within the processor (assuming Types match):

    Object obj = processor.output(123);

## On RetryPolicy

As the pipeline itself is executed with [failsafe](https://github.com/jhalterman/failsafe) you can optionally pass in a `RetryPolicy` to the Builder:

    PipelineProcessor processor = PipelineProcessor.builder()
        .handler(MyFunction1.class)
        .handler(MyFunction2.class)
        .handler(MyFunction3.class)
        .retryPolicy(new RetryPolicy().withMaxRetries(5)).build();
	
It should be noted that retries are done on the entirety of the PipelineProcessor itself and not for any singular handler. 
With the above example in mind we defined a `RetryPolicy` with 5 retries. If the first handler fails and it takes 2 retries to 
get it to work you can consider the `max retries` to be decremented by 2. If the second handler fails and it takes 1 retry to 
get it to work you can then decrement `max retries` by 1. If the third handler fails we now have at most 2 retries to get it 
to work before the entire PipelineProcessor fails.
    
## On reactive-streams

The PipelineProcessor.Builder acts as, and implements, the [Publisher](https://github.com/reactive-streams/reactive-streams-jvm#1-publisher-code) 
interface thus allowing you to submit N number of [Subscribers](https://github.com/reactive-streams/reactive-streams-jvm#2-subscriber-code).
However, and because the `subscribe` method returns `void` and does not correspond to the Builder pattern, we provide a `subscriber` method, 
which internally defers to the `subscribe` method, to return the Builder object. 

    PipelineProcessor processor = PipelineProcessor.builder()
        .handler(MyFunction1.class)
        .subscriber(new MySubscriber())
        .subscriber(new MySubscriber2())
        .subscriber(MySubscriber3.class).build();

## Documentation

javadocs can be found via [github pages here](https://apis-and-processors.github.io/pipeline-processor/docs/javadoc/)

## Examples

The [various tests](https://github.com/apis-and-processors/pipeline-processor/tree/master/src/test/java/com/github/pipeline/processor) 
provide many examples that you can use in your own code.
    
## Testing

Running tests can be done like so:

    ./gradlew clean build
	
# Additional Resources

* [processor-tools](https://github.com/apis-and-processors/processor-tools)
* [Guava](https://github.com/google/guava/wiki)
* [failsafe](https://github.com/jhalterman/failsafe)
* [reactive-streams](https://github.com/reactive-streams/reactive-streams-jvm)
