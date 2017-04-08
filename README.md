
Welcome to the Shabdiz Project
==============================
Shabdiz is a distributed application instance management framework.
It allows deployment termination and monitoring of distributed applications across multiple machines.
Shabdiz is written in pure Java.

Shabdiz consists of three modules:
 - shabdiz-core: contains the core monitoring, deployment and remote command execution functionality,
 - shabdiz-job: provides a distributed job execution framework, and
 - shabdiz-example: contains examples of Shabdiz usage.

Download
--------
The JAR files of latest Shabdiz snapshot is available for download from:
 - shabdiz-core: https://builds.cs.st-andrews.ac.uk/job/shabdiz/lastSuccessfulBuild/artifact/core/target/
 - shabdiz-job: https://builds.cs.st-andrews.ac.uk/job/shabdiz/lastSuccessfulBuild/artifact/job/target/
 - shabdiz-example: https://builds.cs.st-andrews.ac.uk/job/shabdiz/lastSuccessfulBuild/artifact/example/target/

Alternatively, you can add the following section to your pom.xml:
 1. Add the following to your pom.xml repositories:
	<repository>
		<id>uk.ac.standrews.cs.maven.repository</id>
		<name>School of Computer Science Maven Repository</name>
		<url>http://maven.cs.st-andrews.ac.uk/</url>
	</repository>

 2. Add the following to your pom.xml dependencies:
 	<dependency>
		<groupId>uk.ac.standrews.cs</groupId>
		<artifactId>shabdiz</artifactId>
		<version>1.0-SNAPSHOT</version>
	</dependency>

API Documentation
-----------------
The API documentation of the latest Shabdiz build is available here:
 - https://builds.cs.st-andrews.ac.uk/job/shabdiz/javadoc



