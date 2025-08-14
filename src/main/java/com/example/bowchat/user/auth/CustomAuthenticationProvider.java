package com.example.bowchat.user.auth;

import com.example.bowchat.user.entity.PrincipalDetails;
import com.example.bowchat.user.entity.ProviderType;
import com.example.bowchat.user.entity.User;
import com.example.bowchat.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationProvider implements AuthenticationProvider {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String email = authentication.getName();
        String rawPassword = authentication.getCredentials().toString();

        log.info("CustomAuthenticationProvider - 사용자 인증 시작: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("사용자 없음: {}", email);
                    return new BadCredentialsException("존재하지 않는 사용자입니다.");
                });

        if (user.getProvider() != ProviderType.LOCAL) {
            throw new BadCredentialsException("SNS 로그인 계정입니다. 일반 로그인 불가");
        }

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            log.error("비밀번호 불일치: {}", email);
            throw new BadCredentialsException("비밀번호가 올바르지 않습니다.");
        }

        log.info("사용자 인증 성공: {}", email);

        PrincipalDetails principalDetails = new PrincipalDetails(user);

        return new UsernamePasswordAuthenticationToken(
                principalDetails,
                null,
                principalDetails.getAuthorities()
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
