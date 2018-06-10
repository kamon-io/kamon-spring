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
val jettyV9Version = "9.4.8.v20171121"
val jettyV7Version = "7.6.21.v20160908"

val kamonCore               = "io.kamon"                  %% "kamon-core"             % kamonVersion
val kamonTestkit            = "io.kamon"                  %% "kamon-testkit"          % kamonVersion

val springBootStarterWeb    = "org.springframework.boot"  %  "spring-boot-starter-web"    % "2.0.1.RELEASE" exclude("org.springframework.boot", "spring-boot-starter-tomcat")
val springBootStarterTest   = "org.springframework.boot"  %  "spring-boot-starter-test"   % "2.0.1.RELEASE"
val springStarterJetty      = "org.springframework.boot"  %  "spring-boot-starter-jetty"  % "2.0.1.RELEASE"
val springBootAutoconfigure = "org.springframework.boot"  %  "spring-boot-autoconfigure"  % "2.0.1.RELEASE"
val kamonServlet3           = "io.kamon"                  %% "kamon-servlet-3.x.x"        % "0.1-14845b9cb92eedf1a091becfbf06b4ad74c16986"
val servletApiV3            = "javax.servlet"             %  "javax.servlet-api"      % "3.0.1"

val httpClient              = "org.apache.httpcomponents" %  "httpclient"             % "4.5.5"
val logbackClassic          = "ch.qos.logback"            %  "logback-classic"        % "1.0.13"
val scalatest               = "org.scalatest"             %% "scalatest"              % "3.0.1"


lazy val root = (project in file("."))
  .settings(noPublishing: _*)
  .aggregate(kamonSpring, kamonSpringAuto, kamonSpringBench)

val commonSettings = Seq(
  scalaVersion := "2.12.6",
  resolvers += Resolver.mavenLocal,
  crossScalaVersions := Seq("2.12.6", "2.11.12", "2.10.7"),
  scalacOptions ++= Seq(
    "-language:higherKinds",
    "-language:postfixOps") ++ (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2,10)) => Seq("-Yno-generic-signatures", "-target:jvm-1.7")
    case Some((2,11)) => Seq("-Ybackend:GenBCode","-Ydelambdafy:method","-target:jvm-1.8")
    case Some((2,12)) => Seq("-opt:l:method","-target:jvm-1.8")
    case _ => Seq.empty
  })
)

lazy val kamonSpring = Project("kamon-spring", file("kamon-spring"))
  .settings(moduleName := "kamon-spring")
  .settings(parallelExecution in Test := false)
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++=
      compileScope(kamonCore, kamonServlet3) ++
      providedScope(springBootStarterWeb, servletApiV3) ++
      testScope(scalatest, kamonTestkit, logbackClassic, springBootStarterTest, springStarterJetty, httpClient))

lazy val kamonSpringAuto = Project("kamon-spring-auto", file("kamon-spring-auto"))
  .dependsOn(kamonSpring)
  .settings(moduleName := "kamon-spring-auto")
  .settings(parallelExecution in Test := false)
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++=
      compileScope(kamonCore, kamonServlet3, springBootAutoconfigure) ++
      providedScope(springBootStarterWeb, servletApiV3) ++
      testScope(scalatest, kamonTestkit, logbackClassic, springBootStarterTest, springStarterJetty, httpClient))

lazy val kamonSpringBench = Project("benchmarks", file("kamon-spring-bench"))
  .enablePlugins(JmhPlugin)
  .settings(commonSettings: _*)
  .settings(
    moduleName := "kamon-spring-bench",
    scalaVersion := "2.12.6",
    crossScalaVersions := Seq("2.12.6"),
    fork in Test := true)
  .settings(noPublishing: _*)
  .settings(
    libraryDependencies ++=
      compileScope(springBootStarterWeb, springStarterJetty, httpClient))
  .dependsOn(kamonSpringAuto)
