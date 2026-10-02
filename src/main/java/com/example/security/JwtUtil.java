/*
 * JWT Utility Class - TEMPORARILY DISABLED
 * 
 * This class will be implemented when NestJS authentication is integrated.
 * Currently disabled to avoid compilation errors from missing JJWT dependencies.
 * 
 * Implementation checklist for when NestJS JWT is ready:
 * 
 * 1. Add these dependencies to pom.xml:
 *    - org.springframework.boot:spring-boot-starter-security
 *    - io.jsonwebtoken:jjwt-api:0.12.3
 *    - io.jsonwebtoken:jjwt-impl:0.12.3 (runtime)
 *    - io.jsonwebtoken:jjwt-jackson:0.12.3 (runtime)
 * 
 * 2. Delete this file or replace with working implementation
 * 
 * 3. Create JwtAuthenticationFilter class in this package
 * 
 * 4. Create SecurityConfig class in this package with @EnableMethodSecurity
 * 
 * 5. Add @PreAuthorize annotations to controllers:
 *    - See ClientsController.java for TODO comments
 * 
 * The NestJS shared JWT_SECRET must match spring.properties jwt.secret
 */