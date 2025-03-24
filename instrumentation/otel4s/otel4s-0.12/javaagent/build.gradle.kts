plugins {
  id("otel.javaagent-instrumentation")
  id("otel.nullaway-conventions")
  id("otel.scala-conventions")
}

val otel4sVersion = "0.12-594844e-20250324T065122Z-SNAPSHOT"
val scalaVersion = "2.13"

muzzle {
  pass {
    group.set("org.typelevel")
    module.set("otel4s-oteljava-context-storage_2.13")
    versions.set("[$otel4sVersion,)")
    assertInverse.set(true)
  }
  pass {
    group.set("org.typelevel")
    module.set("otel4s-oteljava-context-storage_3")
    versions.set("[$otel4sVersion,)")
    assertInverse.set(true)
  }
}

dependencies {
  implementation(project(":instrumentation:cats-effect:cats-effect-common-3.6:javaagent"))

  // we need access to the "application.io.opentelemetry.context.Context"
  // to properly bridge fiber and agent context storages
  compileOnly(project(":opentelemetry-api-shaded-for-instrumenting", configuration = "shadow"))

  implementation(project(":instrumentation:opentelemetry-api:opentelemetry-api-1.0:javaagent"))

  // otel4s
  compileOnly("org.typelevel:otel4s-oteljava-context-storage_$scalaVersion:$otel4sVersion")

  testImplementation("org.typelevel:otel4s-oteljava-context-storage_$scalaVersion:$otel4sVersion")

  // bring cats-effect instrumentation
  testInstrumentation(project(":instrumentation:cats-effect:cats-effect-3.6:javaagent"))

  latestDepTestLibrary("org.typelevel:otel4s-oteljava-context-storage_$scalaVersion:latest.release")
}

tasks {
  withType<Test>().configureEach {
    jvmArgs("-Dio.opentelemetry.javaagent.shaded.io.opentelemetry.context.enableStrictContext=false")
    jvmArgs("-Dcats.effect.trackFiberContext=true")
  }
}
