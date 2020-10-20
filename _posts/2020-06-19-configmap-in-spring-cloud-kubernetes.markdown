---
title: Configmap in Spring Cloud Kubernetes
date: 2020-06-19 11:51:00 Z
categories: [SpringCloud]
tags: [helm, kubernetes, configmap]
author: Bhuwan Prasad Upadhyay
github_repo: https://github.com/BhuwanUpadhyay/configmap-in-spring-cloud-kubernetes
---

For the integration of Spring Cloud and Spring Boot applications that are running inside Kubernetes, we can use 
[Spring Cloud Kubernetes](https://cloud.spring.io/spring-cloud-kubernetes/) 
which provides Spring Cloud common interface implementations that are easy to use and ready for production.

In k8s deployment, [configmap](https://kubernetes.io/docs/concepts/configuration/configmap/) can be used as a source for application configuration properties.
In this article, I will take you through how to use spring cloud Kubernetes to provide configuration properties for spring boot applications in k8s deployment using congfigmap.

This example needs `kubectl` `minikube` `helm` command-line tools in your machine so for the setup you can see my previous note: [minikube setup and helm deployment]({{ site.baseurl }}/posts/minikube-setup-and-helm-deployment/).

## Create Microservice

Let's create one simple spring boot microservice that manages orders coming from a customer. In my example, order microservice exposes basic CRUD operations APIS, 
along with [Spring Webflux](https://docs.spring.io/spring/docs/current/spring-framework-reference/web-reactive.html) I used [Spring Data R2DBC](https://github.com/spring-projects/spring-data-r2dbc) for data access and Kotlin language. 

### Initialize Project

```shell
NAME='Configmap in Spring Cloud Kubernetes' && PRJ=configmap-in-spring-cloud-kubernetes && \
mkdir -p $PRJ && cd $PRJ && \
curl https://start.spring.io/starter.tgz \
    -d dependencies=actuator,webflux,cloud-starter,data-r2dbc,h2,postgresql \
    -d groupId=io.github.bhuwanupadhyay -d artifactId=$PRJ -d packageName=io.github.bhuwanupadhyay.example \
    -d applicationName=SpringBoot -d name=$NAME -d description=$NAME \
    -d language=kotlin -d platformVersion=2.3.1.RELEASE -d javaVersion=11 \
    -o demo.tgz && \
    tar -xzvf demo.tgz && rm -rf demo.tgz
```

### Basic CRUD operations APIS [`Github`]({{page.github_repo}}/blob/master/src/main/kotlin/io/github/bhuwanupadhyay/example/OrderManager.kt){:target="_blank"}

```kotlin
@Table("ORDERS")
data class OrderEntity(@Id var id: Long?, var item: String, var quantity: Int)

@Configuration
class OrderRoutes(private val handler: OrderHandler) {

    @Bean
    fun router() = router {
        accept(APPLICATION_JSON).nest {
            POST("/orders", handler::save)
            GET("/orders", handler::findAll)
            GET("/orders/{id}", handler::findOne)
            PUT("/orders/{id}", handler::update)
        }
    }
}
```

## Containerizing Spring Boot Application

From Spring Boot [2.3.0.RELEASE](https://docs.spring.io/spring-boot/docs/2.3.0.RELEASE/maven-plugin/reference/html/#build-image)
the maven plugin of spring boot by default support `build-image` goal during execution which creates an [OCI image](https://github.com/opencontainers/image-spec) using [Cloud Native Buildpacks](https://buildpacks.io/).

```xml
<plugin>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-maven-plugin</artifactId>
  <executions>
   <execution>
     <goals>
      <goal>build-image</goal>
     </goals>
     <configuration>
      <imageName>docker.io/bhuwanupadhyay/${project.artifactId}:${project.version}</imageName>
     </configuration>
   </execution>
  </executions>
</plugin>
``` 

Run `mvn clean install` : spring boot maven plugin will create a docker image. The end part of the output log:
```shell
[INFO] 
[INFO] Successfully built image 'docker.io/bhuwanupadhyay/configmap-in-spring-cloud-kubernetes:0.0.1-SNAPSHOT'
[INFO]
``` 
To publish docker image in the registry run the following command
```shell
docker push docker.io/bhuwanupadhyay/configmap-in-spring-cloud-kubernetes:0.0.1-SNAPSHOT
```

## Helm Chart

To create a helm chart from your project directory run the following command.

```shell
helm create src/helm-chart
```

Replace value image repository and tag with your published docker image name and tag in `src/helm-chart/values.yaml` inside the helm chart.

```yaml
image:
  repository: docker.io/bhuwanupadhyay/configmap-in-spring-cloud-kubernetes
  pullPolicy: IfNotPresent
  tag: "0.0.1-SNAPSHOT"
```

Order microservice required data source configuration properties in the deployment to connect with the database to store order information.

Let's say we have two spring profiles `dev` and `prod`. In `dev` profile we want to run an application using `h2` database while `prod` profile we want to run an application using `postgresql` database.
H2 is an embedded database so no need to create different pod instance, but it is not the case for postgresql because it is not embeddable, 
so we need to run in a different pod. 

To run `postgresql` in helm deployment you need to add dependency inside helm chart `src/helm-chart/Chart.yaml`.

```yaml
dependencies:
  - name: postgresql
    alias: springbootdb
    version: 8.10.5
    repository: https://charts.bitnami.com/bitnami
```


In Kubernetes config map is used to provide application configuration properties. 
To add configmap in your helm chart simply create a new YAML file in `src/helm-chart/templates/configmap.yaml`. 
For our example, we have to define multiple spring profiles for h2 and postgresql that are given below:

<script type="text/javascript" charset="UTF-8" src="https://gist-it.appspot.com/github.com/BhuwanUpadhyay/configmap-in-spring-cloud-kubernetes/blob/master/src/helm-chart/templates/configmap.yaml?footer=minimal"></script>

## Spring Cloud Kubernetes

Add dependency that used to load application properties from Kubernetes ConfigMaps and Secrets or reload application properties when a ConfigMap or Secret changes.
```xml
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-kubernetes-config</artifactId>
</dependency>
```
 
You need to make sure a pod that runs with `spring-cloud-Kubernetes` has access to the Kubernetes API. 
Using helm you can create a service account with `Role` and `RoleBinding` that has access to read configmaps and secrets by defining new yaml file in `src/helm-chart/templates/cluster-reader.yaml`.

<script type="text/javascript" charset="UTF-8" src="https://gist-it.appspot.com/github.com/BhuwanUpadhyay/configmap-in-spring-cloud-kubernetes/blob/master/src/helm-chart/templates/cluster-reader.yaml?footer=minimal"></script>

Finally, you need to provide environment variables to tell the application to use your configmap and namespace for spring cloud Kubernetes. 
Also, In your helm chart under `src/microservice/templates/deployment.yaml` change `readinessProbe` and `livenessProbe` health check settings, also modify container port to `8080` is the default for spring boot application.

<script type="text/javascript" charset="UTF-8" src="https://gist-it.appspot.com/github.com/BhuwanUpadhyay/configmap-in-spring-cloud-kubernetes/blob/master/src/helm-chart/templates/deployment.yaml?footer=minimal&slice=35:62"></script>

## Deployment

Get ready for the deployment!

#### Start Minikube
```shell
minikube start
```

#### Add helm repositories
```shell
helm repo add bitnami https://charts.bitnami.com/bitnami
```

#### Update charts
```shell
helm dependency update src/helm-chart
```

#### `DEV` Profile helm deployment
```shell
helm upgrade \
    --install -f src/helm-chart/values.yaml \
    --set spring.profiles.active=dev \
    --set springbootdb.enabled=false \
    example-deployment src/helm-chart --force
```

#### `PROD` Profile helm deployment
```shell
helm upgrade \
    --install -f src/helm-chart/values.yaml \
    --set spring.profiles.active=prod \
    --set springbootdb.enabled=true \
    --set springbootdb.postgresqlDatabase=orders-db \
    --set springbootdb.postgresqlUsername=user \
    --set springbootdb.postgresqlPassword=password \
    --set springbootdb.persistence.enabled=false \
    example-deployment src/helm-chart --force
```

#### Watch the deployment
```shell
watch kubectl get pods
```

#### Test Order Microservice APIS
Firstly, install `httpie` command line tool.
```shell
sudo apt install httpie
```

`Open New Terminal` - Port forward for order microservice

```
# Get pods -> Run the following command
kubectl get pods

# Output
NAME                                            READY   STATUS    RESTARTS   AGE
example-deployment-helm-chart-8ff55d4db-rnt6j   1/1     Running   0          12m
example-deployment-springbootdb-0               1/1     Running   0          12m

# Port forward -> Run the following command

kubectl port-forward example-deployment-helm-chart-8ff55d4db-rnt6j 8080:8080
```

`Open New Terminal` - Call order service APIS

```shell
# POST orders

echo '{"item": "k8s-item", "quantity": 20}' | http POST :8080/orders

# GET orders
http :8080/orders
```

We are done ! Thanks for reading. [Github]({{page.github_repo}}){:target="_blank"} 

## References
- Versions: `helm - v3.2.3` `minikube - v1.11.0` `kubectl - v1.17.0`
- https://docs.spring.io/initializr/docs/current/reference/html/
- https://buildpacks.io/
- https://docs.spring.io/spring-boot/docs/2.3.0.RELEASE/maven-plugin/reference/html/
- https://spring.io/projects/spring-cloud-kubernetes
