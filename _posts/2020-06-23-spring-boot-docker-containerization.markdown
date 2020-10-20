---
title: Spring Boot Docker Containerization
date: 2020-06-23 00:20:00 Z
categories: [SpringBoot]
tags: [docker, containerization]
author: Bhuwan Prasad Upadhyay
github_repo: https://github.com/BhuwanUpadhyay/spring-boot-docker-containerization
---

**Spring Boot** is one of the very popular framework to build the microservices and the docker container is the default choice to run the application in a cloud-native environment.

<!--more--> 

**Docker** provides the ability to package and run an application in a loosely isolated environment called a container. So, it's very important to build the right layers of the docker image for your application.

This blog post shows the available options to build a docker image for the spring boot application. Before deep into how to build the docker image, Let's create one very simple spring boot application that will return the given name as a response. After that, we will explore how to build a docker image of this application. 

## Create a Spring Boot application

To create a Spring Boot application, we'll use Spring Initializr. The application that we'll create uses:
`Spring Boot` `Spring WebFlux` `Spring Actuator` `Kotlin`

### Initialize Project

```bash
NAME='Spring Boot Docker Containerization' && \
PRJ=spring-boot-docker-containerization && \
mkdir -p $PRJ && cd $PRJ && \
curl https://start.spring.io/starter.tgz \
    -d dependencies=actuator,webflux \
    -d groupId=io.github.bhuwanupadhyay -d artifactId=$PRJ \
    -d packageName=io.github.bhuwanupadhyay.example \
    -d applicationName=SpringBoot -d name="$NAME" -d description="$NAME" \
    -d language=java -d platformVersion=2.3.1.RELEASE -d javaVersion=11 \
    -o demo.tgz && tar -xzvf demo.tgz && rm -rf demo.tgz
```

###  API Example

Create [NameManager.kt](https://github.com/BhuwanUpadhyay/spring-boot-docker-containerization/blob/master/src/main/kotlin/io/github/bhuwanupadhyay/example/NameManager.kt) under package `io.github.bhuwanupadhyay.example` and add the following text:

```kotlin
@Component
class NameHandler {

    fun findGivenName(req: ServerRequest): Mono<ServerResponse> {
        return Optional.ofNullable(req.pathVariable("given-name"))
                .map { t -> ok().bodyValue("{ \"givenName\": \"$t\"}") }
                .orElseGet { badRequest().build() }
    }

}

@Configuration
class NameRoutes(private val handler: NameHandler) {

    @Bean
    fun router() = router {
        accept(APPLICATION_JSON).nest {
            GET("/names/{given-name}", handler::findGivenName)
        }
    }

}
```

## Docker Containerization

There are four ways to containerize spring boot application. Let's take a look one by one.

### Using fat jar

Dockerfile to create a docker image of spring boot application with a fat jar.

```dockerfile
FROM amd64/openjdk:14-alpine
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java", "-jar" , "/app.jar"]
``` 

A maven profile to build a docker image with plugins: `spring-boot-maven-plugin` and `dockerfile-maven-plugin`.

```xml
<profile>
  <id>fatJar</id>
  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
        <executions>
          <execution>
            <goals>
              <goal>repackage</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
      <plugin>
        <groupId>com.spotify</groupId>
        <artifactId>dockerfile-maven-plugin</artifactId>
        <version>1.4.13</version>
        <executions>
          <execution>
            <id>default</id>
            <goals>
              <goal>build</goal>
            </goals>
          </execution>
        </executions>
        <configuration>
          <repository>docker.io/bhuwanupadhyay/${project.artifactId}-fat-jar</repository>
          <dockerfile>${project.basedir}/src/main/docker/fat-jar.dockerfile</dockerfile>
          <tag>${project.version}</tag>
        </configuration>
      </plugin>
    </plugins>
  </build>
</profile>
```

To build and test run the following command:

```bash
# Build a docker image
mvn clean install -PfatJar

# Run app
docker run -d -p8080:8080 docker.io/bhuwanupadhyay/spring-boot-docker-containerization-fat-jar:0.0.1-SNAPSHOT

# Test API
curl http://localhost:8080/names/hurry

# Output
{ "givenName": "hurry"}
```

### Using classpath in exploded jar

Dockerfile to create a docker image of spring boot application with an exploded jar.

```dockerfile
# Stage 0, "builder", extract fat jar
FROM amd64/openjdk:14-alpine as builder
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} /target/app.jar
RUN mkdir -p /target/dependency && (cd /target/dependency; jar -xf ../*.jar)

# Stage 1, "boot-app"
FROM amd64/openjdk:14-alpine
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
COPY --from=builder /target/dependency/BOOT-INF/lib /app/lib
COPY --from=builder /target/dependency/BOOT-INF/classes /app
COPY --from=builder /target/dependency/META-INF /app
ENTRYPOINT ["java", "-cp" , "app:app/lib/*", "io.github.bhuwanupadhyay.example.SpringBoot"]
``` 

A maven profile to build a docker image with plugins: `spring-boot-maven-plugin` and `dockerfile-maven-plugin`. 

```xml
<profile>
  <id>flatClasspath</id>
  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
        <executions>
          <execution>
            <goals>
              <goal>repackage</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
      <plugin>
        <groupId>com.spotify</groupId>
        <artifactId>dockerfile-maven-plugin</artifactId>
        <version>1.4.13</version>
        <executions>
          <execution>
            <id>default</id>
            <goals>
              <goal>build</goal>
            </goals>
          </execution>
        </executions>
        <configuration>
          <repository>docker.io/bhuwanupadhyay/${project.artifactId}-flat-classpath</repository>
          <dockerfile>${project.basedir}/src/main/docker/flat-classpath.dockerfile</dockerfile>
          <tag>${project.version}</tag>
        </configuration>
      </plugin>
    </plugins>
  </build>
</profile>
```

To build and test run the following command:

```bash
# Build a docker image
mvn clean install -PflatClasspath

# Run app
docker run -d -p8081:8080 docker.io/bhuwanupadhyay/spring-boot-docker-containerization-flat-classpath:0.0.1-SNAPSHOT

# Test API
curl http://localhost:8081/names/hurry

# Output
{ "givenName": "hurry"}
```

### Using layertools 

Dockerfile to create a docker image of spring boot application with a layertools.

```dockerfile
FROM adoptopenjdk:11.0.7_10-jre-hotspot as builder
WORKDIR /app
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
RUN java -Djarmode=layertools -jar app.jar extract

FROM adoptopenjdk:11.0.7_10-jre-hotspot
WORKDIR /app
COPY --from=builder app/dependencies/ ./
COPY --from=builder app/spring-boot-loader/ ./
COPY --from=builder app/snapshot-dependencies/ ./
COPY --from=builder app/application/ ./
ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]
``` 

A maven profile to build a docker image with plugins: `spring-boot-maven-plugin` and `dockerfile-maven-plugin`. 

```xml
<profile>
  <id>layertools</id>
  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
        <configuration>
          <layers>
            <enabled>true</enabled>
          </layers>
        </configuration>
      </plugin>
      <plugin>
        <groupId>com.spotify</groupId>
        <artifactId>dockerfile-maven-plugin</artifactId>
        <version>1.4.13</version>
        <executions>
          <execution>
            <id>default</id>
            <goals>
              <goal>build</goal>
            </goals>
          </execution>
        </executions>
        <configuration>
          <repository>docker.io/bhuwanupadhyay/${project.artifactId}-layertools</repository>
          <dockerfile>${project.basedir}/src/main/docker/layertools.dockerfile</dockerfile>
          <tag>${project.version}</tag>
        </configuration>
      </plugin>
    </plugins>
  </build>
</profile>
```

To build and test run the following command:

```bash
# Build a docker image
mvn clean install -Playertools

# Run app
docker run -d -p8082:8080 docker.io/bhuwanupadhyay/spring-boot-docker-containerization-layertools:0.0.1-SNAPSHOT

# Test API
curl http://localhost:8082/names/hurry

# Output
{ "givenName": "hurry"}
```

### Using Buildpacks.

A maven profile to build docker image with plugins: `spring-boot-maven-plugin`. 

```xml
<profile>
  <id>buildpacks</id>
  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
        <executions>
          <execution>
            <goals>
              <goal>build-image</goal>
            </goals>
            <configuration>
              <imageName>docker.io/bhuwanupadhyay/${project.artifactId}-buildpacks:${project.version}</imageName>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</profile>
```

To build and test run the following command:

```bash
# Build a docker image
mvn clean install -Pbuildpacks

# Run app
docker run -d -p8083:8080 docker.io/bhuwanupadhyay/spring-boot-docker-containerization-buildpacks:0.0.1-SNAPSHOT

# Test API
curl http://localhost:8083/names/hurry

# Output
{ "givenName": "hurry"}
```

We are done, Thanks for reading! [Github](https://github.com/BhuwanUpadhyay/spring-boot-docker-containerization)

## References
- https://docs.spring.io/spring-boot/docs/2.3.0.RELEASE/reference/htmlsingle
- https://github.com/spotify/dockerfile-maven
