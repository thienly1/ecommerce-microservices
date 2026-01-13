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

    



    



