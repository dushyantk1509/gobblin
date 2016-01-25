/*
 * Copyright (C) 2014-2016 LinkedIn Corp. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the
 * License at  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.
 */
package gobblin.data.management.copy;

import gobblin.configuration.SourceState;
import gobblin.configuration.State;
import gobblin.configuration.WorkUnitState;
import gobblin.data.management.copy.extractor.CloseableFsFileAwareInputStreamExtractor;
import gobblin.source.extractor.Extractor;
import gobblin.source.extractor.extract.sftp.SftpLightWeightFileSystem;
import gobblin.util.HadoopUtils;

import java.io.IOException;

import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.FileSystem;

import com.google.common.io.Closer;


/**
 * Used instead of {@link CopySource} for {@link FileSystem}s that need be closed after use E.g
 * {@link SftpLightWeightFileSystem}.
 * <p>
 * Note that all {@link FileSystem} implementations should not be closed as Hadoop's
 * {@link FileSystem#get(org.apache.hadoop.conf.Configuration)} API returns a cached copy of {@link FileSystem} by
 * default. The same {@link FileSystem} instance may be used by other classes in the same JVM. Closing a cached
 * {@link FileSystem} may cause {@link IOException} at other parts of the code using the same instance.
 * </p>
 * <p>
 * For {@link SftpLightWeightFileSystem} a new instance is returned on every
 * {@link FileSystem#get(org.apache.hadoop.conf.Configuration)} call. Closing is necessary as the file system maintains
 * a session with the remote server.
 *
 * @see {@link HadoopUtils#newConfiguration()}
 * @See {@link SftpLightWeightFileSystem}
 *      </p>
 */
@Slf4j
public class CloseableFsCopySource extends CopySource {

  private final Closer closer = Closer.create();

  protected FileSystem getSourceFileSystem(State state) throws IOException {
    return closer.register(super.getSourceFileSystem(state));
  }

  @Override
  public void shutdown(SourceState state) {
    try {
      closer.close();
    } catch (IOException e) {
      log.warn("Failed to close all closeables", e);
    }
  }

  @Override
  public Extractor<String, FileAwareInputStream> getExtractor(WorkUnitState state) throws IOException {

    CopyableFile copyableFile = deserializeCopyableFile(state);

    return new CloseableFsFileAwareInputStreamExtractor(getSourceFileSystem(state), copyableFile);
  }

}
