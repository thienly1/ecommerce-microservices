# SIMPLE ECOMMERCE- MICROSERVICES

## SERVICE DISCOVERY
    - To create communication automatically between other services
### Discovery-server
    1. Start with Start.spring.io: choose Maven, Jar, Java version 21, Spring Boot version 3.5.x, add Eureka Server dependency
    2.  Generate code, copy and paste to ecommerce-microservices
    3.  Enable Eureka Server in DiscoveryServerApplication.java by @EnableEurekaServer
    4. Create application.yml file to handle server and port, ..
    5. Run discovery-server, check at port 8761 by web browser: localhost:8761
### Add other microservice to covert to Eureka Client
    1. Add Eureka Discovery Client dependency to pom.xml
    2. Update application.yml with Eureka Client
    3. Reconfigure WebClientConfig and client/UserServiceClient and client/ProductServiceClient to handle service name from Eureka
    4. Restart the client service
    5. Check on localhost:8761
### => Now regardless the port user-service and order-server are using, order-service can get the data from both without changing their url.
    Try to run user-service at another port, then check GET: localhost:8083/api/orders/1.

## API-GATEWAY

### Generate the Project
    Go to start.spring.io:
    Project: Maven
    Language: Java
    Spring Boot: 3.3.x
    Group: com.ecommerce
    Artifact: api-gateway
    Name: api-gateway
    Package name: com.ecommerce.api_gateway
    Packaging: Jar
    Java: 21
    Dependencies: Gateway (Spring Cloud Gateway), Eureka Discovery Client, Actuator
    Download and extract to ecommerce-microservices/api-gateway
### Create application.yml file to handle all services
### Create a config/CorsConfig.java to handle the connection with Frontend part.
