| Quality Attribute                              | Relevant Tactics                                                                                                                          |
| ---------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------- |
| **Extensibility**                              | - Abstract common services<br>- Plugin/repository abstraction<br>- Dependency inversion (DIP)<br>- Separate interface from implementation |
| **Configurability**                            | - Runtime configuration binding<br>- Parameterization<br>- Dependency injection (DI)<br>- Externalized configuration files                |
| **Reliability** (External APIs)                | - Retry with backoff<br>- Timeout & circuit breaker<br>- Fallback to alternative provider<br>- Caching                                    |
| **Persistence in multiple data models / DBMS** | - Repository pattern<br>- Adapter pattern<br>- Interface segregation<br>- Configuration-driven data access                                |
| **External ISBN retrieval**                    | - Gateway pattern<br>- Fallback chain<br>- Caching of responses                                                                           |
| **Configurable ID generation**                 | - Strategy pattern<br>- Factory method<br>- Dependency injection                                                                          |
| **Automated testing support**                  | - Test doubles & mock injection<br>- Layer isolation<br>- Configuration profiles for testing                                              |
