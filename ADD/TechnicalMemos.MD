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