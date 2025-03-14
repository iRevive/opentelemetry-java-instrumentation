pluginManagement {
  plugins {
    id("com.github.jk1.dependency-license-report") version "2.9"
    id("com.google.cloud.tools.jib") version "3.4.4"
    id("com.gradle.plugin-publish") version "1.3.1"
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
    id("org.jetbrains.kotlin.jvm") version "2.1.10"
    id("org.xbib.gradle.plugin.jflex") version "3.0.2"
    id("org.unbroken-dome.xjc") version "2.0.0"
    id("org.graalvm.buildtools.native") version "0.10.6"
  }
}

plugins {
  id("com.gradle.common-custom-user-data-gradle-plugin") version "2.1"
  id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
  // this can't live in pluginManagement currently due to
  // https://github.com/bmuschko/gradle-docker-plugin/issues/1123
  // in particular, these commands are failing (reproducible locally):
  // ./gradlew :smoke-tests:images:servlet:buildLinuxTestImages pushMatrix -PsmokeTestServer=jetty
  // ./gradlew :smoke-tests:images:servlet:buildWindowsTestImages pushMatrix -PsmokeTestServer=jetty
  id("com.bmuschko.docker-remote-api") version "9.4.0" apply false
  id("com.gradle.develocity") version "3.19.2"
}

dependencyResolutionManagement {
  repositories {
    mavenCentral()
    mavenLocal()
  }

  versionCatalogs {
    fun addSpringBootCatalog(name: String, minVersion: String, maxVersion: String) {
      val latestDepTest = gradle.startParameter.projectProperties["testLatestDeps"] == "true"
      create(name) {
        val version =
          gradle.startParameter.projectProperties["${name}Version"]
            ?: (if (latestDepTest) maxVersion else minVersion)
        plugin("versions", "org.springframework.boot").version(version)
      }
    }
    // r2dbc is not compatible with earlier versions
    addSpringBootCatalog("springBoot2", "2.6.15", "2.+")
    // spring boot 3.0 is not compatible with graalvm native image
    addSpringBootCatalog("springBoot31", "3.1.0", "3.+")
    addSpringBootCatalog("springBoot32", "3.2.0", "3.+")
  }
}

develocity {
  buildScan {
    publishing.onlyIf { System.getenv("CI") != null }
    termsOfUseUrl.set("https://gradle.com/help/legal-terms-of-use")
    termsOfUseAgree.set("yes")

    if (!gradle.startParameter.taskNames.contains("listTestsInPartition") &&
      !gradle.startParameter.taskNames.contains(":test-report:reportFlakyTests")) {
      buildScanPublished {
        File("build-scan.txt").printWriter().use { writer ->
          writer.println(buildScanUri)
        }
      }
    }
  }
}

rootProject.name = "opentelemetry-java-instrumentation"

includeBuild("conventions")

include(":custom-checks")

include(":muzzle")

// agent projects
include(":opentelemetry-api-shaded-for-instrumenting")
include(":opentelemetry-ext-annotations-shaded-for-instrumenting")
include(":opentelemetry-instrumentation-annotations-shaded-for-instrumenting")
include(":opentelemetry-instrumentation-api-shaded-for-instrumenting")
include(":javaagent-bootstrap")
include(":javaagent-extension-api")
include(":javaagent-tooling")
include(":javaagent-tooling:javaagent-tooling-java9")
include(":javaagent-tooling:jdk18-testing")
include(":javaagent-internal-logging-application")
include(":javaagent-internal-logging-simple")
include(":javaagent")
include(":sdk-autoconfigure-support")

include(":bom")
include(":bom-alpha")
include(":instrumentation-api")
include(":instrumentation-api-incubator")
include(":instrumentation-annotations")
include(":instrumentation-annotations-support")
include(":instrumentation-annotations-support-testing")

// misc
include(":dependencyManagement")
include(":instrumentation-docs")
include(":test-report")
include(":testing:agent-exporter")
include(":testing:agent-for-testing")
include(":testing:armeria-shaded-for-testing")
include(":testing:proto-shaded-for-testing")
include(":testing-common")
include(":testing-common:integration-tests")
include(":testing-common:library-for-integration-tests")

// smoke tests
include(":smoke-tests")
include(":smoke-tests:images:early-jdk8")
include(":smoke-tests:images:fake-backend")
include(":smoke-tests:images:grpc")
include(":smoke-tests:images:play")
include(":smoke-tests:images:quarkus")
include(":smoke-tests:images:security-manager")
include(":smoke-tests:images:servlet")
include(":smoke-tests:images:servlet:servlet-3.0")
include(":smoke-tests:images:servlet:servlet-5.0")
include(":smoke-tests:images:spring-boot")

include(":smoke-tests-otel-starter:spring-smoke-testing")
include(":smoke-tests-otel-starter:spring-boot-2")
include(":smoke-tests-otel-starter:spring-boot-3")
include(":smoke-tests-otel-starter:spring-boot-3.2")
include(":smoke-tests-otel-starter:spring-boot-common")
include(":smoke-tests-otel-starter:spring-boot-reactive-2")
include(":smoke-tests-otel-starter:spring-boot-reactive-3")
include(":smoke-tests-otel-starter:spring-boot-reactive-common")

include(":instrumentation:activej-http-6.0:javaagent")
include(":instrumentation:akka:akka-actor-2.3:javaagent")
include(":instrumentation:akka:akka-actor-fork-join-2.5:javaagent")
include(":instrumentation:akka:akka-http-10.0:javaagent")
include(":instrumentation:alibaba-druid-1.0:javaagent")
include(":instrumentation:alibaba-druid-1.0:library")
include(":instrumentation:alibaba-druid-1.0:testing")
include(":instrumentation:apache-dbcp-2.0:javaagent")
include(":instrumentation:apache-dbcp-2.0:library")
include(":instrumentation:apache-dbcp-2.0:testing")
include(":instrumentation:apache-dubbo-2.7:javaagent")
include(":instrumentation:apache-dubbo-2.7:library-autoconfigure")
include(":instrumentation:apache-dubbo-2.7:testing")
include(":instrumentation:apache-httpasyncclient-4.1:javaagent")
include(":instrumentation:apache-httpclient:apache-httpclient-2.0:javaagent")
include(":instrumentation:apache-httpclient:apache-httpclient-4.0:javaagent")
include(":instrumentation:apache-httpclient:apache-httpclient-4.3:library")
include(":instrumentation:apache-httpclient:apache-httpclient-4.3:testing")
include(":instrumentation:apache-httpclient:apache-httpclient-5.0:javaagent")
include(":instrumentation:apache-httpclient:apache-httpclient-5.2:library")
include(":instrumentation:apache-shenyu-2.4:javaagent")
include(":instrumentation:armeria:armeria-1.3:javaagent")
include(":instrumentation:armeria:armeria-1.3:library")
include(":instrumentation:armeria:armeria-1.3:testing")
include(":instrumentation:armeria:armeria-grpc-1.14:javaagent")
include(":instrumentation:async-http-client:async-http-client-1.9:javaagent")
include(":instrumentation:async-http-client:async-http-client-2.0:javaagent")
include(":instrumentation:aws-lambda:aws-lambda-core-1.0:javaagent")
include(":instrumentation:aws-lambda:aws-lambda-core-1.0:library")
include(":instrumentation:aws-lambda:aws-lambda-core-1.0:testing")
include(":instrumentation:aws-lambda:aws-lambda-events-2.2:javaagent")
include(":instrumentation:aws-lambda:aws-lambda-events-2.2:library")
include(":instrumentation:aws-lambda:aws-lambda-events-2.2:testing")
include(":instrumentation:aws-sdk:aws-sdk-1.11:javaagent")
include(":instrumentation:aws-sdk:aws-sdk-1.11:library")
include(":instrumentation:aws-sdk:aws-sdk-1.11:library-autoconfigure")
include(":instrumentation:aws-sdk:aws-sdk-1.11:testing")
include(":instrumentation:aws-sdk:aws-sdk-2.2:javaagent")
include(":instrumentation:aws-sdk:aws-sdk-2.2:library")
include(":instrumentation:aws-sdk:aws-sdk-2.2:library-autoconfigure")
include(":instrumentation:aws-sdk:aws-sdk-2.2:testing")
include(":instrumentation:azure-core:azure-core-1.14:javaagent")
include(":instrumentation:azure-core:azure-core-1.14:library-instrumentation-shaded")
include(":instrumentation:azure-core:azure-core-1.19:javaagent")
include(":instrumentation:azure-core:azure-core-1.19:library-instrumentation-shaded")
include(":instrumentation:azure-core:azure-core-1.36:javaagent")
include(":instrumentation:azure-core:azure-core-1.36:library-instrumentation-shaded")
include(":instrumentation:c3p0-0.9:javaagent")
include(":instrumentation:c3p0-0.9:library")
include(":instrumentation:c3p0-0.9:testing")
include(":instrumentation:camel-2.20:javaagent")
include(":instrumentation:camel-2.20:javaagent-unit-tests")
include(":instrumentation:cassandra:cassandra-3.0:javaagent")
include(":instrumentation:cassandra:cassandra-4.0:javaagent")
include(":instrumentation:cassandra:cassandra-4.4:javaagent")
include(":instrumentation:cassandra:cassandra-4.4:library")
include(":instrumentation:cassandra:cassandra-4.4:testing")
include(":instrumentation:cassandra:cassandra-4-common:testing")
include(":instrumentation:cdi-testing")
include(":instrumentation:clickhouse-client-0.5:javaagent")
include(":instrumentation:couchbase:couchbase-2.0:javaagent")
include(":instrumentation:couchbase:couchbase-2.6:javaagent")
include(":instrumentation:couchbase:couchbase-2-common:javaagent")
include(":instrumentation:couchbase:couchbase-2-common:javaagent-unit-tests")
include(":instrumentation:couchbase:couchbase-3.1:javaagent")
include(":instrumentation:couchbase:couchbase-3.1:tracing-opentelemetry-shaded")
include(":instrumentation:couchbase:couchbase-3.1.6:javaagent")
include(":instrumentation:couchbase:couchbase-3.1.6:tracing-opentelemetry-shaded")
include(":instrumentation:couchbase:couchbase-3.2:javaagent")
include(":instrumentation:couchbase:couchbase-3.2:tracing-opentelemetry-shaded")
include(":instrumentation:couchbase:couchbase-common:testing")
include(":instrumentation:dropwizard:dropwizard-metrics-4.0:javaagent")
include(":instrumentation:dropwizard:dropwizard-testing")
include(":instrumentation:dropwizard:dropwizard-views-0.7:javaagent")
include(":instrumentation:elasticsearch:elasticsearch-api-client-7.16:javaagent")
include(":instrumentation:elasticsearch:elasticsearch-api-client-7.16:javaagent-unit-tests")
include(":instrumentation:elasticsearch:elasticsearch-rest-5.0:javaagent")
include(":instrumentation:elasticsearch:elasticsearch-rest-6.4:javaagent")
include(":instrumentation:elasticsearch:elasticsearch-rest-7.0:javaagent")
include(":instrumentation:elasticsearch:elasticsearch-rest-7.0:library")
include(":instrumentation:elasticsearch:elasticsearch-rest-common-5.0:javaagent")
include(":instrumentation:elasticsearch:elasticsearch-rest-common-5.0:library")
include(":instrumentation:elasticsearch:elasticsearch-transport-5.0:javaagent")
include(":instrumentation:elasticsearch:elasticsearch-transport-5.3:javaagent")
include(":instrumentation:elasticsearch:elasticsearch-transport-6.0:javaagent")
include(":instrumentation:elasticsearch:elasticsearch-transport-6.0:testing")
include(":instrumentation:elasticsearch:elasticsearch-transport-common:javaagent")
include(":instrumentation:elasticsearch:elasticsearch-transport-common:testing")
include(":instrumentation:executors:bootstrap")
include(":instrumentation:executors:javaagent")
include(":instrumentation:executors:jdk21-testing")
include(":instrumentation:executors:testing")
include(":instrumentation:external-annotations:javaagent")
include(":instrumentation:external-annotations:javaagent-unit-tests")
include(":instrumentation:finagle-http-23.11:javaagent")
include(":instrumentation:finatra-2.9:javaagent")
include(":instrumentation:geode-1.4:javaagent")
include(":instrumentation:google-http-client-1.19:javaagent")
include(":instrumentation:grails-3.0:javaagent")
include(":instrumentation:graphql-java:graphql-java-12.0:javaagent")
include(":instrumentation:graphql-java:graphql-java-12.0:library")
include(":instrumentation:graphql-java:graphql-java-20.0:javaagent")
include(":instrumentation:graphql-java:graphql-java-20.0:library")
include(":instrumentation:graphql-java:graphql-java-common:library")
include(":instrumentation:graphql-java:graphql-java-common:testing")
include(":instrumentation:grizzly-2.3:javaagent")
include(":instrumentation:grpc-1.6:javaagent")
include(":instrumentation:grpc-1.6:library")
include(":instrumentation:grpc-1.6:testing")
include(":instrumentation:guava-10.0:javaagent")
include(":instrumentation:guava-10.0:library")
include(":instrumentation:gwt-2.0:javaagent")
include(":instrumentation:hibernate:hibernate-3.3:javaagent")
include(":instrumentation:hibernate:hibernate-4.0:javaagent")
include(":instrumentation:hibernate:hibernate-6.0:javaagent")
include(":instrumentation:hibernate:hibernate-6.0:spring-testing")
include(":instrumentation:hibernate:hibernate-common:javaagent")
include(":instrumentation:hibernate:hibernate-procedure-call-4.3:javaagent")
include(":instrumentation:hibernate:hibernate-reactive-1.0:javaagent")
include(":instrumentation:hikaricp-3.0:javaagent")
include(":instrumentation:hikaricp-3.0:library")
include(":instrumentation:hikaricp-3.0:testing")
include(":instrumentation:http-url-connection:javaagent")
include(":instrumentation:hystrix-1.4:javaagent")
include(":instrumentation:influxdb-2.4:javaagent")
include(":instrumentation:internal:internal-application-logger:bootstrap")
include(":instrumentation:internal:internal-application-logger:javaagent")
include(":instrumentation:internal:internal-class-loader:javaagent")
include(":instrumentation:internal:internal-class-loader:javaagent-integration-tests")
include(":instrumentation:internal:internal-eclipse-osgi-3.6:javaagent")
include(":instrumentation:internal:internal-lambda:javaagent")
include(":instrumentation:internal:internal-reflection:javaagent")
include(":instrumentation:internal:internal-reflection:javaagent-integration-tests")
include(":instrumentation:internal:internal-url-class-loader:javaagent")
include(":instrumentation:internal:internal-url-class-loader:javaagent-integration-tests")
include(":instrumentation:java-http-client:javaagent")
include(":instrumentation:java-http-client:library")
include(":instrumentation:java-http-client:testing")
include(":instrumentation:java-http-server:javaagent")
include(":instrumentation:java-http-server:library")
include(":instrumentation:java-http-server:testing")
include(":instrumentation:java-util-logging:javaagent")
include(":instrumentation:java-util-logging:shaded-stub-for-instrumenting")
include(":instrumentation:javalin-5.0:javaagent")
include(":instrumentation:jaxrs:jaxrs-1.0:javaagent")
include(":instrumentation:jaxrs:jaxrs-2.0:jaxrs-2.0-annotations:javaagent")
include(":instrumentation:jaxrs:jaxrs-2.0:jaxrs-2.0-arquillian-testing")
include(":instrumentation:jaxrs:jaxrs-2.0:jaxrs-2.0-common:javaagent")
include(":instrumentation:jaxrs:jaxrs-2.0:jaxrs-2.0-common:testing")
include(":instrumentation:jaxrs:jaxrs-2.0:jaxrs-2.0-cxf-3.2:javaagent")
include(":instrumentation:jaxrs:jaxrs-2.0:jaxrs-2.0-jersey-2.0:javaagent")
include(":instrumentation:jaxrs:jaxrs-2.0:jaxrs-2.0-payara-testing")
include(":instrumentation:jaxrs:jaxrs-2.0:jaxrs-2.0-resteasy-3.0:javaagent")
include(":instrumentation:jaxrs:jaxrs-2.0:jaxrs-2.0-resteasy-3.1:javaagent")
include(":instrumentation:jaxrs:jaxrs-2.0:jaxrs-2.0-resteasy-common:javaagent")
include(":instrumentation:jaxrs:jaxrs-2.0:jaxrs-2.0-tomee-testing")
include(":instrumentation:jaxrs:jaxrs-2.0:jaxrs-2.0-wildfly-testing")
include(":instrumentation:jaxrs:jaxrs-3.0:jaxrs-3.0-annotations:javaagent")
include(":instrumentation:jaxrs:jaxrs-3.0:jaxrs-3.0-common:javaagent")
include(":instrumentation:jaxrs:jaxrs-3.0:jaxrs-3.0-common:testing")
include(":instrumentation:jaxrs:jaxrs-3.0:jaxrs-3.0-jersey-3.0:javaagent")
include(":instrumentation:jaxrs:jaxrs-3.0:jaxrs-3.0-resteasy-6.0:javaagent")
include(":instrumentation:jaxrs:jaxrs-common:bootstrap")
include(":instrumentation:jaxrs:jaxrs-common:javaagent")
include(":instrumentation:jaxrs:jaxrs-common:testing")
include(":instrumentation:jaxrs-client:jaxrs-client-1.1-testing")
include(":instrumentation:jaxrs-client:jaxrs-client-2.0-testing")
include(":instrumentation:jaxws:jaxws-2.0:javaagent")
include(":instrumentation:jaxws:jaxws-2.0-arquillian-testing")
include(":instrumentation:jaxws:jaxws-2.0-axis2-1.6:javaagent")
include(":instrumentation:jaxws:jaxws-2.0-axis2-1.6-testing")
include(":instrumentation:jaxws:jaxws-2.0-common-testing")
include(":instrumentation:jaxws:jaxws-2.0-metro-2.2-testing")
include(":instrumentation:jaxws:jaxws-2.0-tomee-testing")
include(":instrumentation:jaxws:jaxws-2.0-wildfly-testing")
include(":instrumentation:jaxws:jaxws-3.0-axis2-2.0-testing")
include(":instrumentation:jaxws:jaxws-3.0-common-testing")
include(":instrumentation:jaxws:jaxws-3.0-cxf-4.0-testing")
include(":instrumentation:jaxws:jaxws-3.0-metro-3.0-testing")
include(":instrumentation:jaxws:jaxws-common:javaagent")
include(":instrumentation:jaxws:jaxws-cxf-3.0:javaagent")
include(":instrumentation:jaxws:jaxws-cxf-3.0:javaagent-unit-tests")
include(":instrumentation:jaxws:jaxws-jws-api-1.1:javaagent")
include(":instrumentation:jaxws:jaxws-metro-2.2:javaagent")
include(":instrumentation:jboss-logmanager:jboss-logmanager-appender-1.1:javaagent")
include(":instrumentation:jboss-logmanager:jboss-logmanager-mdc-1.1:javaagent")
include(":instrumentation:jdbc:bootstrap")
include(":instrumentation:jdbc:javaagent")
include(":instrumentation:jdbc:library")
include(":instrumentation:jdbc:testing")
include(":instrumentation:jedis:jedis-1.4:javaagent")
include(":instrumentation:jedis:jedis-1.4:testing")
include(":instrumentation:jedis:jedis-3.0:javaagent")
include(":instrumentation:jedis:jedis-4.0:javaagent")
include(":instrumentation:jedis:jedis-common:javaagent")
include(":instrumentation:jetty:jetty-8.0:javaagent")
include(":instrumentation:jetty:jetty-11.0:javaagent")
include(":instrumentation:jetty:jetty-12.0:javaagent")
include(":instrumentation:jetty:jetty-common:javaagent")
include(":instrumentation:jetty-httpclient:jetty-httpclient-9.2:javaagent")
include(":instrumentation:jetty-httpclient:jetty-httpclient-9.2:library")
include(":instrumentation:jetty-httpclient:jetty-httpclient-9.2:testing")
include(":instrumentation:jetty-httpclient:jetty-httpclient-12.0:javaagent")
include(":instrumentation:jetty-httpclient:jetty-httpclient-12.0:library")
include(":instrumentation:jetty-httpclient:jetty-httpclient-12.0:testing")
include(":instrumentation:jms:jms-1.1:javaagent")
include(":instrumentation:jms:jms-3.0:javaagent")
include(":instrumentation:jms:jms-common:bootstrap")
include(":instrumentation:jms:jms-common:javaagent")
include(":instrumentation:jms:jms-common:javaagent-unit-tests")
include(":instrumentation:jmx-metrics:javaagent")
include(":instrumentation:jmx-metrics:library")
include(":instrumentation:jodd-http-4.2:javaagent")
include(":instrumentation:jodd-http-4.2:javaagent-unit-tests")
include(":instrumentation:jsf:jsf-jakarta-common:javaagent")
include(":instrumentation:jsf:jsf-jakarta-common:testing")
include(":instrumentation:jsf:jsf-javax-common:javaagent")
include(":instrumentation:jsf:jsf-javax-common:testing")
include(":instrumentation:jsf:jsf-mojarra-1.2:javaagent")
include(":instrumentation:jsf:jsf-mojarra-3.0:javaagent")
include(":instrumentation:jsf:jsf-myfaces-1.2:javaagent")
include(":instrumentation:jsf:jsf-myfaces-3.0:javaagent")
include(":instrumentation:jsp-2.3:javaagent")
include(":instrumentation:kafka:kafka-clients:kafka-clients-0.11:bootstrap")
include(":instrumentation:kafka:kafka-clients:kafka-clients-0.11:javaagent")
include(":instrumentation:kafka:kafka-clients:kafka-clients-0.11:testing")
include(":instrumentation:kafka:kafka-clients:kafka-clients-2.6:library")
include(":instrumentation:kafka:kafka-clients:kafka-clients-common-0.11:library")
include(":instrumentation:kafka:kafka-streams-0.11:javaagent")
include(":instrumentation:kotlinx-coroutines:kotlinx-coroutines-1.0:javaagent")
include(":instrumentation:kotlinx-coroutines:kotlinx-coroutines-flow-1.3:javaagent")
include(":instrumentation:kotlinx-coroutines:kotlinx-coroutines-flow-1.3:javaagent-kotlin")
include(":instrumentation:ktor:ktor-1.0:library")
include(":instrumentation:ktor:ktor-2.0:javaagent")
include(":instrumentation:ktor:ktor-2.0:library")
include(":instrumentation:ktor:ktor-2.0:testing")
include(":instrumentation:ktor:ktor-2-common:library")
include(":instrumentation:ktor:ktor-3.0:javaagent")
include(":instrumentation:ktor:ktor-3.0:library")
include(":instrumentation:ktor:ktor-3.0:testing")
include(":instrumentation:ktor:ktor-common:library")
include(":instrumentation:kubernetes-client-7.0:javaagent")
include(":instrumentation:kubernetes-client-7.0:javaagent-unit-tests")
include(":instrumentation:lettuce:lettuce-4.0:javaagent")
include(":instrumentation:lettuce:lettuce-5.0:javaagent")
include(":instrumentation:lettuce:lettuce-5.1:javaagent")
include(":instrumentation:lettuce:lettuce-5.1:library")
include(":instrumentation:lettuce:lettuce-5.1:testing")
include(":instrumentation:lettuce:lettuce-common:library")
include(":instrumentation:liberty:compile-stub")
include(":instrumentation:liberty:liberty-20.0:javaagent")
include(":instrumentation:liberty:liberty-dispatcher-20.0:javaagent")
include(":instrumentation:log4j:log4j-appender-1.2:javaagent")
include(":instrumentation:log4j:log4j-appender-2.17:javaagent")
include(":instrumentation:log4j:log4j-appender-2.17:library")
include(":instrumentation:log4j:log4j-context-data:log4j-context-data-2.7:javaagent")
include(":instrumentation:log4j:log4j-context-data:log4j-context-data-2.17:javaagent")
include(":instrumentation:log4j:log4j-context-data:log4j-context-data-2.17:library-autoconfigure")
include(":instrumentation:log4j:log4j-context-data:log4j-context-data-common:testing")
include(":instrumentation:log4j:log4j-mdc-1.2:javaagent")
include(":instrumentation:logback:logback-appender-1.0:javaagent")
include(":instrumentation:logback:logback-appender-1.0:library")
include(":instrumentation:logback:logback-mdc-1.0:javaagent")
include(":instrumentation:logback:logback-mdc-1.0:library")
include(":instrumentation:logback:logback-mdc-1.0:testing")
include(":instrumentation:methods:javaagent")
include(":instrumentation:micrometer:micrometer-1.5:javaagent")
include(":instrumentation:micrometer:micrometer-1.5:library")
include(":instrumentation:micrometer:micrometer-1.5:testing")
include(":instrumentation:mongo:mongo-3.1:javaagent")
include(":instrumentation:mongo:mongo-3.1:library")
include(":instrumentation:mongo:mongo-3.1:testing")
include(":instrumentation:mongo:mongo-3.7:javaagent")
include(":instrumentation:mongo:mongo-4.0:javaagent")
include(":instrumentation:mongo:mongo-async-3.3:javaagent")
include(":instrumentation:mongo:mongo-common:testing")
include(":instrumentation:mybatis-3.2:javaagent")
include(":instrumentation:netty:netty-3.8:javaagent")
include(":instrumentation:netty:netty-4.0:javaagent")
include(":instrumentation:netty:netty-4.1:javaagent")
include(":instrumentation:netty:netty-4.1:library")
include(":instrumentation:netty:netty-4.1:testing")
include(":instrumentation:netty:netty-common-4.0:javaagent")
include(":instrumentation:netty:netty-common-4.0:library")
include(":instrumentation:netty:netty-common:library")
include(":instrumentation:okhttp:okhttp-2.2:javaagent")
include(":instrumentation:okhttp:okhttp-3.0:javaagent")
include(":instrumentation:okhttp:okhttp-3.0:library")
include(":instrumentation:okhttp:okhttp-3.0:testing")
include(":instrumentation:opencensus-shim:testing")
include(":instrumentation:opensearch:opensearch-rest-1.0:javaagent")
include(":instrumentation:opensearch:opensearch-rest-common:javaagent")
include(":instrumentation:opentelemetry-api:opentelemetry-api-1.0:javaagent")
include(":instrumentation:opentelemetry-api:opentelemetry-api-1.4:javaagent")
include(":instrumentation:opentelemetry-api:opentelemetry-api-1.10:javaagent")
include(":instrumentation:opentelemetry-api:opentelemetry-api-1.15:javaagent")
include(":instrumentation:opentelemetry-api:opentelemetry-api-1.27:javaagent")
include(":instrumentation:opentelemetry-api:opentelemetry-api-1.31:javaagent")
include(":instrumentation:opentelemetry-api:opentelemetry-api-1.32:javaagent")
include(":instrumentation:opentelemetry-api:opentelemetry-api-1.37:javaagent")
include(":instrumentation:opentelemetry-api:opentelemetry-api-1.38:javaagent")
include(":instrumentation:opentelemetry-api:opentelemetry-api-1.40:javaagent")
include(":instrumentation:opentelemetry-api:opentelemetry-api-1.42:javaagent")
include(":instrumentation:opentelemetry-api:opentelemetry-api-1.47:javaagent")
include(":instrumentation:opentelemetry-extension-annotations-1.0:javaagent")
include(":instrumentation:opentelemetry-extension-kotlin-1.0:javaagent")
include(":instrumentation:opentelemetry-instrumentation-annotations-1.16:javaagent")
include(":instrumentation:opentelemetry-instrumentation-api:javaagent")
include(":instrumentation:opentelemetry-instrumentation-api:testing")
include(":instrumentation:oracle-ucp-11.2:javaagent")
include(":instrumentation:oracle-ucp-11.2:library")
include(":instrumentation:oracle-ucp-11.2:testing")
include(":instrumentation:oshi:javaagent")
include(":instrumentation:oshi:library")
include(":instrumentation:oshi:testing")
include(":instrumentation:otel4s:otel4s-0.12:bootstrap")
include(":instrumentation:otel4s:otel4s-0.12:javaagent")
include(":instrumentation:payara:javaagent")
include(":instrumentation:pekko:pekko-actor-1.0:javaagent")
include(":instrumentation:pekko:pekko-http-1.0:javaagent")
include(":instrumentation:play:play-mvc:play-mvc-2.4:javaagent")
include(":instrumentation:play:play-mvc:play-mvc-2.6:javaagent")
include(":instrumentation:play:play-ws:play-ws-1.0:javaagent")
include(":instrumentation:play:play-ws:play-ws-2.0:javaagent")
include(":instrumentation:play:play-ws:play-ws-2.1:javaagent")
include(":instrumentation:play:play-ws:play-ws-common:javaagent")
include(":instrumentation:play:play-ws:play-ws-common:testing")
include(":instrumentation:powerjob-4.0:javaagent")
include(":instrumentation:pulsar:pulsar-2.8:javaagent")
include(":instrumentation:pulsar:pulsar-2.8:javaagent-unit-tests")
include(":instrumentation:quarkus-resteasy-reactive:common-testing")
include(":instrumentation:quarkus-resteasy-reactive:javaagent")
include(":instrumentation:quarkus-resteasy-reactive:quarkus2-testing")
include(":instrumentation:quarkus-resteasy-reactive:quarkus3-testing")
include(":instrumentation:quartz-2.0:javaagent")
include(":instrumentation:quartz-2.0:library")
include(":instrumentation:quartz-2.0:testing")
include(":instrumentation:r2dbc-1.0:javaagent")
include(":instrumentation:r2dbc-1.0:library")
include(":instrumentation:r2dbc-1.0:library-instrumentation-shaded")
include(":instrumentation:r2dbc-1.0:testing")
include(":instrumentation:rabbitmq-2.7:javaagent")
include(":instrumentation:ratpack:ratpack-1.4:javaagent")
include(":instrumentation:ratpack:ratpack-1.4:testing")
include(":instrumentation:ratpack:ratpack-1.7:javaagent")
include(":instrumentation:ratpack:ratpack-1.7:library")
include(":instrumentation:reactor:reactor-3.1:javaagent")
include(":instrumentation:reactor:reactor-3.1:library")
include(":instrumentation:reactor:reactor-3.1:testing")
include(":instrumentation:reactor:reactor-3.4:javaagent")
include(":instrumentation:reactor:reactor-kafka-1.0:javaagent")
include(":instrumentation:reactor:reactor-kafka-1.0:testing")
include(":instrumentation:reactor:reactor-netty:reactor-netty-0.9:javaagent")
include(":instrumentation:reactor:reactor-netty:reactor-netty-1.0:javaagent")
include(":instrumentation:reactor:reactor-netty:reactor-netty-1.0:javaagent-unit-tests")
include(":instrumentation:rediscala-1.8:javaagent")
include(":instrumentation:redisson:redisson-3.0:javaagent")
include(":instrumentation:redisson:redisson-3.17:javaagent")
include(":instrumentation:redisson:redisson-common:javaagent")
include(":instrumentation:redisson:redisson-common:testing")
include(":instrumentation:resources:library")
include(":instrumentation:restlet:restlet-1.1:javaagent")
include(":instrumentation:restlet:restlet-1.1:library")
include(":instrumentation:restlet:restlet-1.1:testing")
include(":instrumentation:restlet:restlet-2.0:javaagent")
include(":instrumentation:restlet:restlet-2.0:library")
include(":instrumentation:restlet:restlet-2.0:testing")
include(":instrumentation:rmi:bootstrap")
include(":instrumentation:rmi:javaagent")
include(":instrumentation:rocketmq:rocketmq-client:rocketmq-client-4.8:javaagent")
include(":instrumentation:rocketmq:rocketmq-client:rocketmq-client-4.8:library")
include(":instrumentation:rocketmq:rocketmq-client:rocketmq-client-4.8:testing")
include(":instrumentation:rocketmq:rocketmq-client:rocketmq-client-5.0:javaagent")
include(":instrumentation:rocketmq:rocketmq-client:rocketmq-client-5.0:testing")
include(":instrumentation:runtime-telemetry:runtime-telemetry-java8:javaagent")
include(":instrumentation:runtime-telemetry:runtime-telemetry-java8:library")
include(":instrumentation:runtime-telemetry:runtime-telemetry-java8:testing")
include(":instrumentation:runtime-telemetry:runtime-telemetry-java17:javaagent")
include(":instrumentation:runtime-telemetry:runtime-telemetry-java17:library")
include(":instrumentation:rxjava:rxjava-1.0:library")
include(":instrumentation:rxjava:rxjava-2.0:javaagent")
include(":instrumentation:rxjava:rxjava-2.0:library")
include(":instrumentation:rxjava:rxjava-2.0:testing")
include(":instrumentation:rxjava:rxjava-3.0:javaagent")
include(":instrumentation:rxjava:rxjava-3.0:library")
include(":instrumentation:rxjava:rxjava-3.1.1:javaagent")
include(":instrumentation:rxjava:rxjava-3.1.1:library")
include(":instrumentation:rxjava:rxjava-3-common:library")
include(":instrumentation:rxjava:rxjava-3-common:testing")
include(":instrumentation:scala-fork-join-2.8:javaagent")
include(":instrumentation:servlet:servlet-2.2:javaagent")
include(":instrumentation:servlet:servlet-3.0:javaagent")
include(":instrumentation:servlet:servlet-3.0:javaagent-unit-tests")
include(":instrumentation:servlet:servlet-3.0:testing")
include(":instrumentation:servlet:servlet-5.0:javaagent")
include(":instrumentation:servlet:servlet-5.0:javaagent-unit-tests")
include(":instrumentation:servlet:servlet-5.0:jetty11-testing")
include(":instrumentation:servlet:servlet-5.0:jetty12-testing")
include(":instrumentation:servlet:servlet-5.0:testing")
include(":instrumentation:servlet:servlet-5.0:tomcat-testing")
include(":instrumentation:servlet:servlet-common:bootstrap")
include(":instrumentation:servlet:servlet-common:javaagent")
include(":instrumentation:servlet:servlet-javax-common:javaagent")
include(":instrumentation:spark-2.3:javaagent")
include(":instrumentation:spring:spring-batch-3.0:javaagent")
include(":instrumentation:spring:spring-boot-actuator-autoconfigure-2.0:javaagent")
include(":instrumentation:spring:spring-boot-autoconfigure")
include(":instrumentation:spring:spring-boot-resources:javaagent")
include(":instrumentation:spring:spring-boot-resources:javaagent-unit-tests")
include(":instrumentation:spring:spring-cloud-aws-3.0:javaagent")
include(":instrumentation:spring:spring-cloud-gateway:spring-cloud-gateway-2.0:javaagent")
include(":instrumentation:spring:spring-cloud-gateway:spring-cloud-gateway-2.2:testing")
include(":instrumentation:spring:spring-cloud-gateway:spring-cloud-gateway-common:testing")
include(":instrumentation:spring:spring-core-2.0:javaagent")
include(":instrumentation:spring:spring-data:spring-data-1.8:javaagent")
include(":instrumentation:spring:spring-data:spring-data-3.0:testing")
include(":instrumentation:spring:spring-data:spring-data-3.0:kotlin-testing")
include(":instrumentation:spring:spring-data:spring-data-common:testing")
include(":instrumentation:spring:spring-integration-4.1:javaagent")
include(":instrumentation:spring:spring-integration-4.1:library")
include(":instrumentation:spring:spring-integration-4.1:testing")
include(":instrumentation:spring:spring-jms:spring-jms-2.0:javaagent")
include(":instrumentation:spring:spring-jms:spring-jms-2.0:testing")
include(":instrumentation:spring:spring-jms:spring-jms-6.0:javaagent")
include(":instrumentation:spring:spring-kafka-2.7:javaagent")
include(":instrumentation:spring:spring-kafka-2.7:library")
include(":instrumentation:spring:spring-kafka-2.7:testing")
include(":instrumentation:spring:spring-pulsar-1.0:javaagent")
include(":instrumentation:spring:spring-pulsar-1.0:testing")
include(":instrumentation:spring:spring-rabbit-1.0:javaagent")
include(":instrumentation:spring:spring-rmi-4.0:javaagent")
include(":instrumentation:spring:spring-scheduling-3.1:bootstrap")
include(":instrumentation:spring:spring-scheduling-3.1:javaagent")
include(":instrumentation:spring:spring-security-config-6.0:javaagent")
include(":instrumentation:spring:spring-security-config-6.0:library")
include(":instrumentation:spring:spring-web:spring-web-3.1:javaagent")
include(":instrumentation:spring:spring-web:spring-web-3.1:library")
include(":instrumentation:spring:spring-web:spring-web-3.1:testing")
include(":instrumentation:spring:spring-web:spring-web-6.0:javaagent")
include(":instrumentation:spring:spring-webflux:spring-webflux-5.0:javaagent")
include(":instrumentation:spring:spring-webflux:spring-webflux-5.0:testing")
include(":instrumentation:spring:spring-webflux:spring-webflux-5.3:library")
include(":instrumentation:spring:spring-webflux:spring-webflux-5.3:testing")
include(":instrumentation:spring:spring-webmvc:spring-webmvc-3.1:javaagent")
include(":instrumentation:spring:spring-webmvc:spring-webmvc-3.1:wildfly-testing")
include(":instrumentation:spring:spring-webmvc:spring-webmvc-5.3:library")
include(":instrumentation:spring:spring-webmvc:spring-webmvc-6.0:javaagent")
include(":instrumentation:spring:spring-webmvc:spring-webmvc-6.0:library")
include(":instrumentation:spring:spring-webmvc:spring-webmvc-common:javaagent")
include(":instrumentation:spring:spring-webmvc:spring-webmvc-common:testing")
include(":instrumentation:spring:spring-ws-2.0:javaagent")
include(":instrumentation:spring:starters:spring-boot-starter")
include(":instrumentation:spring:starters:zipkin-spring-boot-starter")
include(":instrumentation:spymemcached-2.12:javaagent")
include(":instrumentation:struts:struts-2.3:javaagent")
include(":instrumentation:struts:struts-7.0:javaagent")
include(":instrumentation:tapestry-5.4:javaagent")
include(":instrumentation:tomcat:tomcat-7.0:javaagent")
include(":instrumentation:tomcat:tomcat-10.0:javaagent")
include(":instrumentation:tomcat:tomcat-common:javaagent")
include(":instrumentation:tomcat:tomcat-jdbc")
include(":instrumentation:twilio-6.6:javaagent")
include(":instrumentation:undertow-1.4:bootstrap")
include(":instrumentation:undertow-1.4:javaagent")
include(":instrumentation:vaadin-14.2:javaagent")
include(":instrumentation:vaadin-14.2:testing")
include(":instrumentation:vertx:vertx-http-client:vertx-http-client-3.0:javaagent")
include(":instrumentation:vertx:vertx-http-client:vertx-http-client-4.0:javaagent")
include(":instrumentation:vertx:vertx-http-client:vertx-http-client-common:javaagent")
include(":instrumentation:vertx:vertx-kafka-client-3.6:javaagent")
include(":instrumentation:vertx:vertx-kafka-client-3.6:testing")
include(":instrumentation:vertx:vertx-redis-client-4.0:javaagent")
include(":instrumentation:vertx:vertx-rx-java-3.5:javaagent")
include(":instrumentation:vertx:vertx-sql-client-4.0:javaagent")
include(":instrumentation:vertx:vertx-web-3.0:javaagent")
include(":instrumentation:vertx:vertx-web-3.0:testing")
include(":instrumentation:vibur-dbcp-11.0:javaagent")
include(":instrumentation:vibur-dbcp-11.0:library")
include(":instrumentation:vibur-dbcp-11.0:testing")
include(":instrumentation:wicket-8.0:common-testing")
include(":instrumentation:wicket-8.0:javaagent")
include(":instrumentation:wicket-8.0:wicket8-testing")
include(":instrumentation:wicket-8.0:wicket10-testing")
include(":instrumentation:xxl-job:xxl-job-1.9.2:javaagent")
include(":instrumentation:xxl-job:xxl-job-2.1.2:javaagent")
include(":instrumentation:xxl-job:xxl-job-2.3.0:javaagent")
include(":instrumentation:xxl-job:xxl-job-common:javaagent")
include(":instrumentation:xxl-job:xxl-job-common:testing")
include(":instrumentation:zio:zio-2.0:javaagent")

// benchmark
include(":benchmark-overhead-jmh")
include(":benchmark-jfr-analyzer")
