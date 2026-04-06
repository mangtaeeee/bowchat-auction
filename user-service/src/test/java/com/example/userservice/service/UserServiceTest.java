package com.example.userservice.service;

import com.example.userservice.dto.request.SingUpRequest;
import com.example.userservice.entity.User;
import com.example.userservice.event.OutboxEventPublisher;
import com.example.userservice.event.UserCreatedEvent;
import com.example.userservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    // 회원가입 로직은 저장소, 비밀번호 인코더, outbox 발행기만 검증하면 충분하다.
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private OutboxEventPublisher outboxEventPublisher;

    @InjectMocks
    private UserService userService;

    @Test
    void signupCreatesUserAndPublishesOutboxEvent() {
        // given: 신규 회원가입 요청과 저장 성공 상황을 준비한다.
        SingUpRequest request = new SingUpRequest("user@test.com", "plain-password", "tester");

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", 1L);
            return user;
        });

        userService.signup(request);

        // then: 저장된 User가 요청값과 인코딩 결과를 반영하는지 확인한다.
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo("user@test.com");
        assertThat(savedUser.getPassword()).isEqualTo("encoded-password");
        assertThat(savedUser.getNickname()).isEqualTo("tester");

        // then: 회원 생성 이벤트가 outbox 발행기로 전달되는지 확인한다.
        ArgumentCaptor<UserCreatedEvent> eventCaptor = ArgumentCaptor.forClass(UserCreatedEvent.class);
        verify(outboxEventPublisher).saveUserCreatedEvent(eventCaptor.capture());
        UserCreatedEvent event = eventCaptor.getValue();
        assertThat(event.userId()).isEqualTo(1L);
        assertThat(event.email()).isEqualTo("user@test.com");
        assertThat(event.nickName()).isEqualTo("tester");
    }

    @Test
    void signupRejectsDuplicateEmail() {
        // given: 이미 같은 이메일이 존재하는 상황을 만든다.
        SingUpRequest request = new SingUpRequest("duplicate@test.com", "plain-password", "tester");
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        // when/then: 중복 이메일이면 409를 던지고 후속 작업은 하지 않아야 한다.
        assertThatThrownBy(() -> userService.signup(request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        // 비밀번호 인코딩이나 outbox 발행은 호출되면 안 된다.
        verifyNoInteractions(passwordEncoder, outboxEventPublisher);
    }
}
