[![CircleCI](https://circleci.com/gh/stacs-srg/shabdiz.svg?style=svg&circle-token=f30af40b8b0a958d1b49ad2e4e1ccc69818e49ac)](https://circleci.com/gh/stacs-srg/shabdiz)

Welcome to the Shabdiz Project
==============================
Shabdiz is a distributed application instance management framework.
It allows deployment termination and monitoring of distributed applications across multiple machines.
Shabdiz is written in pure Java.

Shabdiz consists of three modules:
 - shabdiz-core: contains the core monitoring, deployment and remote command execution functionality,
 - shabdiz-job: provides a distributed job execution framework, and
 - shabdiz-example: contains examples of Shabdiz usage.

## Usage via maven

```
<repository>
    <id>uk.ac.standrews.cs.maven.repository</id>
    <name>School of Computer Science Maven Repository</name>
    <url>https://maven.cs.st-andrews.ac.uk/</url>
</repository>
```
        
```
<dependency>
    <groupId>uk.ac.standrews.cs</groupId>
    <artifactId>shabdiz</artifactId>
    <version>2.0-SNAPSHOT</version>
</dependency>
```

## Build and coverage status

[![CircleCI](https://circleci.com/gh/stacs-srg/shabdiz.svg?style=svg)](https://circleci.com/gh/stacs-srg/shabdiz) [![codecov](https://codecov.io/gh/stacs-srg/shabdiz/branch/master/graph/badge.svg)](https://codecov.io/gh/stacs-srg/shabdiz)

## See also

* [API documentation](https://quicksilver.host.cs.st-andrews.ac.uk/apidocs/shabdiz/)
* [relevant Maven goals](https://github.com/stacs-srg/hub/tree/master/maven) (private)
