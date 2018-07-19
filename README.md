# Kamon-Spring <img align="right" src="https://rawgit.com/kamon-io/Kamon/master/kamon-logo.svg" height="150px" style="padding-left: 20px"/>
[![Build Status](https://travis-ci.org/kamon-io/kamon-spring.svg?branch=master)](https://travis-ci.org/kamon-io/kamon-spring)
[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/kamon-io/Kamon?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.kamon/kamon-spring_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.kamon/kamon-spring_2.12)




### Config

`kamon-spring` uses [TypeSafe Config][4]. Default configuration is
in `resources/reference.conf` for each subproject:

* [kamon-spring config][5]

You can customize/override any property adding an `application.conf` in the `/resources/` of your app or
by providing *System properties* (e.g. `-Dpath.of.your.key=value`). This is the standard
behavior of *TypeSafe Config*, for more info see its [doc][7].

### Micro Benchmarks

Execute from your terminal:

```bash
sbt
project benchmarks
jmh:run -i 50 -wi 20 -f1 -t1 .*Benchmark.*
```


[1]: http://www.oracle.com/technetwork/java/index-jsp-135475.html
[2]: kamon-spring-2.5/src/main/scala/kamon/servlet/v25/KamonFilterV25.scala
[3]: kamon-servlet-3.x.x/src/main/scala/kamon/servlet/v3/KamonFilterV3.scala
[4]: https://github.com/lightbend/config
[5]: kamon-spring/src/main/resources/reference.conf
[6]: kamon-servlet-3.x.x/src/main/resources/reference.conf
[7]: https://github.com/lightbend/config#standard-behavior
