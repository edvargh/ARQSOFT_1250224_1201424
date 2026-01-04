# 4. ADD First Iteration

## 4.1 Step 1 of ADD: Confirm There Is Sufficient Requirements Information

At the start of the first ADD iteration, sufficient requirements information was available to proceed with architectural design.  
The system requirements clearly stated the need to support multiple data models (relational, document, search) and DBMS technologies (SQL, MongoDB, Elasticsearch) with configuration-driven behavior at setup time. These concerns are foundational for achieving extensibility and configurability.  
The quality attribute scenarios and architectural tactics were already identified in the provided documentation (QualityAtributteScenarios.MD, Tactics.MD, TechnicalMemos.MD), making the persistence layer a suitable candidate for the first architectural iteration.

---

## 4.2 Step 2 of ADD: Choose an Element of the System to Decompose

For the first iteration, the chosen element to decompose was the persistence layer, specifically the capability to support multiple DBMS technologies based on external configuration.  
The persistence layer was selected first because:

- All core domain entities (Books, Authors, Readers, Lendings) require persistent storage.  
- The requirement to support SQL, MongoDB, and Elasticsearch simultaneously is a central extensibility goal.  
- Decoupling persistence logic early reduces risk and complexity in later migrations or additions of new data stores.  

---

## 4.3 Step 3 of ADD: Identify Candidate Architectural Drivers

### Functional Drivers

- The system must persist books, authors, readers, and lendings.  
- It must support multiple DBMS: SQL (MySQL/PostgreSQL), MongoDB, and Elasticsearch.  
- Redis must be used for caching regardless of the primary DBMS.  
- DBMS selection must be configurable at setup time without code changes.  

### Quality Drivers

- Extensibility: Add support for a new DBMS without modifying business logic.  
- Configurability: Switch DBMS via external configuration (YAML/JSON) without recompilation.  
- Maintainability: Clear separation between domain logic and persistence logic.  
- Performance: Use Redis to cache frequent queries and reduce DB load.  
- Reliability: Graceful degradation during DB failures (cache fallback, retry mechanisms).  
- Testability: Support for in-memory and test-specific persistence implementations.  

These drivers guided all design and implementation decisions in this iteration.

---

## 4.4 Step 4 of ADD: Choose a Design Concept that Satisfies the Architectural Drivers

The persistence layer was designed as a configurable, abstraction-based repository system using the following key concepts:

- Repository Pattern with common interfaces per entity  
- Dependency Injection to bind concrete implementations at runtime  
- Spring Profiles + Configuration Properties for setup-time configuration  
- Redis as a centralized cache layer  
- Strategy Pattern for selecting DB-specific repository implementations  
- Circuit Breaker + Retry for resilience against transient DB failures  

This design satisfies extensibility, configurability, and maintainability while ensuring performance and reliability.

---

## 4.4.1 Step 4, Substep 1 of ADD: Identify Design Concerns

The main design concerns identified were:

- How to abstract CRUD operations across different data models?  
- How to handle transactions in non-relational DBMS?  
- How to load and apply persistence configuration at startup?  
- How to integrate caching (Redis) transparently?  
- How to ensure data consistency between cache and multiple DBMS?  
- How to monitor health and performance across different persistence technologies?  

---

## 4.4.2 Step 4, Substep 2 of ADD: List Alternative Patterns for Subordinate Concerns

The following alternatives were considered:

- Repository Pattern vs Active Record  
- Static (compile-time) configuration vs dynamic (runtime) configuration  
- Centralized cache vs per-DB cache  
- Distributed transactions vs event-based compensation  
- Hardcoded DB connections vs connection pooling with health checks  

---

## 4.4.3 Step 4, Substep 3 of ADD: Select Patterns from the List

The following patterns were selected:

- Repository Pattern with standardized interfaces  
- Spring Configuration Properties for externalized setup  
- Redis as a shared cache layer  
- Strategy Pattern for runtime repository selection  
- Retry with Exponential Backoff + Circuit Breaker for DB resilience  
- Health Indicators for monitoring DB and cache status  

These patterns were chosen to balance extensibility, simplicity, and operational robustness.

---

## 4.4.4 Step 4, Substep 4 of ADD: Determine Relationship Between Patterns and Drivers

- Repository Pattern → Supports Extensibility and Maintainability  
- Spring Configuration → Enables Configurability  
- Redis Cache → Improves Performance  
- Circuit Breaker + Retry → Enhances Reliability  
- Strategy Pattern → Facilitates Runtime Configurability  
- Health Checks → Supports Operability  

Each pattern directly supports one or more architectural drivers.

---

## 4.4.5 Step 4, Substep 5 of ADD: Capture Preliminary Architectural Views

At this stage, the persistence layer consists of:

- Repository Interfaces (e.g., BookRepository, AuthorRepository)  
- Concrete Implementations (SQL, MongoDB, Elasticsearch variants)  
- PersistenceConfig – configuration class that binds implementations based on profiles  
- RedisCacheService – centralized caching service  
- DatabaseHealthIndicator – monitors DB and cache availability  
- RetryableRepositoryDecorator – adds retry logic to repository operations  

Configuration example (application.yml):

This represents the stable architectural structure of the configurable persistence layer.

---

## 4.4.6 Step 4.6 of ADD: Evaluate and Resolve Inconsistencies

During design and prototyping, several issues were identified and resolved:

- Interface consistency: All repository implementations were verified to implement the same method signatures.  
- Transaction support: MongoDB and Elasticsearch implementations use their own transaction semantics where applicable.  
- Cache synchronization: Cache invalidation strategies were defined per entity type.  
- Configuration validation: Spring Boot's @ConfigurationProperties was used to validate setup configuration at startup.  
- Fallback behavior: Cache-only mode was implemented for read operations during DB outages.  

---

## 4.5 Step 5 of ADD: Instantiate Architectural Elements and Allocate Responsibilities

Responsibilities were allocated as follows:

- PersistenceConfig: Loads configuration, instantiates appropriate repository implementations via @Profile.  
- Repository Interfaces: Define contract for CRUD and query operations per entity.  
- Concrete Repositories: Implement DB-specific logic (SQL, MongoDB, Elasticsearch).  
- RedisCacheService: Manages caching logic, expiration, and invalidation.  
- RetryableRepositoryDecorator: Wraps repository calls with retry and circuit breaker logic.  
- HealthIndicator: Provides /health endpoint details for DB and cache status.  

---

## 4.6 Step 7 of ADD: Verify and Refine Requirements and Make Them Constraints

After design refinement, the following constraints were confirmed:

- All repository implementations must implement the same interface.  
- DBMS selection must be configurable via application.yml or environment variables.  
- Cache must be enabled for all read-heavy operations.  
- The system must start with any configured DBMS without code changes.  
- In case of DB failure, read operations should fall back to cache if possible.  
- All persistent operations must be logged and monitorable.
