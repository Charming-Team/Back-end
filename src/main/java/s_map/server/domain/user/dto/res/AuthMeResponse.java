package s_map.server.domain.user.dto.res;

import lombok.Builder;
import lombok.Getter;
import s_map.server.domain.user.entity.User;

@Getter
@Builder
public class AuthMeResponse {

    private Long id;
    private String name;
    private String email;
    private String role;
    private String department;
    private String companyName;
    private String phoneNumber;

    public static AuthMeResponse from(User user) {
        return AuthMeResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .department(user.getDepartment())
                .companyName(user.getCompanyName())
                .phoneNumber(user.getPhoneNumber())
                .build();
    }
}