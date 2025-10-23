# Commands for MySQL and redis: 
- docker compose up -d mysql redis
- mvn spring-boot:run -D"spring-boot.run.profiles=sql,redis" (powershell)

# Commands for MongoDB and redis:
- docker compose up -d mongo redis
- mvn spring-boot:run -D"spring-boot.run.profiles=mongo,redis" (powershell)

# Commands for Google Books API and Open Library Search API: 
- mvn spring-boot:run -D"spring-boot.run.profiles=mongo,redis,isbn-google-only" (powershell)
- mvn spring-boot:run -D"spring-boot.run.profiles=mongo,redis,isbn-openlibrary-only" (powershell)