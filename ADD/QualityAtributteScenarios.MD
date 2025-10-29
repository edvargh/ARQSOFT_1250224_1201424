# Library Management System - Quality Attribute Scenarios

## 1. Reliability – External ISBN APIs

| Element          | Content                                                                       |
| ---------------- | ----------------------------------------------------------------------------- |
| Stimulus         | Temporary failure when querying an external ISBN API (Google Books or ISBNdb) |
| Stimulus Source  | External ISBN API                                                             |
| Environment      | System in production, multiple simultaneous requests                          |
| Artifact         | ISBNGateway + BookService                                                     |
| Response         | Automatic retry with exponential backoff; fallback to another configured API  |
| Response Measure | 95% of requests completed successfully; response time ≤ 3s                    |

## 2. Performance – Book Query

| Element          | Content                                                               |
| ---------------- | --------------------------------------------------------------------- |
| Stimulus         | User searches for books by genre, title, or author                    |
| Stimulus Source  | User via frontend                                                     |
| Environment      | System with 10,000+ books and high concurrency                        |
| Artifact         | BookService + RedisCache                                              |
| Response         | Queries return from cache if available; otherwise, query the database |
| Response Measure | Response time ≤ 500ms for 95% of queries                              |

## 3. Extensibility – Multi-DB Support

| Element          | Content                                                                             |
| ---------------- | ----------------------------------------------------------------------------------- |
| Stimulus         | Administrator decides to switch the database or add support for another DBMS        |
| Stimulus Source  | Setup configuration                                                                 |
| Environment      | System in production or testing                                                     |
| Artifact         | Repository Layer + ConfigLoader                                                     |
| Response         | System dynamically switches repository implementation (SQL, MongoDB, Elasticsearch) |
| Response Measure | Database switch without code changes; all CRUD operations remain functional         |

## 4. Configurability – External API Selection

| Element          | Content                                                                         |
| ---------------- | ------------------------------------------------------------------------------- |
| Stimulus         | Administrator defines which external APIs to use during setup                   |
| Stimulus Source  | Configuration file (YAML/JSON)                                                  |
| Environment      | System in production or testing                                                 |
| Artifact         | ISBNGateway + ConfigLoader                                                      |
| Response         | System uses configured APIs dynamically without recompilation                   |
| Response Measure | API change reflected at runtime; 100% of requests use the correct configuration |

## 5. Reliability – Asynchronous Metrics

| Element          | Content                                                          |
| ---------------- | ---------------------------------------------------------------- |
| Stimulus         | Book loan registered or return completed                         |
| Stimulus Source  | LendingService                                                   |
| Environment      | System with thousands of simultaneous loans                      |
| Artifact         | EventQueue + MetricsService                                      |
| Response         | Asynchronous update of metrics (Top 5, average duration, etc.)   |
| Response Measure | Metrics updated within 5s; does not block loan/return operations |

## 6. Security – JWT Authentication

| Element          | Content                                                |
| ---------------- | ------------------------------------------------------ |
| Stimulus         | User attempts to access an authenticated resource      |
| Stimulus Source  | Frontend or external user                              |
| Environment      | System in production, multiple concurrent users        |
| Artifact         | AuthService + Controllers                              |
| Response         | Verify JWT token; access denied if invalid or expired  |
| Response Measure | 100% of invalid token requests denied; no data leakage |

## 7. Performance – Paginated Listings

| Element          | Content                                                          |
| ---------------- | ---------------------------------------------------------------- |
| Stimulus         | User requests list of readers, books, or loans with large volume |
| Stimulus Source  | Frontend or external system                                      |
| Environment      | System with thousands of records                                 |
| Artifact         | Controller + Service + Repository                                |
| Response         | Returns only the requested page; uses pagination and cache       |
| Response Measure | Response time ≤ 1s per page; controlled memory usage             |

## 8. Resilience – Database Failure

| Element          | Content                                                                     |
| ---------------- | --------------------------------------------------------------------------- |
| Stimulus         | Database does not respond or is temporarily offline                         |
| Stimulus Source  | Relational DB, MongoDB, or Elasticsearch                                    |
| Environment      | Production system with high concurrency                                     |
| Artifact         | Repository Layer + Service Layer                                            |
| Response         | Retry with exponential backoff; temporary fallback using cache if available |
| Response Measure | 95% of critical operations completed successfully; system does not crash    |

## 9. Configurable ID Generation

| Element          | Content                                                                         |
| ---------------- | ------------------------------------------------------------------------------- |
| Stimulus         | Creation of a new entity (Book, Author, Reader, Loan)                           |
| Stimulus Source  | User or automated system                                                        |
| Environment      | Production system, multiple simultaneous requests                               |
| Artifact         | IDGenerator + Service Layer                                                     |
| Response         | ID generated according to configuration (UUID or sequential) without collisions |
| Response Measure | 100% unique IDs; generation time ≤ 1ms per ID                                   |

## 10. Logging and Monitoring

| Element          | Content                                                        |
| ---------------- | -------------------------------------------------------------- |
| Stimulus         | Any critical operation or error in the system                  |
| Stimulus Source  | System or user                                                 |
| Environment      | Production system under high load                              |
| Artifact         | LoggingService + MonitoringTool                                |
| Response         | Detailed event logging and automatic alerts in case of failure |
| Response Measure | 100% of critical failures logged; alerts sent in real-time     |

## 11. Data Integrity – Loan Conflicts

| Element          | Content                                                                                         |
| ---------------- | ----------------------------------------------------------------------------------------------- |
| Stimulus         | Two users attempt to borrow the same book simultaneously                                        |
| Stimulus Source  | Frontend / LendingService                                                                       |
| Environment      | Production system, multiple concurrent loans                                                    |
| Artifact         | LendingService + Repository Layer                                                               |
| Response         | Concurrency control; only one loan operation allowed; second request receives informative error |
| Response Measure | 0 inconsistent records; response ≤ 500ms                                                        |

## 12. Extensibility – Adding a New Metric Type

| Element          | Content                                                                                            |
| ---------------- | -------------------------------------------------------------------------------------------------- |
| Stimulus         | Analyst wants to create a new metric for the dashboard                                             |
| Stimulus Source  | Configuration or administrative code                                                               |
| Environment      | System in production or testing                                                                    |
| Artifact         | MetricsService + EventQueue + DB                                                                   |
| Response         | Metric added without impacting existing metrics; new events published and processed asynchronously |
| Response Measure | Metric available for query within 5s; system remains stable                                        |

## 13. Resilience – Cache Failure

| Element          | Content                                                                       |
| ---------------- | ----------------------------------------------------------------------------- |
| Stimulus         | RedisCache not available                                                      |
| Stimulus Source  | Redis server or network                                                       |
| Environment      | Production system under high load                                             |
| Artifact         | BookService + MetricsService + Repository Layer                               |
| Response         | Queries fallback to DB; error logs generated; cache automatically reconnected |
| Response Measure | 95% of requests completed; system remains responsive                          |

---
