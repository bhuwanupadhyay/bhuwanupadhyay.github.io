---
title: Domain-Driven Design Building Blocks
date: 2020-05-02 15:23:26 Z
categories: [Domain-Driven-Design]
tags: [value-object, entity, aggregate-root, repository, factory, domain-service]
author: Bhuwan Prasad Upadhyay
image: https://raw.githubusercontent.com/BhuwanUpadhyay/17-ddd-building-blocks/master/assets/featured.png
---

Domain-driven design (DDD), is an approach used to build systems that have a complex business domain.
So you wouldn’t apply DDD to, say, infrastructure software or building routers, proxies, or caching layers, 
but instead to business software that solves real-world business problems.
It’s a great technique for separating the way the business is modeled from the plumbing code that ties it all together.
Separating these two in the software itself makes it easier to design, model, build and evolve an implementation over time.
        
In tactical DDD, the building blocks play an important role in how business is modeled into the code.
In this article, I will take you through the best available options for building blocks in object-oriented principles.

## Value Object

> An object that represents a descriptive aspect of the domain with no conceptual identity is called a Value Object. Value Objects are instantiated to represent elements of the design that we care about only for what they are, not who or which they are.
> — **Eric Evans**

In other words, value objects don’t have their own identity. The value object possess concept of structural equality 
— if two objects are equal then they have equivalent content. 
Also, If two value objects have the same set of attributes we can treat them interchangeably.

### Attributes
- No Identity - value objects are identity-less.
- Immutable - value object can be replaced by another value object with same content. To make sure, equality by structure for value object is by using Immutable design especially in multi-thread scenarios (immutable objects are threadsafe by design).  
- Lifespan - can’t exist without a parent entity (should not have separate table in a database).
- Business Constraints - value object is always valid (should need to validate business rules on creation). 
I always prefer to validate business constraints on the `constructor` for the value object.

### Code Example

Equality logic implementation for value object is very important to ensure they are equals by its content.
It’s too easy to forget to override `equals()` and `hashCode()` in the value object.
It's very important to make sure all properties of value object should be part of equality logic. 

To enforce equality logic for value object I used following class as a base class for every value object.  

<script type="text/javascript" charset="UTF-8" src="https://gist-it.appspot.com/github.com/BhuwanUpadhyay/17-ddd-building-blocks/blob/master/ddd-core/src/main/java/io/github/bhuwanupadhyay/ddd/ValueObject.java?footer=minimal&slice=2:"></script>

## Entity
> Many objects are not fundamentally defined by their attributes, but rather by a thread of continuity and identity.
> — **Eric Evans**

The entity possess concept of identifier equality — Two instances of entity would be equal if they have the same identifiers. 
We can change everything related to an entity (except its identifier), and after modification also it remains the same entity.

### Code Example

Identifier equality means that entity class has a field for an identifier. 
Following class can be used as a base class for entity class.

<script type="text/javascript" charset="UTF-8" src="https://gist-it.appspot.com/github.com/BhuwanUpadhyay/17-ddd-building-blocks/blob/master/ddd-core/src/main/java/io/github/bhuwanupadhyay/ddd/Entity.java?footer=minimal&slice=2:"></script>

## Domain Event
> The essence of a Domain Event is that you use it to capture things that can trigger a change to the state of the application you are developing.
> — **Martin Fowler**

A domain event is, something that happened in the domain that you want notify to other parts of the same domain. 
The important benefit of domain events is that side effects can be expressed explicitly.

![](https://raw.githubusercontent.com/BhuwanUpadhyay/17-ddd-building-blocks/master/assets/aggregate_transaction.png)

### Code Example

According to nature side effects, the published domain event can be listened inside same bounded context or another bounded context.
Following class can be used as a base class for domain event.

<script type="text/javascript" charset="UTF-8" src="https://gist-it.appspot.com/github.com/BhuwanUpadhyay/17-ddd-building-blocks/blob/master/ddd-core/src/main/java/io/github/bhuwanupadhyay/ddd/DomainEvent.java?footer=minimal&slice=2:"></script>

## Aggregate Root
> An AGGREGATE is a cluster of associated objects that we treat as a unit for the purpose of data changes. Each aggregate has a root and a boundary.
> — **Eric Evans**

- Group the entities and value objects into aggregates and define boundaries around each. 
- Control all access to the objects inside the boundary through the root. 
- Allow external objects to hold references to the root only. 
- Register domain events into the aggregate root. 

### Code Example

The example of base class for aggregate root in my tactical ddd implementation as below.

<script type="text/javascript" charset="UTF-8" src="https://gist-it.appspot.com/github.com/BhuwanUpadhyay/17-ddd-building-blocks/blob/master/ddd-core/src/main/java/io/github/bhuwanupadhyay/ddd/AggregateRoot.java?footer=minimal&slice=2:"></script>

 
## Repositories

A repository is a service that uses a global interface to provide access to all entities and value objects that are within a particular aggregate collection.

### Code Example
From a repository, we have to publish all domain events when persist the aggregate root and then detached domain events from the aggregate root.
You can use following code to extend your domain repository in your project.

<script type="text/javascript" charset="UTF-8" src="https://gist-it.appspot.com/github.com/BhuwanUpadhyay/17-ddd-building-blocks/blob/master/ddd-core/src/main/java/io/github/bhuwanupadhyay/ddd/DomainRepository.java?footer=minimal&slice=0:20"></script>

## Domain Service

> Some concepts from the domain aren’t natural to model as objects…a service tends to be named for an activity, rather than an entity — a verb rather than a noun.
> — **Eric Evans**

A service that expresses a business logic that is not part of any Aggregate Root.

> When an operation does not conceptually belong to any object. Following the natural contours of the problem, you can implement these operations in services.
> — **Wikipedia**

Some business rules don't make sense to be part of an Aggregate.  If something is 'outside' an Aggregate, then it's probably is a Domain Service.

## Factories

Factories are often used to create Aggregates.
Aggregates provide encapsulation and a consistent boundary around a group of objects.
Aggregates are important because they enforce the internal consistency of the object they are responsible for.

A Factory can be useful when creating a new Aggregate because it will encapsulate the knowledge required to create an Aggregate in a consistent state and with all invariants enforced.
