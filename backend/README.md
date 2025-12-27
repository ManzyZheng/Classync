# Classync Backend

Spring Boot 后端服务

## 运行方式

```bash
mvn clean install
mvn spring-boot:run
```

## 访问地址

- API: http://localhost:8080/api
- H2 Console: http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:mem:classync`
  - Username: `sa`
  - Password: (空)

## 技术栈

- Spring Boot 3.2.0
- Spring Data JPA
- Spring WebSocket
- H2 Database
- Lombok

