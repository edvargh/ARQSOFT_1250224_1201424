
# Library Management System - Architectural Tactics with Alternatives and Justifications

| Quality Attribute / Scenario | Priority | Architectural Tactics | Alternatives Considered | Rationale / Decision Justification |
|-------------------------------|-----------|------------------------|--------------------------|------------------------------------|
| **Reliability – External ISBN APIs** | 4 | Retry with exponential backoff, Fallback to alternate API, Circuit Breaker | Manual retry by client, or fail-fast approach | Automated retry + fallback ensures service reliability and better user experience when external APIs are unstable. |
| **Performance – Book Query** | 4 | Caching (Redis), Indexing, Pagination, Asynchronous queries | DB-only queries, client-side caching | Redis caching significantly reduces DB load and latency; pagination prevents over-fetching. |
| **Extensibility – Multi-DB Support** | 3 | Repository pattern, Dependency Injection, DB abstraction layer | Hardcoded DB connections per implementation | Repository abstraction allows adding new DB engines without changing business logic. |
| **Configurability – External API Selection** | 3 | Configuration-driven API selection, Plugin architecture | Compile-time API configuration | Runtime configurability improves flexibility and supports dynamic selection without redeployment. |
| **Reliability – Asynchronous Metrics** | 4 | Event-driven architecture, Message Queue (e.g., RabbitMQ/Kafka), Async processing | Synchronous metrics computation during lending | Asynchronous event-driven updates avoid blocking user actions and scale better under heavy load. |
| **Security – JWT Authentication** | 4 | Token-based authentication (JWT), Role-based access control (RBAC), HTTPS | Session-based authentication | JWT enables stateless authentication across distributed services; simplifies scalability. |
| **Performance – Paginated Listings** | 3 | Pagination, Redis caching, Lazy loading | Load full datasets in memory | Pagination reduces resource usage; caching optimizes repeated queries. |
| **Resilience – Database Failure** | 4 | Retry with backoff, Circuit Breaker, Read replicas / clustering | Failover to secondary DB only | Retry and circuit breaker patterns minimize downtime; replication enhances fault tolerance. |
| **Configurable ID Generation** | 3 | ID generation service, Strategy pattern (UUID or sequential) | Manual ID generation in DB | Configurable ID generation increases portability and consistency across data stores. |
| **Logging and Monitoring** | 3 | Centralized logging (ELK), Health checks, Metrics dashboards, Alerts | Local log files only | Centralized monitoring improves observability and simplifies maintenance in distributed setups. |
| **Data Integrity – Loan Conflicts** | 4 | Concurrency control, Transactions, Optimistic/Pessimistic locking | Last-write-wins approach | Concurrency control ensures consistent lending state; prevents double loans. |
| **Extensibility – Adding a New Metric Type** | 3 | Event-driven metrics pipeline, Plugin pattern | Static metric definitions | Event-based plugins enable flexible metric extensions without core code changes. |
| **Resilience – Cache Failure** | 3 | Cache fallback to DB, Cache warming, Auto-reconnect, Circuit Breaker | Immediate system halt on cache error | Cache fallback ensures continuity; automatic reconnection maintains availability. |


