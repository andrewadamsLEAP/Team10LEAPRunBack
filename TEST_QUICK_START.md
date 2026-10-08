# Quick Start Guide: Running the Test Suite

## Overview
This trading application has a comprehensive test suite covering unit tests, edge cases, and integration scenarios.

## Test Files Created

### ✅ Unit Tests (Ready to Run)
1. **OrdersServiceTest.java** (12 tests)
   - Buy/sell order creation and validation
   - Transaction synchronization testing
   - Kafka publishing verification
   
2. **OrderConsumerServiceTest_Extended.java** (35 tests)
   - Kafka message consumption and deserialization
   - Order execution logic
   - Error handling and recovery

3. **OrderEdgeCasesTest.java** (30+ tests)
   - Boundary condition validation
   - Input validation edge cases
   - Status transition rules

### ⚠️ Integration Tests (Require Setup)
4. **KafkaOrderFlowIntegrationTest.java** (6 tests)
   - End-to-end order flow through Kafka
   - Requires: Kafka running on localhost:9092

5. **OrdersAPIIntegrationTest.java** (11 tests)
   - REST API endpoint testing (Postman-style)
   - Requires: Spring Boot application context

## Running Tests Locally

### Option 1: Run Unit Tests Only (Fastest)
```bash
# Terminal at project root
mvn clean test -Dtest=OrdersServiceTest,OrderConsumerServiceTest_Extended,OrderEdgeCasesTest
```

**Expected Result:**
```
[INFO] Tests run: 77, Failures: 0, Errors: 0, Skipped: 0
```

**Time:** ~10-15 seconds ⚡

### Option 2: Run All Tests
```bash
# Requires Kafka running on localhost:9092
docker-compose up -d kafka

# Run full test suite
mvn clean test
```

### Option 3: Run Specific Test Class
```bash
# Run only buy/sell order tests
mvn test -Dtest=OrdersServiceTest

# Run only consumer/execution tests
mvn test -Dtest=OrderConsumerServiceTest_Extended

# Run only edge case tests
mvn test -Dtest=OrderEdgeCasesTest
```

### Option 4: Run Specific Test Method
```bash
# Test a specific scenario
mvn test -Dtest=OrdersServiceTest#placeBuyOrderCreatesOrderWhenAllValidationsPass

# Test multiple specific methods
mvn test -Dtest=OrdersServiceTest#placeBuyOrder*,OrdersServiceTest#placeSellOrder*
```

## What Each Test File Tests

### OrdersServiceTest.java - Core Order Placement Logic
**Tests order creation, validation, and Kafka publishing**

Sample tests:
- ✅ Buy order creation with all validations passing
- ✅ Sell order creation with all validations passing
- ✅ Rejection when client has insufficient cash
- ✅ Rejection when client lacks holdings for sell
- ✅ Rejection when instrument/ticker doesn't exist
- ✅ Transaction synchronization for Kafka publishing

### OrderConsumerServiceTest_Extended.java - Kafka Message Consumption
**Tests async order execution triggered by Kafka messages**

Sample tests:
- ✅ Successfully consume and execute valid order from Kafka
- ✅ Handle malformed JSON gracefully
- ✅ Deserialize Order correctly from JSON
- ✅ Execute PENDING order and transition to FULFILLED
- ✅ Reject non-PENDING orders
- ✅ Update holdings after execution
- ✅ Handle database failures

### OrderEdgeCasesTest.java - Boundary & Edge Cases
**Tests unusual inputs and boundary conditions**

Sample tests:
- ✅ Quantity validation (0, negative, 1, large)
- ✅ Price validation (fractional, large, zero, negative)
- ✅ Ticker validation (length, special chars, case)
- ✅ Client ID validation (null, zero, negative, large)
- ✅ Status transition rules (valid/invalid)
- ✅ Data integrity across operations

### KafkaOrderFlowIntegrationTest.java - End-to-End Kafka Flow
**Tests complete order flow from placement to execution**

Sample tests:
- ✅ Complete order lifecycle: place → kafka → execute
- ✅ Buy order publishing to Kafka
- ✅ Sell order publishing to Kafka
- ✅ Order status transitions through Kafka
- ✅ Cancelled order handling
- ✅ Data consistency through Kafka pipeline

### OrdersAPIIntegrationTest.java - REST API Endpoints
**Tests all order management REST endpoints**

Sample tests:
- ✅ POST /orders/buy - success and error cases
- ✅ POST /orders/sell - success and error cases
- ✅ GET /orders/{id} - retrieve order details
- ✅ GET /orders/client/{clientId}/fulfilled - list fulfilled
- ✅ DELETE /orders/{id} - cancel order
- ✅ Error handling - malformed JSON, missing fields, etc.

## Test Results Summary

### Current Status
```
Unit Tests:       77/77 passing ✅
Integration Tests: Require Kafka (optional)
Total Coverage:   Comprehensive (order placement, execution, edge cases)
```

### Performance
```
Unit tests only:        ~15 seconds
With integration tests:  ~45 seconds (+ Kafka startup)
```

## Running with IDE Integration

### VS Code with Java Extension
1. Open any test file
2. Click "Run" or "Debug" above test methods
3. Results appear in VS Code Test Explorer

### IntelliJ IDEA
1. Right-click test class → Run 'ClassName'
2. Or right-click test method → Run test
3. Results in Run tool window

### Eclipse
1. Right-click test class → Run As → JUnit Test
2. Or Run → Run Configurations

## Test Configuration

### Default Configuration
- **Database:** Uses application-test.properties
- **Kafka:** Can be disabled with app.kafka.enabled=false
- **Spring Profile:** "test" profile active during tests

### Custom Configuration
If you need to override test properties:
```java
@TestPropertySource(properties = {
    "app.kafka.enabled=true",
    "spring.kafka.bootstrap.servers=localhost:9092"
})
```

## Debugging Failed Tests

### Common Issues & Solutions

**Issue: "Order not actively in a transaction"**
- **Cause:** Missing TransactionSynchronizationManager mock
- **Solution:** Ensure OrdersServiceTest uses MockedStatic
- **Status:** ✅ Fixed in current version

**Issue: "Kafka broker not reachable"**
- **Cause:** Kafka not running for integration tests
- **Solution:** Skip integration tests or start Kafka: `docker-compose up -d`
- **Fix:** Run unit tests only: `mvn test -Dtest=*ServiceTest`

**Issue: "ApplicationContext failure threshold exceeded"**
- **Cause:** Spring Boot context can't load
- **Solution:** Ensure @SpringBootTest(classes = TradingAppApplication.class)
- **Status:** ✅ Fixed in current version

**Issue: "Compilation error: cannot find symbol"**
- **Cause:** Missing Maven dependencies
- **Solution:** Run `mvn clean compile` first
- **Workaround:** Let IDE rebuild project

## Test Coverage

To generate a coverage report:
```bash
mvn clean test jacoco:report
open target/site/jacoco/index.html  # macOS
start target\site\jacoco\index.html # Windows
```

## CI/CD Integration

### GitHub Actions Example
```yaml
- name: Run Unit Tests
  run: mvn clean test -Dtest=*ServiceTest

- name: Start Kafka for Integration Tests
  run: docker-compose up -d

- name: Run Integration Tests
  run: mvn test
```

### Jenkins Pipeline
```groovy
stage('Unit Tests') {
    steps {
        sh 'mvn clean test -Dtest=*ServiceTest'
    }
}

stage('Integration Tests') {
    steps {
        sh 'docker-compose up -d'
        sh 'mvn test'
    }
}
```

## Manual Testing with Postman

Use OrdersAPIIntegrationTest.java as reference for:

**Buy Order:**
```
POST /orders/buy
Content-Type: application/json

{
  "clientId": 1,
  "ticker": "AAPL",
  "quantity": 10
}
```

**Sell Order:**
```
POST /orders/sell
Content-Type: application/json

{
  "clientId": 1,
  "ticker": "AAPL",
  "quantity": 5
}
```

**Get Order:**
```
GET /orders/1
```

**Get Fulfilled Orders:**
```
GET /orders/client/1/fulfilled
```

**Cancel Order:**
```
DELETE /orders/1
```

## Troubleshooting

### Tests Pass Locally but Fail in CI
- Check Java version (project uses Java 11+)
- Verify Maven/JDK in CI environment
- Check for timezone or locale dependencies
- Ensure database is initialized in CI

### Tests Hang or Timeout
- Check if Kafka is stuck (restart: `docker-compose restart`)
- Verify network connectivity
- Check for deadlocks in transaction tests
- Increase timeout: `mvn test -Dorg.apache.maven.plugins.surefire.useSystemClassLoader=false`

### Memory Issues
- Increase heap: `mvn -Xmx1G test`
- Reduce parallel test execution
- Check for resource leaks in tests

## Next Steps

1. **Run unit tests locally:**
   ```bash
   mvn clean test -Dtest=OrdersServiceTest,OrderConsumerServiceTest_Extended,OrderEdgeCasesTest
   ```

2. **Review test results** in target/surefire-reports/

3. **For integration tests:**
   - Start Kafka: `docker-compose up -d`
   - Run full suite: `mvn test`

4. **For API testing:**
   - Start application: `mvn spring-boot:run`
   - Use Postman endpoints documented above

## Additional Resources

- **Test Documentation:** See TEST_SUITE_DOCUMENTATION.md
- **Architecture Notes:** See repo memory files
- **Code Comments:** Every test has detailed comments

## Support

For issues or questions:
1. Check test file comments for specific test details
2. Review TEST_SUITE_DOCUMENTATION.md for comprehensive guide
3. Check application logs: `target/logs/`
4. Review Maven output for detailed error messages

---

**Happy Testing! 🧪**
