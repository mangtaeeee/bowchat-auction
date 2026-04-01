package com.example.userservice.entity;


import com.example.userservice.dto.request.SingUpRequest;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "USERS",
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

//    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
//    private List<ChatRoomParticipant> chatRooms = new ArrayList<>(); // TOTO :: 의존성 분리 필요
//
//    private List<Long> chatRoomIds = new ArrayList<>(); // ( ChatRoom 를 관리하는 서비스에서는 유저 아이디별 사용하고 있는 쳇 룸의 아이디를 제공해줄 필요가 있음 또는 아예 유저가 챗 룸에 대한 정보를 몰라도 될 수도 있음 )

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
