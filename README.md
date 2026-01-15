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


## SPRING CLOUD CONFIG SERVER: Centralized Configuration
    - Config Server stores all configuration in ONE place (Git repository) to solve: 
        - Duplicated configuration (database credentials, eureka URL, etc.)
        - To change one setting, you must update multiple files
        - Sensitive data (passwords) in source code
        - Must restart and redeploy to change config

### Generate the Project
    Go to start.spring.io:
    Artifact: config-server
    Name: config-server
    Package name: com.ecommerce.configserver
    Dependencies: Config Server, Eureka Discovery Client
    Download and extract to ecommerce-microservices/config-server
### Create a new repository for ecommerce-config (private)
    Create a new file application.yml to handle eureka client config for all services
    Create new config files for all services: api-gateway.yml, user-service.yml, product-service.yml adn order-service.yml with nessacery configs
    Create a new Private Access Token to grant permit access (repo)
### Configure config-server folder
    In ConfigServerApplication: add @EnableConfigServer notation
    Create a new file: application.yml to handle git access and Eureka client as well(this is an eureka client service)
    There are many ways to handle Private access token. 
        - Add hard code username and password=private_access_token here.
        - Use the reusable code here.
            - If you use CMD, set GIT_USERNAME=username (github's username) and set GIT_PASSWORD=private_access_token
            - If you use Intelij: go to ConfigServerApplication => Edit configuaration => Environment variables => GIT_PASSWORD=private_access_token;GIT_USERNAME=thienly1
    Run the config-server, check if it works or not
    Test Configuration's endpoints:
        - Get product-service config: curl http://localhost:8888/user-service/default
        - Get product-service config: curl http://localhost:8888/product-service/default
        - Get api-gateway config: curl http://localhost:8888/api-gateway/default
### Update all services as Config-Client
    Add Config-client dependency to pom.xml file
    Remove all the configures which had added in github repo, just keep the name and add the config server to this config client
        spring:
            application:
                name: user-service
            config:
                import: optional:configserver:http://localhost:8888
    Start the all microservices with right order:
        1. Discovery Server  (port 8761)  ← FIRST
        2. Config Server     (port 8888)  ← SECOND
        3. User Service      (port 8081)
        4. Product Service   (port 8082)
        5. Order Service     (port 8083)
        6. API Gateway       (port 8080)  ← LAST


##  Circuit Breaker (Resilience4j)
    - Circuit Breaker - Handle service failures gracefully
    - Retry - Automatically retry failed requests
    - Fallback - Return default response when service is down
### Add Dependencies to Order Service 
    - This service need to comunicate with User service and product service to get data
    - 2 dependencies need to be added:
        <!-- Resilience4j -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
        </dependency>
        <!-- AOP for annotations -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-aop</artifactId>
        </dependency>
### Add Resilience4j Configuration to order-service.yml file on gitHub ecommerce-config
    - Add Resilience4j Configuration to order-service.yml file
    - Also add circuit breaker to Actuator configuration in that yml file.
### Reimplement UserServiceClient and ProductServiceClient to integrate circuit breaker
    - Reimplement UserServiceClient and ProductServiceClient to integrate circuit breaker
    - Create new exception class: ServiceUnavailableException to handle fallback methods in both classes above then add it to GlobalExceptionHandler
### Test the Circuit Breaker
    - Restart all the services in the project with right order
    - Create new user, product, order by http://localhost:8080/api/* => should work!
    - check the http://localhost:8083/actuator/health. See all the services are up, circuit breaker is closed
    - Stop Product Service or User Service
    - Try creating new order: http://localhost:8080/api/orders around 3-5 times. It should have reponse status:500- Product is unavailable
    - Check the http://localhost:8083/actuator/health again => Get circuit Breaker status: HALF-OPEN
    - Try more than 10 times to create orders, the status should be: OPEN

## Distributed Tracing with Zipkin
    - When a request flows through multiple services, it's hard to debug. 
    - Zipkin solves this by:  Assigning a unique Trace ID to each request
                              Tracking time spent in each service        
                              Showing the complete request flow visually    
    1. Start Zipkin Server with Docker
    - docker run -d -p 9411:9411 --name zipkin openzipkin/zipkin or add zipin configuration to docker-compose.yml:
     zipkin:
         image: openzipkin/zipkin
         container_name: zipkin
         ports:
           - "9411:9411"
    2. Add Dependencies(ZipKin) to All Services
    - Add these dependencies to ALL services (api-gateway, user-service, product-service, order-service)
    3. Update the config files in GitHub (ecommerce-config repo): application.yml with tracing point and Zipkin configuration
    4. Restart All Services with right order:                                                                   
        Zipkin (port 9411)                                                                       
        Discovery Server (port 8761)                                                                       
        Config Server (port 8888)
        User Service (port 8081)
        Product Service (port 8082) 
        Order Service (port 8083)
        API Gateway (port 8080)
    5. Test Distributed Tracing
        - Make some requests
        - open http://localhost:9411
        - click RUN QUERY or choose serviceName or ...




