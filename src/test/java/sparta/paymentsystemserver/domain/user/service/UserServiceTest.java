package sparta.paymentsystemserver.domain.user.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import sparta.paymentsystemserver.domain.auth.dto.SignupResponse;
import sparta.paymentsystemserver.domain.membership.entity.MembershipGradePolicy;
import sparta.paymentsystemserver.domain.membership.entity.MembershipGradeType;
import sparta.paymentsystemserver.domain.membership.exception.MembershipNotFoundException;
import sparta.paymentsystemserver.domain.membership.service.MembershipService;
import sparta.paymentsystemserver.domain.user.dto.UserRequest;
import sparta.paymentsystemserver.domain.user.dto.UserResponse;
import sparta.paymentsystemserver.domain.user.dto.UserUpdateRequest;
import sparta.paymentsystemserver.domain.user.entity.User;
import sparta.paymentsystemserver.domain.user.exception.DuplicateEmailException;
import sparta.paymentsystemserver.domain.user.exception.UserNotFoundException;
import sparta.paymentsystemserver.domain.user.repository.UserRepository;
import sparta.paymentsystemserver.global.exception.ErrorCode;
import sparta.paymentsystemserver.global.util.PublicIdGenerator;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PublicIdGenerator publicIdGenerator;

    @Mock
    private MembershipService membershipService;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("회원가입 실패 - 이미 존재하는 이메일은 예외가 발생한다.")
    void signup_duplicated_email_fail() {
        // given
        UserRequest userRequest = new UserRequest();
        ReflectionTestUtils.setField(userRequest, "name", "르탄이");
        ReflectionTestUtils.setField(userRequest, "email", "test@test.com");
        ReflectionTestUtils.setField(userRequest, "password", "password");
        ReflectionTestUtils.setField(userRequest, "phone", "010-1234-5678");

        given(userRepository.existsByEmail("test@test.com")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.save(userRequest))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessage("이미 존재하는 이메일입니다.");
    }

    @Test
    @DisplayName("회원가입 성공 - 존재하지 않는 이메일은 성공적으로 가입한다.")
    void signup_success() {
        // given
        UserRequest request = new UserRequest();
        ReflectionTestUtils.setField(request, "name", "르탄이");
        ReflectionTestUtils.setField(request, "email", "test@test.com");
        ReflectionTestUtils.setField(request, "password", "password");
        ReflectionTestUtils.setField(request, "phone", "010-1234-5678");

        given(userRepository.existsByEmail("test@test.com")).willReturn(false);
        given(passwordEncoder.encode("password")).willReturn("encodedPassword");
        given(publicIdGenerator.generate("USR")).willReturn("USR-001");

        // when
        SignupResponse response = userService.save(request);

        // then
        assertThat(response.success()).isTrue();
        assertThat(response.message()).isEqualTo("성공적으로 회원 가입하였습니다.");
    }

    @Test
    @DisplayName("회원 조회 실패 - 회원 Id가 유효하지 않다면, 조회에 실패한다.")
    void getUser_invalid_userId_fail() {
        // given
        Long userId = 1L;

        given(userRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.getUser(userId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("유저가 존재하지 않습니다.");
    }

    @Test
    @DisplayName("회원 조회 성공 - 회원 Id가 유효하면 조회에 성공한다.")
    void getUser_success() {
        // given
        Long userId = 1L;
        User user = new User("르탄이", "test@test.com",
                "password", "010-1234-5678", "USR-001");

        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        // when
        UserResponse response = userService.getUser(userId);

        // then
        assertThat(response.name()).isEqualTo("르탄이");
        assertThat(response.email()).isEqualTo("test@test.com");
        assertThat(response.phone()).isEqualTo("010-1234-5678");
        assertThat(response.customerUid()).isEqualTo("USR-001");
        assertThat(response).isInstanceOf(UserResponse.class);
    }

    @Test
    @DisplayName("회원 정보 수정 실패 - 회원 Id가 유효하지 않다면, 수정에 실패한다.")
    void update_invalid_userId_fail() {
        // given
        Long userId = 1L;
        UserUpdateRequest request = new UserUpdateRequest();
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.update(userId, request))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("유저가 존재하지 않습니다.");
    }

    @Test
    @DisplayName("회원 정보 수정 성공 - 회원 Id가 유효하면 수정에 성공한다.")
    void update_success() {
        // given
        Long userId = 1L;
        User user = new User("팀스파르타", "test@test.com",
                "password", "010-7777-7777", "USR-001");

        UserUpdateRequest request = new UserUpdateRequest();
        ReflectionTestUtils.setField(request, "name", "르탄이");
        ReflectionTestUtils.setField(request, "phone", "010-1234-5678");

        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        // when
        UserResponse response = userService.update(userId, request);

        // then
        assertThat(response).isInstanceOf(UserResponse.class);
        assertThat(response.name()).isEqualTo("르탄이");
        assertThat(response.phone()).isEqualTo("010-1234-5678");
    }

    @Test
    @DisplayName("회원 등급 계산 성공 - 누적 결제금액이 조건 미달이면 NORMAL 등급이 된다.")
    void calculateGrade_normal_success() {
        // given
        User user = new User("팀스파르타", "test@test.com",
                "password", "010-7777-7777", "USR-001");
        user.addTotalPaidAmount(30_000L);

        MembershipGradePolicy normalPolicy = mock(MembershipGradePolicy.class);
        given(normalPolicy.getMembershipCode()).willReturn(MembershipGradeType.NORMAL);
        given(normalPolicy.getMinTotalPaidAmount()).willReturn(0L);

        MembershipGradePolicy vipPolicy = mock(MembershipGradePolicy.class);
        given(vipPolicy.getMinTotalPaidAmount()).willReturn(100_000L);

        MembershipGradePolicy vvipPolicy = mock(MembershipGradePolicy.class);
        given(vvipPolicy.getMinTotalPaidAmount()).willReturn(300_000L);

        given(membershipService.findPolicyByType())
                .willReturn(List.of(normalPolicy, vipPolicy, vvipPolicy));

        // when
        userService.calculateGrade(user);

        // then
        assertThat(user.getMembershipGrade()).isEqualTo(MembershipGradeType.NORMAL);
    }

    @Test
    @DisplayName("회원 등급 계산 성공 - 누적 결제금액이 VIP 조건을 충족하면 VIP 등급이 된다.")
    void calculateGrade_vip_success() {
        // given
        User user = new User("팀스파르타", "test@test.com",
                "password", "010-7777-7777", "USR-001");
        user.addTotalPaidAmount(120_000L);

        MembershipGradePolicy normalPolicy = mock(MembershipGradePolicy.class);
        given(normalPolicy.getMinTotalPaidAmount()).willReturn(0L);

        MembershipGradePolicy vipPolicy = mock(MembershipGradePolicy.class);
        given(vipPolicy.getMembershipCode()).willReturn(MembershipGradeType.VIP);
        given(vipPolicy.getMinTotalPaidAmount()).willReturn(100_000L);

        MembershipGradePolicy vvipPolicy = mock(MembershipGradePolicy.class);
        given(vvipPolicy.getMinTotalPaidAmount()).willReturn(300_000L);

        given(membershipService.findPolicyByType())
                .willReturn(List.of(normalPolicy, vipPolicy, vvipPolicy));

        // when
        userService.calculateGrade(user);

        // then
        assertThat(user.getMembershipGrade()).isEqualTo(MembershipGradeType.VIP);
    }

    @Test
    @DisplayName("회원 등급 계산 성공 - 누적 결제금액이 VVIP 조건을 충족하면 VIP 등급이 된다.")
    void calculateGrade_vvip_success() {
        // given
        User user = new User("팀스파르타", "test@test.com",
                "password", "010-7777-7777", "USR-001");
        user.addTotalPaidAmount(900_000L);

        MembershipGradePolicy normalPolicy = mock(MembershipGradePolicy.class);
        given(normalPolicy.getMinTotalPaidAmount()).willReturn(0L);

        MembershipGradePolicy vipPolicy = mock(MembershipGradePolicy.class);
        given(vipPolicy.getMinTotalPaidAmount()).willReturn(100_000L);

        MembershipGradePolicy vvipPolicy = mock(MembershipGradePolicy.class);
        given(vvipPolicy.getMembershipCode()).willReturn(MembershipGradeType.VVIP);
        given(vvipPolicy.getMinTotalPaidAmount()).willReturn(300_000L);

        given(membershipService.findPolicyByType())
                .willReturn(List.of(normalPolicy, vipPolicy, vvipPolicy));

        // when
        userService.calculateGrade(user);

        // then
        assertThat(user.getMembershipGrade()).isEqualTo(MembershipGradeType.VVIP);
    }

    @Test
    @DisplayName("회원 등급 조회 실패 - 존재하지 않는 멤버십 등급을 조회하려고 하면 예외가 발생한다.")
    void getUserMemberShip_notExistsMemberShip_fail() {
        // given
        User user = new User("팀스파르타", "test@test.com",
                "password", "010-7777-7777", "USR-001");

        MembershipGradePolicy vipPolicy = mock(MembershipGradePolicy.class);
        given(vipPolicy.getMembershipCode()).willReturn(MembershipGradeType.VIP);

        MembershipGradePolicy vvipPolicy = mock(MembershipGradePolicy.class);
        given(vvipPolicy.getMembershipCode()).willReturn(MembershipGradeType.VVIP);

        given(membershipService.findPolicyByType())
                .willReturn(List.of(vipPolicy, vvipPolicy));

        // when & then
        assertThatThrownBy(() -> userService.getUserMemberShip(user))
                .isInstanceOf(MembershipNotFoundException.class)
                .hasMessage("멤버십 등급 정책을 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("회원 등급 조회 성공")
    void getUserMemberShip_success() {
        // given
        User user = new User("팀스파르타", "test@test.com",
                "password", "010-7777-7777", "USR-001");
        ReflectionTestUtils.setField(user, "membershipGrade", MembershipGradeType.NORMAL);

        MembershipGradePolicy normalPolicy = mock(MembershipGradePolicy.class);
        given(normalPolicy.getMembershipCode()).willReturn(MembershipGradeType.NORMAL);

        MembershipGradePolicy vipPolicy = mock(MembershipGradePolicy.class);

        MembershipGradePolicy vvipPolicy = mock(MembershipGradePolicy.class);

        given(membershipService.findPolicyByType())
                .willReturn(List.of(normalPolicy, vipPolicy, vvipPolicy));

        // when
        userService.getUserMemberShip(user);

        // then
        assertThat(user.getMembershipGrade()).isEqualTo(MembershipGradeType.NORMAL);
    }
}