Your goal is to cover the implemented changes with comprehensive test cases. Follow this pattern:

**Test Class Structure:**

- Create nested test classes using `@Nested` to group related functionality (Create, Read, Update, Delete, Validation,
  ErrorHandling)
- Use `@DisplayName` to describe the test intention clearly
- Follow the Given-When-Then pattern in each test method
- Use descriptive method names: `should_ReturnResult_When_ConditionMet()`

**Test Coverage Requirements:**

- Happy path scenarios with valid inputs
- Edge cases (null, empty, boundary values)
- Error scenarios and exception handling
- Multiple assertions per test to validate thoroughly

**Assertion Best Practices:**

- Use AssertJ fluent assertions: `assertThat(result).isNotNull().hasSize(3)`
- Verify mock interactions: `verify(dependency).method(expectedParam)`
- Test both return values and side effects
- Include meaningful error messages in assertions

**Mock Setup:**

- Use `@Mock` for dependencies and `@InjectMocks` for class under test
- Configure mock behavior in the Given section
- Verify mock calls in a Then section

**Project Best Practices:**

- Follow the existing structure and naming conventions used across the project
- notify me if there is no unified structure or testing approach
- run the tsts that you added to make sure they pass
- don't alter or refactor any existing test classes

**Example Structure:**

```java

@Nested
@DisplayName("Create Operations")
class CreateOperations {

    @Test
    @DisplayName("Should create entity when valid data provided")
    void should_CreateEntity_When_ValidDataProvided() {
        // Given - set up test data and mocks
        // When - execute method under test
        // Then - assert results and verify interactions
    }
}
```