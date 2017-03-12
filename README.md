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

While designing the [api-processor](https://github.com/apis-and-processors/api-processor) it became apparent that invoking a given Api function was analagous to processing a pipeline (i.e. the output of one function is the input to the next). With that in mind the need for a generic way to process java Functions in a pipeline fashion was needed.

## Setup and How to use

PipelineProcessors are created using a Builder and executed by calling `output`:

    PipelineProcessor processor = PipelineProcessor.builder()
        .handler(MyFunction1.class)
        .handler(new MyFunction2())
        .handler(MyFunction3.class).build();
        
    Object obj = processor.output();

You can optionally give an input to the first Function within the processor (assuming Types match):

    Object obj = processor.output(123);
    
## Documentation

javadocs can be found via [github pages here](https://apis-and-processors.github.io/pipeline-processor/docs/javadoc/)

## Examples

The [various tests](https://github.com/apis-and-processors/pipeline-processor/tree/master/src/test/java/com/github/pipeline/processor) provide many examples that you can use in your own code.
    
## Testing

Running tests can be done like so:

    ./gradlew clean build
	
# Additional Resources

* [Guava](https://github.com/google/guava/wiki)
* [processor-tools](https://github.com/apis-and-processors/processor-tools)
