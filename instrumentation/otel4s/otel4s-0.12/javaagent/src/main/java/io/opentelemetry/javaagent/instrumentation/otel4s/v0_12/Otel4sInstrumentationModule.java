/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.otel4s.v0_12;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.internal.ExperimentalInstrumentationModule;
import net.bytebuddy.matcher.ElementMatcher;

import java.util.Collections;
import java.util.List;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;

@AutoService(InstrumentationModule.class)
public class Otel4sInstrumentationModule extends InstrumentationModule
    implements ExperimentalInstrumentationModule {

  public Otel4sInstrumentationModule() {
    super("otel4s", "otel4s-0.12");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return Collections.singletonList(new Otel4sIOLocalContextStorageInstrumentation());
  }

  @Override
  public ElementMatcher.Junction<ClassLoader> classLoaderMatcher() {
    return hasClassesNamed("org.typelevel.otel4s.oteljava.context.IOLocalContextStorage$");
  }

  // ensure it's the last one
  @Override
  public int order() {
    return Integer.MAX_VALUE;
  }

  @Override
  public String getModuleGroup() {
    // This module uses the api context bridge helpers, therefore must be in the same classloader
    return "opentelemetry-api-bridge";
  }
}
