package com.example.bowchat.user.entity;


import com.example.bowchat.user.dto.SingUpRequest;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"provider", "providerId"})
        }
)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class User {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String email;

    // 일반 로그인용. OAuth2 로그인은 null일 수 있음
    private String password;

    @Column(nullable = false)
    private String nickname;

    // 로그인 제공자: local, google 등
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProviderType provider;

    @Column
    private String providerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    public void updateNickname(String newNickname) {
        this.nickname = newNickname;
    }




    public static User createLocalUserFromRequest(SingUpRequest signInRequest, String password) {
        return User.builder()
                .email(signInRequest.email())
                .password(password)
                .nickname(signInRequest.nickName())
                .provider(ProviderType.LOCAL)
                .role(Role.USER)
                .build();
    }


}
