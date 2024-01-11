# DrinkIt

## Technologies

* Java 21
* Node 20.11.0
* Gradle
* Spring Boot 3.2.1 + Kotlin
* Nuxt 3
* OpenApi to generate frontend files and Backend Apis

## How to run the app

### Backend
```
./gradlew :drinkit-backend:build
./gradlew :drinkit-backend:bootRun #To start Spring Boot Application 
```

* Api available on: `http://localhost:8080/drinkit/api/cellars`
* OpenApi Documentation available on: `http://localhost:8080/swagger-ui/index.html`
* Actuator Endpoints available on: `http://localhost:8080/actuator`

### Frontend

**Requirements**
You need to have the java executable in your path (openapi generator javascript client downloads the Java OpenApi client to execute the task :/)


```
cd drinkit-frontend
npm i
npm run generate:client-api
npm run dev
```

* Frontend application available on: `http://localhost:3000/cellars`

