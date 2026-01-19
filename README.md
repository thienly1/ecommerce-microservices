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

## Security with OAuth2 + JWT

   1. Add the keyCloak configuration to docker-compose.yml then create and run the image by: docker-compose up
   2. Verify Keycloak is running by open: http//localhost:8180, login as admin: admin
   3. Configure KeyCloak
        Create a Realm4: Click "Keycloak" dropdown → "Create Realm"
                         Enter: ecommerce -> Click: Create.
        Create a Client (for this application)
            Go to Clients → "Create client"
            General Settings: 
                Client type: OpenID Connect 
                Client ID: ecommerce-app
                Click "Next".
            Capability config:
                Client authentication: ON
                Authorization: OFF
                Authentication flow: Check "Standard flow" and "Direct access grants"
                Click "Next".
            Login setting:
                Valid redirect URIs: http://localhost:8080/*
                Web origins: http://localhost:8080
                Click "Save"
        Get Client Secret:
            Go to Clients → ecommerce-app → Credentials tab
            Copy the Client secret (you'll need this later)
        Create roles:
            Go to Realm roles → "Create role"
            Create these roles, note that Keycloak will add prefix ROLE_ to every role name:
                Role name: USER → Save
                Role name: ADMIN → Save
        Create Test Users:
            Go to Users → "Add user"
            User 1 (Regular User):
                Username: user
                Email: user@test.com
                First name: Test
                Last name: User
                Click "Create"
                Go to Credentials tab → "Set password"
                Password: user123
                Temporary: OFF
                Click "Save"
                Go to Role mapping tab → "Assign role" → Select USER.
            User 2 (Admin):
                Username: admin
                Email: admin@test.com
                First name: Admin
                Last name: User
                Click "Create"
                Go to Credentials tab → Set password: admin123
                Go to Role mapping tab → Assign roles: ROLE_USER and ROLE_ADMIN
   4. Add Security Dependencies
        - Go to start.spring.io, search for 2 dependencies: OAuth2 Resource Server and Spring Security
        - Copy the dependencies to the pom.xml for all services( user-service, product-service, order-service and api-gateway)
   5. Update Config Repository
        - In the application.yml for all services in github, add OAuth2 Resource Server Configuration to shared config. Note if you have "spring" feature allready, add this to that "spring":
            spring:
              security:
                oauth2:
                  resourceserver:
                    jwt:
                      issuer-uri: http://localhost:8180/realms/ecommerce
                      jwk-set-uri: http://localhost:8180/realms/ecommerce/protocol/openid-connect/certs
   6. Config all the services:
        -Create config folder, implement SecurityConfig and KeycloakRoleConverter to configure security
   7. Propagate JWT Token Between Services
        When Order Service calls User Service or Product Service, it needs to forward the JWT token.
        Update order-service/src/main/java/com/ecommerce/order_service/config/WebClientConfig.java to all jwt to all requests and handle routes
   8. Restart All Services
   9. Test Security
        - Test without token: shoudl be 401
        -  Get JWT Token from Keycloak with postman: 
            POST: http://localhost:8180/realms/ecommerce/protocol/openid-connect/token
            Go to Body Tab: Select x-www-form-urlencoded then add These Key-Value Pairs:
                Key                 Value
                client_id           ecommerce-app
                client_secret       client_secret_key (from keycloak: localhost:8180)
                username            user  (user in keycloak, in this case: user)
                password            user123 (user password in keycloak, in this case for testing: user123)
                grant_type          password
        - Copy access_token
        - Go to check urls with USER permit, for example: localhost:8080/api/users
            Authorization -> Bearer Token -> paste token here
            Run the url => It should work now
        - Test with admin role with same logic
## Add WebClient to send order-service to user-service and product-service 
    - to handle exception before deleting user and product if they are in active orders.

## Add Kafka Broker to microservices
    Apache Kafka is a distributed event streaming platform that allows services to communicate asynchronously through events.
    - For example, when an user places an order, it will publish to kafka broker.
    - If product-service doesn't work so whenever it works, it will get that order to update the stocks (won't break microservices)
   1. Set up kafka
    - Update docker-compose.yml file
    - Run with: docker-compose up -d
    - check with: docker ps
    - Open the kafka ui in browser. http://localhost:8090. There is no topic right now.
   2.  Add Kafka Dependency to Services
       - Go to start.spring.io, search for Spring for Apache Kafka
       - Add this dependency to 3 services: user-service, product-service, order-service
   3. Create Event Models
          - Note: I will create Publishers to user-service and order-service, so whenever an event is created, it will be published to kafka broker.
          - Also, product-service will be kafka consumer, it will get event whenever the event is sent to kafka broker from user-service and order-service.
          product-service will be announced when order created ,... to update quantity of stocks, also, when new user is created, it will be announced
          to product as well,...
          - In this project, just emit the event when user created and deleted and order created and deleted
          - We also can create Publishers or Consumers as we wish.
          - I also added some services to restore product if order is canceled, and some user preference services
   4. Rerun with right order
   5. Test the services
       - Try to make some requests from POST: http://localhost:8080/api/users and http://localhost:8080/api/orders then check if product-service get those events or not.
       - Test in kafka ui to see the topics as well.
   


    


