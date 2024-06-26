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
package org.apache.servicecomb.fence.observability;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

import org.apache.servicecomb.config.BootStrapProperties;
import org.apache.servicecomb.fence.api.observability.ObservabilityService;
import org.apache.servicecomb.fence.api.observability.SearchLogResponse;
import org.apache.servicecomb.fence.api.observability.SearchTraceResponse;
import org.apache.servicecomb.fence.api.observability.Trace;
import org.apache.servicecomb.foundation.common.net.NetUtils;
import org.apache.servicecomb.foundation.common.part.FilePart;
import org.apache.servicecomb.provider.rest.common.RestSchema;
import org.apache.servicecomb.registry.RegistrationId;
import org.apache.servicecomb.swagger.invocation.exception.InvocationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.Part;
import jakarta.ws.rs.core.Response.Status;

@RestSchema(schemaId = ObservabilityService.NAME, schemaInterface = ObservabilityService.class)
public class ObservabilityEndpoint implements ObservabilityService {
  private static final Logger LOGGER = LoggerFactory.getLogger(ObservabilityEndpoint.class);

  private static final String FILE_SUFFIX = ".log";

  private static final String TRACE_FILE_PREFIX = "trace";

  private static final String LOG_FILE_PREFIX = "root";

  private static final String METRICS_FILE_PREFIX = "metrics";

  public static final ObjectMapper OBJ_MAPPER = new ObjectMapper();

  private final File logPath;

  private final Environment environment;

  private final RegistrationId registrationId;

  @Autowired
  public ObservabilityEndpoint(Environment environment, RegistrationId registrationId) {
    this.environment = environment;
    this.registrationId = registrationId;
    String path = environment.getProperty("servicecomb.observability.logPath", "./logs/"
        + BootStrapProperties.readServiceName(environment));
    LOGGER.info("observability log path is {}", path);
    logPath = new File(path);
  }

  @Override
  public SearchTraceResponse searchTrace(String timestamp, String traceId) {
    File target = findFile(TRACE_FILE_PREFIX, timestamp);
    if (target == null) {
      throw new InvocationException(Status.BAD_REQUEST, "not found");
    }
    List<String> lines = searchLines(target, buildSearchTraceId(traceId));
    Collections.reverse(lines);
    SearchTraceResponse response = new SearchTraceResponse();
    response.setApplication(BootStrapProperties.readApplication(environment));
    response.setServiceName(BootStrapProperties.readServiceName(environment));
    response.setLocalhost(NetUtils.getHostAddress());
    response.setInstanceId(registrationId.getInstanceId());
    List<Trace> data = new ArrayList<>(lines.size());
    lines.forEach(e -> {
      try {
        data.add(OBJ_MAPPER.readValue(e.substring(e.indexOf("{")), Trace.class));
      } catch (JsonProcessingException ex) {
        throw new InvocationException(Status.INTERNAL_SERVER_ERROR, "invalid trace configuration");
      }
    });
    response.setData(data);
    return response;
  }

  private String buildSearchTraceId(String traceId) {
    return "\"traceId\":\"" + traceId + "\"";
  }

  private List<String> searchLines(File target, String traceId) {
    List<String> result = new ArrayList<>();
    try (Scanner scanner = new Scanner(target)) {
      while (scanner.hasNextLine()) {
        String search = scanner.nextLine();
        if (search.contains(traceId)) {
          result.add(search);
        }
      }
    } catch (FileNotFoundException e) {
      LOGGER.error("search file error", e);
    }
    return result;
  }

  @Override
  public SearchLogResponse searchLog(String timestamp, String traceId) {
    File target = findFile(LOG_FILE_PREFIX, timestamp);
    if (target == null) {
      throw new InvocationException(Status.BAD_REQUEST, "not found");
    }
    List<String> lines = searchLines(target, buildSearchLogId(traceId));
    SearchLogResponse response = new SearchLogResponse();
    response.setApplication(BootStrapProperties.readApplication(environment));
    response.setServiceName(BootStrapProperties.readServiceName(environment));
    response.setInstanceId(registrationId.getInstanceId());
    response.setLocalhost(NetUtils.getHostAddress());
    response.setData(lines);
    return response;
  }

  private String buildSearchLogId(String traceId) {
    return "[" + traceId + "]";
  }

  @Override
  public Part downloadLog(String timestamp) {
    File target = findFile(LOG_FILE_PREFIX, timestamp);
    if (target == null) {
      throw new InvocationException(Status.BAD_REQUEST, "not found");
    }
    return new FilePart(target.getName(), target);
  }

  @Override
  public Part downloadMetrics(String timestamp) {
    File target = findFile(METRICS_FILE_PREFIX, timestamp);
    if (target == null) {
      throw new InvocationException(Status.BAD_REQUEST, "not found");
    }
    return new FilePart(target.getName(), target);
  }

  private File findFile(String prefix, String timestamp) {
    File[] targetFiles = logPath.listFiles(
        file -> file.getName().startsWith(prefix) && file.getName().endsWith(FILE_SUFFIX));
    if (targetFiles == null) {
      return null;
    }
    Arrays.sort(targetFiles, Comparator.comparing(File::getName));

    LocalDateTime time = LocalDateTime.now();
    try {
      time = LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_DATE_TIME);
    } catch (DateTimeParseException e) {
      LOGGER.error("time format error, using current time. {}. ", e.getMessage());
    }
    for (File target : targetFiles) {
      if (prefix.length() + FILE_SUFFIX.length() + 1 >= target.getName().length()) {
        return target;
      }
      String fileTime = target.getName().substring(prefix.length() + 1,
          target.getName().length() - FILE_SUFFIX.length());
      LocalDateTime temp = LocalDateTime.parse(fileTime, DateTimeFormatter.ofPattern("yyyy-MM-dd-HH"));
      if (temp.plus(1, ChronoUnit.HOURS).isAfter(time)) {
        return target;
      }
    }
    return null;
  }
}
