/*
 * Copyright (c) 2014, Cloudera, Inc. All Rights Reserved.
 *
 * Cloudera, Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * This software is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for
 * the specific language governing permissions and limitations under the
 * License.
 */

package com.cloudera.dataflow.spark;

import com.google.cloud.dataflow.sdk.Pipeline;
import com.google.cloud.dataflow.sdk.runners.PipelineRunner;
import com.google.cloud.dataflow.sdk.runners.TransformTreeNode;
import com.google.cloud.dataflow.sdk.transforms.PTransform;
import com.google.cloud.dataflow.sdk.values.PValue;
import org.apache.spark.api.java.JavaSparkContext;

import java.util.logging.Logger;

/**
 * The SparkPipelineRunner translate operations defined on a pipeline to a representation
 * executable by Spark, and then submitting the job to Spark to be executed. If we wanted to run
 * a dataflow pipeline in Spark's local mode with two threads, we would do the following:
 *    Pipeline p = <logic for pipeline creation >
 *    EvaluationResult result = new SparkPipelineRunner("local[2]").run(p);
 */
public class SparkPipelineRunner extends PipelineRunner<EvaluationResult> {

  private static final Logger LOG =
      Logger.getLogger(SparkPipelineRunner.class.getName());
  private SparkPipelineOptions mOptions;

  /**
   * Creates and returns a new SparkPipelineRunner with specified options.
   * @param options The SparkPipelineOptions to use when executing the job.
   * @return
   */
  public static SparkPipelineRunner create(SparkPipelineOptions options) {
    return new SparkPipelineRunner(options);
  }


  /**
   * No parameter constructor defaults to running this pipeline in Spark's local mode, in a single
   * thread.
   */
  private SparkPipelineRunner(SparkPipelineOptions options) {
    mOptions = options;
  }


  @Override
  public EvaluationResult run(Pipeline pipeline) {
    JavaSparkContext jsc = getContext();
    EvaluationContext ctxt = new EvaluationContext(jsc, pipeline);
    pipeline.traverseTopologically(new Evaluator(ctxt));
    return ctxt;
  }

  private JavaSparkContext getContext() {
    return new JavaSparkContext(mOptions.getSparkMaster(), mOptions.getJobName());
  }

  private static class Evaluator implements Pipeline.PipelineVisitor {

    private final EvaluationContext ctxt;

    private Evaluator(EvaluationContext ctxt) {
      this.ctxt = ctxt;
    }

    @Override
    public void enterCompositeTransform(TransformTreeNode node) {
    }

    @Override
    public void leaveCompositeTransform(TransformTreeNode node) {
    }

    @Override
    public void visitTransform(TransformTreeNode node) {
      PTransform<?, ?> transform = node.getTransform();
      TransformEvaluator evaluator = TransformTranslator.getTransformEvaluator(transform.getClass());
      LOG.info("Evaluating " + transform);
      evaluator.evaluate(transform, ctxt);
    }

    @Override
    public void visitValue(PValue pvalue, TransformTreeNode node) {
    }
  }
}

