<!-- Copilot: use these instructions for all Java test completions -->

# GitHub Copilot Custom Instructions for Java Unit Tests (English with Korean Trigger Support)

## 🧠 Purpose
These instructions guide Copilot to generate **meaningful and readable Java unit tests** automatically.  
They are written in English, but also instruct Copilot to recognize **Korean test comments** such as `// 테스트:`.

---

## ✅ General Rules
When writing unit tests in **Java**, Copilot should:
- Use **JUnit 5** (`org.junit.jupiter.api.Test`)
- Use **Mockito** for mocking (`when(...)`, `verify(...)`)
- Automatically import required static methods from `org.mockito.Mockito`
- Recognize the following patterns as test prompts:
    - `// Test:`
    - `// 테스트:` (Korean equivalent of `// Test:`)
- Recognize both English and Korean structure comments:
    - `// Given` or `// 주어진 상황:` → setup mocks or data
    - `// When` or `// 실행:` → call the method being tested
    - `// Then` or `// 결과 확인:` → verify interactions or assertions
- Prefer expressive test names such as `methodName_shouldExpectedBehavior`
- Keep each test self-contained and readable (avoid unnecessary boilerplate)

---

## ✅ Behavior
When Copilot detects a recognized test comment (`// Test:` or `// 테스트:`), it should:
1. Generate a complete test method including `// Given`, `// When`, and `// Then` sections.
2. Insert realistic mock setup (`when(...)`, `verify(...)`, `assertThrows(...)` if applicable).
3. Use contextual variable names from the source file (e.g., `auctionRepository`, `chatProducer`, `userService`).

---

## ✅ Example Prompts
Copilot should respond to comments like:
```java
// 테스트: 회원가입 성공 시 userRepository.save() 호출된다
// 테스트: getChatRoom 호출 시 chatRoomRepository.findWithParticipantsById() 호출되고 chatRoom.addOrActivateMember(user)가 실행된다
// Test: verify that findAuctionById throws NOT_FOUND when missing
```

and produce corresponding **JUnit + Mockito** test code automatically.

---

## ✅ Example Output
```java
// 테스트: 회원가입 성공 시 userRepository.save() 호출된다
@Test
void 회원가입_성공시_저장된다() {
    // 주어진 상황
    SingUpRequest 요청 = new SingUpRequest("test@example.com", "password");
    when(userRepository.existsByEmail(요청.email())).thenReturn(false);
    when(passwordEncoder.encode(요청.password())).thenReturn("encoded");

    // 실행
    userService.signup(요청);

    // 결과 확인
    verify(userRepository).save(any(User.class));
}
```

---

## ✅ File Types
These instructions apply to:
- `*.java` files under `/src/test/java/`
- Especially any file ending with `*Test.java` or located in a package containing `service`, `controller`, or `consumer`.
