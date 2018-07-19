/* =========================================================================================
 * Copyright © 2013-2018 the kamon project <http://kamon.io/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 * =========================================================================================
 */

val kamonVersion = "1.1.0"
val kanelaVersion = "0.0.12-6f56711e5ee41e3e48beab70ad43f77dd09ee725"
val jettyV9Version = "9.4.8.v20171121"
val tomcatV8Version = "8.5.31"
val undertowVersion = "1.4.25.Final"

val kamonCore               = "io.kamon"                  %% "kamon-core"                   % kamonVersion
val kamonTestkit            = "io.kamon"                  %% "kamon-testkit"                % kamonVersion
val scalaExtension          = "io.kamon"                  %% "kanela-scala-extension"       % "0.0.10"

val springWeb               = "org.springframework"       %  "spring-web"                   % "4.3.18.RELEASE"
val springBootStarterWeb    = "org.springframework.boot"  %  "spring-boot-starter-web"      % "1.5.14.RELEASE"
val springBootStarterTest   = "org.springframework.boot"  %  "spring-boot-starter-test"     % "1.5.14.RELEASE"
val springStarterJetty      = "org.springframework.boot"  %  "spring-boot-starter-jetty"    % "1.5.14.RELEASE"
val springStarterUndertow   = "org.springframework.boot"  %  "spring-boot-starter-undertow" % "1.5.14.RELEASE"
val springBootAutoconfigure = "org.springframework.boot"  %  "spring-boot-autoconfigure"    % "1.5.14.RELEASE"
val kamonServlet3           = "io.kamon"                  %  "kamon-servlet-3_2.12"         % "1.0.0-4b315976df805d532c36a64bf9cc5331c4f7835f"
val servletApiV3            = "javax.servlet"             %  "javax.servlet-api"            % "3.0.1"
val jettyServletV9          = "org.eclipse.jetty"         %  "jetty-servlet"                % jettyV9Version
val tomcatServletV8         = "org.apache.tomcat"         %  "tomcat-catalina"              % tomcatV8Version
val undertowServlet         = "io.undertow"               %  "undertow-servlet"             % undertowVersion

val httpClient              = "org.apache.httpcomponents" %  "httpclient"                   % "4.5.5"
val logbackClassic          = "ch.qos.logback"            %  "logback-classic"              % "1.0.13"
val scalatest               = "org.scalatest"             %% "scalatest"                    % "3.0.1"


lazy val root = (project in file("."))
  .settings(scalaVersionSupport)
  .settings(noPublishing: _*)
  .aggregate(kamonSpring, kamonSpringAuto, kamonSpringBench)

lazy val kamonSpring = Project("kamon-spring", file("kamon-spring"))
  .settings(moduleName := "kamon-spring")
  .enablePlugins(JavaAgent)
  .settings(javaAgents += "io.kamon" % "kanela-agent" % kanelaVersion % "compile;test")
  .settings(commonSettings: _*)
  .settings(commonTestSettings: _*)
  .settings(KanelaAttacherTest.settings: _*)
  .settings(
    libraryDependencies ++=
      compileScope(kamonCore, kamonServlet3, scalaExtension) ++
      providedScope(jettyServletV9, tomcatServletV8, undertowServlet, servletApiV3, springWeb) ++
      testScope(scalatest, kamonTestkit, logbackClassic, springBootStarterWeb, springStarterJetty,
        springStarterUndertow, httpClient))

lazy val kamonSpringAuto = Project("kamon-spring-auto", file("kamon-spring-auto"))
  .dependsOn(kamonSpring % "compile->compile;test->test")
  .settings(moduleName := "kamon-spring-auto")
  .settings(commonSettings: _*)
  .settings(commonTestSettings: _*)
  .settings(
    libraryDependencies ++=
      compileScope(kamonCore, kamonServlet3) ++
      providedScope(springBootAutoconfigure, springWeb, servletApiV3) ++
      testScope(scalatest, kamonTestkit, logbackClassic, springBootStarterWeb, httpClient))

lazy val kamonSpringBench = Project("benchmarks", file("kamon-spring-bench"))
  .enablePlugins(JmhPlugin)
  .settings(commonSettings: _*)
  .settings(
    moduleName := "kamon-spring-bench",
    scalaVersion := "2.12.6",
    fork in Test := true)
  .settings(noPublishing: _*)
  .settings(
    libraryDependencies ++=
      compileScope(springBootStarterWeb, httpClient))
  .dependsOn(kamonSpringAuto)

lazy val scalaVersionSupport = crossScalaVersions := Seq("2.12.6")

val commonSettings = Seq(
  scalaVersion := "2.12.6",
  resolvers ++= Seq(
    Resolver.mavenLocal,
    Resolver.bintrayRepo("kamon-io", "releases"),
    Resolver.bintrayRepo("kamon-io", "snapshots")),
  scalaVersionSupport,
  scalacOptions ++= Seq(
    "-language:higherKinds",
    "-language:postfixOps") ++ (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2,12)) => Seq("-opt:l:method","-target:jvm-1.8")
    case _ => Seq.empty
  }))

val commonTestSettings = Seq(parallelExecution in Test := false)
