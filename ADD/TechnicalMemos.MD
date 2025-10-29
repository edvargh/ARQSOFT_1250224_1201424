# Library Management System – Technical Memos

---

## Technical Memo 01 – Multi-DB Persistence (Extensibility)

### Issue

Different customers use diverse DBMS technologies, including relational, document-oriented, and in-memory stores.

### Problem

How to persist data across multiple data models (e.g., relational, document) and DBMS (e.g., MySQL, SQL Server, MongoDB, Redis) without compromising maintainability and consistency.

### Summary of Solution

Adopt the following tactics: **Intermediary**, **Abstract Common Services**, and **Deferred Binding**.

### Factors

The existing layered architecture (domain, repository, service) supports these tactics and allows for the introduction of abstractions without major redesign.

### Solution

Develop a configuration mechanism supported by **SOLID principles** (specifically OCP and DIP) applied to persistence repositories. Implement a **Repository Abstraction Layer** that can dynamically bind to different persistence implementations at setup time. Use **Dependency Injection** to configure repositories for the active persistence technology.

### Motivation

Although it requires professional training and architectural discipline, this solution ensures maintainability and extensibility. It aligns with modern architectural patterns such as **Clean Architecture** and promotes separation of concerns.

### Alternatives

* Use conditional logic (if-then-else) in persistence classes to select DB implementations.
* Create separate branches or codebases per persistence technology.

### Pending Issues

* Are there data models that cannot support all common repository operations?
* How will transactional consistency be handled across heterogeneous data stores?

---

## Technical Memo 02 – External API Reliability (Retry and Fallback)

### Issue

External APIs such as Google Books and ISBNdb are occasionally unavailable or slow, affecting book registration and search operations.

### Problem

How to maintain service reliability and user experience when third-party APIs fail or respond slowly.

### Summary of Solution

Apply **Retry with Exponential Backoff**, **Fallback Mechanism**, and **Circuit Breaker** patterns.

### Factors

System relies on multiple third-party APIs for retrieving ISBN and book metadata.

### Solution

The gateway layer will automatically retry failed API calls using exponential backoff. If an API remains unavailable, the system falls back to an alternative API. The Circuit Breaker monitors failure rates and temporarily blocks requests to unstable APIs to prevent cascading failures.

### Motivation

This ensures fault tolerance and high availability without overloading external services. It reduces user-visible errors and improves perceived responsiveness.

### Alternatives

* Manual retries initiated by the client.
* Fail-fast approach, immediately returning an error.

### Pending Issues

* How to manage and synchronize API quotas and rate limits.
* What is the acceptable timeout threshold before triggering fallback.

---

## Technical Memo 03 – Asynchronous Metrics (Event-driven Architecture)

### Issue

Metrics computation (e.g., Top 5 books, average lending duration) slowed down core lending operations.

### Problem

How to maintain responsive lending and returning operations while keeping metrics updated in near real-time.

### Summary of Solution

Adopt an **Event-driven Architecture** using **Message Queues** and **Asynchronous Processing**.

### Factors

The system already separates command (lending actions) and query (reporting) responsibilities, allowing easy decoupling.

### Solution

Each lending or return operation emits events to a message queue. The **Metrics Service** asynchronously consumes these events and updates summary tables or caches. This avoids blocking the main transactional flow and scales independently.

### Motivation

Asynchronous updates improve system responsiveness and scalability. They also align with microservice-ready architectures.

### Alternatives

* Synchronous metric computation in the main flow.
* Periodic batch processing of metrics.

### Pending Issues

* How to ensure eventual consistency between transactional and analytics data.
* What are acceptable latencies for metric updates.

---

## Technical Memo 04 – JWT Authentication (Security)

### Issue

Authenticated access to resources is required for librarians and readers.

### Problem

How to secure REST APIs in a distributed environment without introducing server-side session management.

### Summary of Solution

Implement **JWT-based Authentication** combined with **Role-based Access Control (RBAC)**.

### Factors

The system exposes stateless REST endpoints and must support horizontal scaling.

### Solution

Each successful authentication request issues a **JWT** signed with a private key. The token carries role information (Librarian, Reader) and expiration time. All subsequent requests include this token for authorization. Token validation occurs at the gateway or controller layer.

### Motivation

JWT enables stateless authentication and reduces session management complexity. It supports secure communication and is compatible with distributed architectures.

### Alternatives

* Server-side session storage.
* API key authentication.

### Pending Issues

* Key rotation and token revocation strategies.
* Secure token storage on client-side.

---

## Technical Memo 05 – Database Failure Resilience (Circuit Breaker and Retry)

### Issue

Database connections may become temporarily unavailable due to network or hardware issues.

### Problem

How to keep the system operational during transient database failures.

### Summary of Solution

Use **Retry with Exponential Backoff** combined with a **Circuit Breaker** pattern, and leverage **Cache Fallback** where possible.

### Factors

The persistence layer relies on external DBMS, which can suffer intermittent outages.

### Solution

Introduce a resilience layer within the repository implementation. It detects failed operations and automatically retries them with increasing intervals. If failures persist, the circuit opens, and the system temporarily serves cached data until the DB recovers.

### Motivation

This approach ensures graceful degradation instead of complete failure. It supports high availability and prevents cascading failures.

### Alternatives

* Immediate failure propagation.
* Manual recovery processes.

### Pending Issues

* How to synchronize cached and persisted data after recovery.
* How long should the circuit remain open.

---

## Technical Memo 06 – Caching and Pagination (Performance)

### Issue

Querying large datasets (e.g., books, readers, lendings) may lead to high latency and resource consumption.

### Problem

How to ensure fast response times for frequent queries without overwhelming the database.

### Summary of Solution

Adopt **Caching (Redis)** and **Pagination** strategies.

### Factors

The library manages tens of thousands of books and readers, with frequent concurrent queries.

### Solution

Implement query result caching for common read operations, using **Redis** for fast retrieval. Introduce pagination parameters (page number, size) on API endpoints to limit the volume of data per request.

### Motivation

Caching and pagination dramatically improve perceived performance and scalability. They also reduce DB load and improve cost efficiency.

### Alternatives

* Load all data at once in memory.
* Client-side filtering and caching only.

### Pending Issues

* Cache invalidation policies when underlying data changes.
* Balance between cache size and memory usage.

---
