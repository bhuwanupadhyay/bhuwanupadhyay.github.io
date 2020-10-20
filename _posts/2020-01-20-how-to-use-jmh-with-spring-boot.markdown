---
title: How to use jmh with spring boot
date: 2020-01-20 00:00:00 Z
categories: [SpringBoot]
tags: [jmh]
author: Bhuwan Prasad Upadhyay
image: /assets/images/how-to-use-jmh-with-spring-boot/featured.png
---

Application performance is a concern of every developer working with software. 
JMH provides an easy API for developer to benchmark application performance.

In this post, I’ll walk you through to how to use jmh with spring boot.
You’ll learn how to set up Spring Boot Test and how to enable jmh benchmark in your test code.

#### Start PostgreSQL

You can download [docker-compose.yml](https://github.com/BhuwanUpadhyay/9-how-to-use-jmh-with-spring-boot/blob/master/docker-compose.yml){:target="_blank"} file and just run the following to get PostgreSQL and Vault started locally in docker containers.

```bash
docker-compose up -d
```

#### Create a Spring Boot Project

```bash
curl https://start.spring.io/starter.tgz -d dependencies=postgresql,data-jpa,data-rest,lombok \
  -d groupId=io.github.bhuwanupadhyay \
  -d artifactId=example \
  -d packageName=io.github.bhuwanupadhyay.tutorial \
  -d baseDir=example \
  -d bootVersion=2.2.2.RELEASE | tar -xzvf -
cd example
```

#### Add JMH dependencies in `pom.xml`

{% highlight xml %}
<dependency>
    <groupId>org.openjdk.jmh</groupId>
	<artifactId>jmh-core</artifactId>
	<version>1.22</version>
	<scope>test</scope>
</dependency>
<dependency>
	<groupId>org.openjdk.jmh</groupId>
	<artifactId>jmh-generator-annprocess</artifactId>
	<version>1.22</version>
	<scope>test</scope>
</dependency>
{% endhighlight %}
 
#### Let's add some classes for benchmark  

- `OrderLine` entity class

{% highlight java %}
@Entity
@Getter
@Setter
@ToString(exclude = {"itemId", "addressLine", "quantity"})
@EqualsAndHashCode(exclude = {"itemId", "addressLine", "quantity"})
class OrderLine implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long itemId;
    private String addressLine;
    private Integer quantity;

}
{% endhighlight %}

- `OrderLineRepository` repository class for an entity `OrderLine`

{% highlight java %}
interface OrderLineRepository extends JpaRepository<OrderLine, Long> {

}
{% endhighlight %}

- The spring boot configuration `application.properties`

```properties
spring.application.name=example
spring.datasource.driver-class-name=org.postgresql.Driver
spring.datasource.url=jdbc:postgresql://localhost:5432/postgres
spring.datasource.username=user
spring.datasource.password=password
spring.jpa.hibernate.ddl-auto=create-drop
```

#### JMH Benchmark Code

Let's benchmark insert operation for `OrderLine` entity into database.

{% highlight java %}
@SpringBootTest
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class DemoApplicationTests {

	private static OrderLineRepository repository;

	@Autowired
	public void setRepository(OrderLineRepository repository) {
		DemoApplicationTests.repository = repository;
	}

	@Test
	public void runBenchmarks() throws Exception {
		Options opts = new OptionsBuilder()
				// set the class name regex for benchmarks to search for to the current class
				.include("\\." + this.getClass().getSimpleName() + "\\.")
				.warmupIterations(3)
				.measurementIterations(3)
				// do not use forking or the benchmark methods will not see references stored within its class
				.forks(0)
				// do not use multiple threads
				.threads(1)
				.shouldDoGC(true)
				.shouldFailOnError(true)
				.jvmArgs("-server")
				.build();

		new Runner(opts).run();
	}

	@Benchmark
	public void dbInserts(Parameters parameters) {
		int size = Integer.parseInt(parameters.batchSize);

		for (int i = 0; i < size; i++) {
			OrderLine line = new OrderLine();
			line.setAddressLine("Jhamsikhel Ward #3, Arun Thapa Chwok, Lalitpur, Nepal");
			line.setItemId(1L);
			line.setQuantity(i);
			repository.save(line);
		}
	}

	@State(value = Scope.Benchmark)
	public static class Parameters {

		@Param({"1", "1000"})
		String batchSize;
	}

}
{% endhighlight %}

#### Benchmark Result for this example

![Test Output]({{ site.baseurl }}/assets/images/how-to-use-jmh-with-spring-boot/test-output.png) 

You can find example on github: [<i class="fab fa-github"></i> SourceCode](https://github.com/BhuwanUpadhyay/9-how-to-use-jmh-with-spring-boot){:target="_blank"}
