---
title: Safely Exchanging Information Across Microservices
date: 2020-05-09 00:00:00 Z
categories: [SpringCloud]
tags: [spring-cloud-stream, avro-schema, schema-registry]
author: Bhuwan Prasad Upadhyay
image: https://raw.githubusercontent.com/BhuwanUpadhyay/12-safely-exchanging-information-across-microservices/master/assets/featured.png
---

Exchanging information between the microservices without breaking existing functionalities is a challenging task specially if your business model evolve with the time. It's very important to ensure new updates on the microservices should be seamless and have backward compatibility.

Specially, if our microservices communicate each other by using pub/sub architecture and have multiple
producers and consumers, it is necessary for all those microservices to agree on a contract that is based on a schema. Because, to accommodate new business requirements the message payload structure might needs to evolve, and the existing components are still required to continue to work.

 In this article, I will take you through how we can used evolving schemas to exchange information between microservices using Spring Boot and Spring Cloud.

## Avro Schema

Avro is used to define the schema for a message's payload. This schema describes the fields allowed in the payload, along with their data types. Avro bindings are used to serialize values before writing them, and to deserialize values after reading them. The usage of these bindings requires your applications to use the Avro data format, which means that each payload is associated with a schema.

In addition, Avro makes use of the Jackson APIs for parsing JSON. This is likely to be of interest to you if you are familiar with a JSON-based system.

## Schema Registry

For evolving schemas, we need to register them somewhere to share between the microservices without
any manual updates. This leads to the consumers have to read schema definitions from a registry and publisher needs to provide schema definitions to a registry. To address this philosophy, the concept of schema registry come into the picture.

> [Spring Cloud Schema Registry](https://spring.io/projects/spring-cloud-schema-registry) provides support for schema evolution so that the data can be evolved over time and still work with older or newer producers and consumers and vice versa.

## Schema Registry Server

To use Spring Cloud Schema Registry Server in a Maven [Spring Boot](https://start.spring.io/) projects, we need to have `spring-cloud-schema-registry-server` from Spring Cloud in the project pom.xml:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-schema-registry-server</artifactId>
</dependency>
```

To enable schema registry server in spring boot, we need to use the annotation `@EnableSchemaRegistryServer` on the main application class:

<script type="text/javascript" charset="UTF-8" src="https://gist-it.appspot.com/github.com/BhuwanUpadhyay/12-safely-exchanging-information-across-microservices/blob/master/schema-registry/src/main/java/io/github/bhuwanupadhyay/SchemaRegistryApplication.java?footer=minimal&slice=2:"></script>

Schema registry server uses `8990` as a default port for application and the example of http `GET` request to fetch schemas by its id will be looks like below:

```bash
curl -X GET http://localhost:8990/schemas/<id>
```

## Schema Registry Client

To use Spring Cloud Schema Registry Client in a Maven [Spring Boot](https://start.spring.io/) projects, we need to have `spring-cloud-schema-registry-client` from Spring Cloud in the project pom.xml:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-schema-registry-client</artifactId>
</dependency>
```

To enable schema registry client in spring boot, we need to use the annotation `@EnableSchemaRegistryCLient` on the main application class:

<script type="text/javascript" charset="UTF-8" src="https://gist-it.appspot.com/github.com/BhuwanUpadhyay/12-safely-exchanging-information-across-microservices/blob/master/order-service/src/main/java/io/github/bhuwanupadhyay/order/OrderServiceApplication.java?footer=minimal&slice=2:"></script>

## Story -- Exchanging Information

Let's consider we have to exchange messages between **Order Service** and **Payment Service**. Order and Payment microservices solution is below:

![Image](https://raw.githubusercontent.com/BhuwanUpadhyay/12-safely-exchanging-information-across-microservices/master/assets/example.png)

As an above diagram, we have three domain events that are **OrderPlaced**, **PaymentRequested** and **Payment Received**. A sequence diagram for order workflow as below:

![](/assets/images/safely-exhange-message-order-domain.png)

## Create Avro Schemas

Let's create first version avro schemas for **PaymentRequested** and **Payment Received** because those domain events are not consumed inside bounded context.

- **PaymentRequestedV1** will publish by [Order Service](https://github.com/BhuwanUpadhyay/12-safely-exchanging-information-across-microservices/blob/master/order-service)

<script type="text/javascript" charset="UTF-8" src="https://gist-it.appspot.com/github.com/BhuwanUpadhyay/12-safely-exchanging-information-across-microservices/blob/master/schemas/src/main/resources/avro/PaymentRequested.v1.avsc?footer=minimal&slice"></script>

- **PaymentReceivedV1** will publish by [Payment Service](https://github.com/BhuwanUpadhyay/12-safely-exchanging-information-across-microservices/blob/master/payment-service)

<script type="text/javascript" charset="UTF-8" src="https://gist-it.appspot.com/github.com/BhuwanUpadhyay/12-safely-exchanging-information-across-microservices/blob/master/schemas/src/main/resources/avro/PaymentReceived.v1.avsc?footer=minimal&slice"></script>

After sometime, business need to add `customerId` attribute on **PaymentRequested** message payload for **Order Service**. To do so we need to define a second version of schema i.e. **PaymentRequestedV2** and use it in the **Order Service**.

- **PaymentRequestedV2** will publish by [Order Service](https://github.com/BhuwanUpadhyay/12-safely-exchanging-information-across-microservices/blob/master/order-service) after upgrade.

<script type="text/javascript" charset="UTF-8" src="https://gist-it.appspot.com/github.com/BhuwanUpadhyay/12-safely-exchanging-information-across-microservices/blob/master/schemas/src/main/resources/avro/PaymentRequested.v2.avsc?footer=minimal&slice"></script>

## Order Service

Once schema upgraded for a message **PaymentRequested** then order service will publish the latest version i.e. **V2** to their consumers:  

<script type="text/javascript" charset="UTF-8" src="https://gist-it.appspot.com/github.com/BhuwanUpadhyay/12-safely-exchanging-information-across-microservices/blob/master/order-service/src/main/java/io/github/bhuwanupadhyay/order/application/internal/outboundservices/OrderEventPublisherService.java?footer=minimal&slice=20:33"></script>

## Payment Service

Still, payment service is consuming old version i.e. **V1**  for a message **PaymentRequested**:

<script type="text/javascript" charset="UTF-8" src="https://gist-it.appspot.com/github.com/BhuwanUpadhyay/12-safely-exchanging-information-across-microservices/blob/master/payment-service/src/main/java/io/github/bhuwanupadhyay/payment/interfaces/events/PaymentEventHandler.java?footer=minimal&slice=22:31"></script>

## Run Example

Use [docker-compose.yaml](https://github.com/BhuwanUpadhyay/12-safely-exchanging-information-across-microservices/blob/master/docker-compose.yaml) to run necessary infrastructure for microservices.

```bash
docker-compose up
```

Clone [Example Github Project](https://github.com/BhuwanUpadhyay/12-safely-exchanging-information-across-microservices) in your directory.

- Build

```bash
  make pull && make build
```

- Run Schema Registry -- (On New Terminal)

```bash
  make pull && make build
```

- Run Order Service -- (On New Terminal)

```bash
  make order_service
```

- Run Payment Service -- (On New Terminal)

```bash
  make payment_service
```

- Run Test - Perform following http request to test microservices.

```http
### Create New Order
POST http://localhost:8080/orders
Content-Type: application/json

{
  "itemId": "ITM00001",
  "quantity": 20,
  "customerId": "CUST00001"
}

### Get orders
GET http://localhost:8080/orders

### Get payments
GET http://localhost:8081/payments
```

## Conclusion

In the above example, microservices were able to communicate between each other seamlessly even we applied schema changes on producer service but not on consumer service.

Finally, we can exchange information between microservices safely by using schema registry and agree upon some sort of contracts between the microservice for message payloads.
