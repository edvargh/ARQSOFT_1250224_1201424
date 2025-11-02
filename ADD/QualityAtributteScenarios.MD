# Library Management System - Quality Attribute Scenarios

## 1. Reliability – External ISBN APIs

| Element          | Content                                                                       |
| ---------------- | ----------------------------------------------------------------------------- |
| Stimulus         | Temporary failure when querying an external ISBN API (Google Books or ISBNdb) |
| Stimulus Source  | External ISBN API                                                             |
| Environment      | System in production, multiple simultaneous requests                          |
| Artifact         | ISBNGateway + BookService                                                     |
| Response         | Automatic retry with exponential backoff; fallback to another configured API  |

## 2. Extensibility – Multi-DB Support

| Element          | Content                                                                             |
| ---------------- | ----------------------------------------------------------------------------------- |
| Stimulus         | Administrator decides to switch the database or add support for another DBMS        |
| Stimulus Source  | Setup configuration                                                                 |
| Environment      | System in production or testing                                                     |
| Artifact         | Repository Layer + ConfigLoader                                                     |
| Response         | System dynamically switches repository implementation (SQL, MongoDB)                |

## 3. Configurability – External API Selection

| Element          | Content                                                                         |
| ---------------- | ------------------------------------------------------------------------------- |
| Stimulus         | Administrator defines which external APIs to use during setup                   |
| Stimulus Source  | Configuration file (YAML/JSON)                                                  |
| Environment      | System in production or testing                                                 |
| Artifact         | ISBNGateway + ConfigLoader                                                      |
| Response         | System uses configured APIs dynamically without recompilation                   |

## 4. Configurable ID Generation

| Element          | Content                                                                         |
| ---------------- | ------------------------------------------------------------------------------- |
| Stimulus         | Creation of a new entity (Book, Author, Reader, Loan)                           |
| Stimulus Source  | User or automated system                                                        |
| Environment      | Production system, multiple simultaneous requests                               |
| Artifact         | IDGenerator + Service Layer                                                     |
| Response         | ID generated according to configuration (UUID or sequential) without collisions |