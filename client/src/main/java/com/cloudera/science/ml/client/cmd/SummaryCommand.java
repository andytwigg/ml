/**
 * Copyright (c) 2013, Cloudera, Inc. All Rights Reserved.
 *
 * Cloudera, Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * This software is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for
 * the specific language governing permissions and limitations under the
 * License.
 */
package com.cloudera.science.ml.client.cmd;

import java.io.IOException;
import java.util.List;

import org.apache.crunch.PCollection;
import org.apache.crunch.Pipeline;
import org.apache.hadoop.conf.Configuration;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;
import com.cloudera.science.ml.client.params.InputParameters;
import com.cloudera.science.ml.client.params.PipelineParameters;
import com.cloudera.science.ml.client.params.SummaryParameters;
import com.cloudera.science.ml.core.records.Record;
import com.cloudera.science.ml.core.records.Spec;
import com.cloudera.science.ml.core.records.Specs;
import com.cloudera.science.ml.parallel.summary.Summarizer;
import com.cloudera.science.ml.parallel.summary.Summary;
import com.google.common.collect.Lists;

@Parameters(commandDescription =
    "Summarize the continuous (and optionally, categorical) attributes of a collection of records")
public class SummaryCommand implements Command {

  @Parameter(names = "--header-file",
      description = "Local file that has info about each column in the input, one column per-line")
  private String headerFile;
  
  @Parameter(names = "--summary-file", required=true,
      description = "The name of the local file to store the JSON summary data to")
  private String summaryFile;

  @ParametersDelegate
  private InputParameters inputParams = new InputParameters();
  
  @ParametersDelegate
  private PipelineParameters pipelineParams = new PipelineParameters();
  
  @ParametersDelegate
  private SummaryParameters summaryParams = new SummaryParameters();
  
  @Override
  public int execute(Configuration conf) throws IOException {
    Pipeline p = pipelineParams.create(SummaryCommand.class, conf);
    PCollection<Record> records = inputParams.getRecords(p);

    Spec spec = null;
    List<Integer> symbolicColumns = Lists.newArrayList();
    List<Integer> ignoredColumns = Lists.newArrayList();
    if (headerFile != null) {
      spec = Specs.readFromHeaderFile(headerFile, ignoredColumns, symbolicColumns);
    }
    
    Summarizer summarizer = new Summarizer()
        .spec(spec)
        .defaultToSymbolic(false)
        .exceptionColumns(symbolicColumns)
        .ignoreColumns(ignoredColumns);
    Summary summary = summarizer.build(records).getValue();
    summaryParams.save(summary, summaryFile);

    p.done();
    return 0;
  }

  @Override
  public String getDescription() {
    return "Summarize the continuous (and optionally, categorical) attributes of a collection of records";
  }

}
