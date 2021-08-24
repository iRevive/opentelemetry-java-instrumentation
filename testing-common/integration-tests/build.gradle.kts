plugins {
  id("otel.javaagent-testing")
}

dependencies {
  implementation(project(":testing-common:library-for-integration-tests"))

  testCompileOnly(project(":instrumentation-api"))
  testCompileOnly(project(":javaagent-instrumentation-api"))
  testCompileOnly(project(":javaagent-tooling"))
  testCompileOnly(project(":javaagent-extension-api"))

  testImplementation("net.bytebuddy:byte-buddy")
  testImplementation("net.bytebuddy:byte-buddy-agent")

  testImplementation("com.google.guava:guava")
  testImplementation("io.opentelemetry:opentelemetry-extension-annotations")

  testImplementation("cglib:cglib:3.2.5")

  // test instrumenting java 1.1 bytecode
  // TODO do we want this?
  testImplementation("net.sf.jt400:jt400:6.1")
}

tasks {
  val testFieldInjectionDisabled by registering(Test::class) {
    filter {
      includeTestsMatching("context.FieldInjectionDisabledTest")
      isFailOnNoMatchingTests = false
    }
    include("**/FieldInjectionDisabledTest.*")
    jvmArgs("-Dotel.javaagent.experimental.field-injection.enabled=false")
  }

  test {
    dependsOn(testFieldInjectionDisabled)
    filter {
      excludeTestsMatching("context.FieldInjectionDisabledTest")
      isFailOnNoMatchingTests = false
    }
    // this is needed for AgentInstrumentationSpecificationTest
    jvmArgs("-Dotel.javaagent.exclude-classes=config.exclude.packagename.*,config.exclude.SomeClass,config.exclude.SomeClass\$NestedClass")
  }
}
