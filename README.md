# Commands for MySQL and redis: 
- docker compose up -d mysql redis
- mvn spring-boot:run -D"spring-boot.run.profiles=sql,redis" (powershell)

# Commands for MongoDB and redis:
- docker compose up -d mongo redis
- mvn spring-boot:run -D"spring-boot.run.profiles=mongo,redis" (powershell)

# Commands for Google Books API and Open Library Search API: 
- mvn spring-boot:run -D"spring-boot.run.profiles=mongo,redis,isbn-google-only" (powershell)
- mvn spring-boot:run -D"spring-boot.run.profiles=mongo,redis,isbn-openlibrary-only" (powershell)

# Commands for base64 and timestamp id generation:
- # Base64
mvn spring-boot:run -D"spring-boot.run.profiles=mongo,redis,isbn-openlibrary-only,id-base65"

# Timestamp/ULID-like
mvn spring-boot:run -D"spring-boot.run.profiles=mongo,redis,isbn-openlibrary-only,id-ts"

# Command to run mutation tests:
- mvn clean test org.pitest:pitest-maven:mutationCoverage

# Command to run unit and integration tests:
- mvn verify

# Command to run unit tests:
- mvn test