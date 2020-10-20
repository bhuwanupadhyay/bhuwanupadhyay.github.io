---
title: Semantic versioning on docker build and helm chart
date: 2020-06-22 16:10:00 Z
categories: [SemanticVersioning]
tags: [helm, docker, semantic-versioning]
author: Bhuwan Prasad Upadhyay
github_repo: https://github.com/BhuwanUpadhyay/semantic-versioning-on-docker-build-and-helm-chart
---

Helm best practice guide advocate semantic versioning for the helm chart that your release for deployment. Wherever possible, Helm uses [SemVer 2](https://semver.org/) to represent version numbers. Semantic versioning is a meaningful method for incrementing version numbers. So, today we will explore how to release helm charts and docker build by using semantic versioning convention.

<i class="fa fa-info text-danger"></i> This example needs `kubectl` `minikube` `helm` command-line tools in your machine so for the setup you can see my previous note: [minikube setup and helm deployment]({{ site.baseurl }}/posts/minikube-setup-and-helm-deployment/).
In this example, I will release semantic versions for helm chart and docker image of spring boot microservice that build upon maven.

## Docker build and Helm chart for spring boot

Let's start with spring boot a simple microservice that exposes API to return a given name.

### Initialize Project

```shell
NAME='Semantic versioning on docker build and helm chart' && PRJ=semantic-versioning-on-docker-build-and-helm-chart && \
mkdir -p $PRJ && cd $PRJ && \
curl https://start.spring.io/starter.tgz \
    -d dependencies=actuator,webflux \
    -d groupId=io.github.bhuwanupadhyay -d artifactId=$PRJ -d packageName=io.github.bhuwanupadhyay.example \
    -d applicationName=SpringBoot -d name=$NAME -d description=$NAME \
    -d language=kotlin -d platformVersion=2.3.1.RELEASE -d javaVersion=11 \
    -o demo.tgz && \
    tar -xzvf demo.tgz && rm -rf demo.tgz
```

### Create API to return given name [`Github`]({{page.github_repo}}/blob/master/src/main/kotlin/io/github/bhuwanupadhyay/example/NameManager.kt){:target="_blank"}

```kotlin
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

### Dockerfile for spring boot `src/main/docker/Dockerfile`

From Spring Boot 2.3.0.RELEASE they introduced [layertools](https://docs.spring.io/spring-boot/docs/2.3.0.RELEASE/reference/htmlsingle/#layering-docker-images) to create optimized Docker images that can be built with a dockerfile.

<script type="text/javascript" charset="UTF-8" src="https://gist-it.appspot.com/github.com/BhuwanUpadhyay/semantic-versioning-on-docker-build-and-helm-chart/blob/master/src/main/docker/Dockerfile?footer=minimal"></script>

### Helm chart for spring boot `src/main/helm/my-service`

Simply run the create helm command.

```shell
mkdir -p src/main/helm && helm create src/main/helm/my-service
```

In your helm chart under `src/main/helm/my-service/templates/deployment.yaml` change `readinessProbe` and `livenessProbe` health check settings, also modify container port to `8080` is the default for spring boot application.

<script type="text/javascript" charset="UTF-8" src="https://gist-it.appspot.com/github.com/BhuwanUpadhyay/semantic-versioning-on-docker-build-and-helm-chart/blob/master/src/main/helm/my-service/templates/deployment.yaml?footer=minimal&slice=35:55"></script>

Also, replace value image repository with your published docker image name without a tag in `src/main/helm/my-service/values.yaml` inside a helm chart.

```yaml
image:
  repository: docker.io/bhuwanupadhyay/my-service
  pullPolicy: IfNotPresent
  tag: ""  
```

In `src/main/helm/my-service/Chart.yaml` there are two properties:
 - `version` [chart version] - Versions are expected to follow Semantic Versioning (https://semver.org/)
    
    This is the chart version. This version number should be incremented each time you make changes. 
 
 - `appVersion` [default value for image tag] - Versions are expected to follow Semantic Versioning (https://semver.org/)
    
    This is the version number of the application being deployed. This version number should be incremented each time you make changes to the application.

In maven [pom.xml](https://github.com/BhuwanUpadhyay/semantic-versioning-on-docker-build-and-helm-chart/blob/master/pom.xml), 
the `flatten-maven-plugin` to set revision number for project, 
which will use by `dockerfile-maven-plugin` to build the docker image `repository/image-name:<revision>` with revision
and `helm-maven-plugin` create package with that revision for chart version and appVersion.  

It's very important to provide consistent releases during the life cycle of the product. To achieve this we will use very popular
tool [Semantic Release](https://semantic-release.gitbook.io/) with [Conventional Commits](https://www.conventionalcommits.org/).

If you want to know more about Semantic versioning or how to use it with git, please read my previous blog [post]({{site.baseurl}}/posts/applying-semantic-versioning-with-git-repository/).

## Semantic Release Process

![](/assets/images/semantic-release-process.png)

## Github Pipeline

Let's create [semantic-release configuration](https://semantic-release.gitbook.io/semantic-release/usage/configuration) `.releaserc` file in your project directory.
In this configuration, we have two commands given by [@semantic-release/exec](https://github.com/semantic-release/exec) plugin that we will use to a build the release and publish helm package on [GitHub Packages]({{page.github_repo}}/packages) and docker image on [docker.io](https://hub.docker.com/).

- `prepareCmd`: The shell command to execute during the prepare step.
- `publishCmd`: The shell command to execute during the publish step.

<script type="text/javascript" charset="UTF-8" src="https://gist-it.appspot.com/github.com/BhuwanUpadhyay/semantic-versioning-on-docker-build-and-helm-chart/blob/master/.releaserc?footer=minimal"></script>

Here `bot.sh` is a script file used to `build` and `publish` the docker image and helm chart package using the next version given by a semantic release process.

<script type="text/javascript" charset="UTF-8" src="https://gist-it.appspot.com/github.com/BhuwanUpadhyay/semantic-versioning-on-docker-build-and-helm-chart/blob/master/bot.sh?footer=minimal"></script>

Finally, we need a workflow action YAML configuration to run the Github pipeline under `.github/workflows/build.yml`.

<script type="text/javascript" charset="UTF-8" src="https://gist-it.appspot.com/github.com/BhuwanUpadhyay/semantic-versioning-on-docker-build-and-helm-chart/blob/master/.github/workflows/build.yml?footer=minimal"></script>

We are done ! Thanks for reading. [Github]({{page.github_repo}}){:target="_blank"}

## References
- https://docs.spring.io/spring-boot/docs/2.3.0.RELEASE/reference/htmlsingle/#layering-docker-images
