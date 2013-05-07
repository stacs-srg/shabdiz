
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

Build
-----
Shabdiz is a Maven compliant project. 
Please make sure Maven 3.0+ is installed on your machine before attempting to build Shabdiz.
For more information on how to install Maven, please visit:
 - http://maven.apache.org/guides/getting-started/maven-in-five-minutes.html

To build Shabdiz:
 1. Clone shabdiz HG repository:
		hg clone http://code.staticiser.com/shabdiz
 2. Change current directory to the cloned shabdiz repository.
 3. Build using Maven:
 		mvn clean package 

To review the build history of Shabdiz, please visit:
 - https://builds.cs.st-andrews.ac.uk/job/shabdiz/

Issues
------
We would love to hear bad (as well as good) things about Shabdiz!
If things are not the way they should be, please share your thoughts with us here:
 - http://code.staticiser.com/shabdiz/issues

Contributions
------------
Please visit:
 - http://code.staticiser.com/shabdiz/pull-requests

Wiki
----
The incomplete wiki can be found here:
 - http://code.staticiser.com/shabdiz/wiki

License
--------
Shabdiz is under GNU GPLv3 license.
For more information, please visit:
 - http://www.tldrlegal.com/license/gnu-general-public-license-v3-(gpl-3)
 - http://opensource.org/licenses/gpl-3.0.html
 
