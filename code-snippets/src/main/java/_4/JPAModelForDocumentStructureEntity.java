package _4;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.*;

enum EmployeeStatus {
    ACTIVE, FIRED
}

interface EmployeeRepository extends JpaRepository<Employee, Long> {
}

@Embeddable
@Access(AccessType.FIELD)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
class Head {
    private String name;
    @Embedded
    private Code code;
}

@Embeddable
@Access(AccessType.FIELD)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
class Department {
    private String name;
    private Head head;
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<Address> addresses;
    @Embedded
    private Code code;
}

@Embeddable
@Access(AccessType.FIELD)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
class Code {
    private String code;
}

@Embeddable
@Access(AccessType.FIELD)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
class Address {
    private String addressLine;
    @Embedded
    private Code code;
}

@Entity
@Table(name = "EMPLOYEES")
@Access(AccessType.FIELD)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(of = "id")
@ToString(of = "id")
class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @Embedded
    private Code code;
    @Embedded
    private Department department;
    @ElementCollection(fetch = FetchType.EAGER)
    private Map<String, Integer> skills;
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<Address> addresses;
    @Enumerated(EnumType.STRING)
    private EmployeeStatus status;
    private LocalDate joiningDate;

    public Employee(String name,
                    Department department,
                    Map<String, Integer> skills,
                    Set<Address> addresses,
                    Code code) {
        this.name = name;
        this.department = department;
        this.skills = skills;
        this.addresses = addresses;
        this.joiningDate = LocalDate.now();
        this.status = EmployeeStatus.ACTIVE;
        this.code = code;
    }
}

@SpringBootApplication
@EnableJpaRepositories(considerNestedRepositories = true)
@Slf4j
public class JPAModelForDocumentStructureEntity {

    private final EmployeeRepository employeeRepository;

    public JPAModelForDocumentStructureEntity(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    public static void main(String[] args) {
        SpringApplication.run(JPAModelForDocumentStructureEntity.class, args);
    }

    @EventListener
    public void run(ApplicationReadyEvent readyEvent) {
        Map<String, Integer> skills = new HashMap<>();
        skills.put("Java", 90);
        skills.put("Python", 80);
        Address addressLine1 = new Address("addressLine1", new Code("1-CODE"));
        Address addressLine2 = new Address("addressLine2", new Code("2-CODE"));
        Set<Address> addresses1 = Set.of(addressLine1, addressLine2);
        Set<Address> addresses2 = Set.of(addressLine1, addressLine2);
        Head head = new Head("h-name", new Code("H-CODE"));
        Department department = new Department("d-name", head, addresses1, new Code("D-CODE"));
        Employee employee = new Employee("e-name", department, skills, addresses2, new Code("E-CODE"));

        // Save
        Employee saved = employeeRepository.save(employee);

        // Find by Id
        Employee findById = employeeRepository.findById(saved.getId()).get();

        log(findById);
    }

    private void log(Employee employee) {
        log.info("{}", employee);
        log.info("-----------------");
        log.info("Name: {}", employee.getName());
        log.info("Code: {}", employee.getCode());
        log.info("Department: {}", employee.getDepartment());
        log.info("Skills: {}", employee.getSkills());
        log.info("Addresses: {}", employee.getAddresses());
        log.info("Status: {}", employee.getStatus());
        log.info("Joining Date: {}", employee.getJoiningDate());
        log.info("-----------------");
    }
}
