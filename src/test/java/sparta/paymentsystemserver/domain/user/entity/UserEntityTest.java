package sparta.paymentsystemserver.domain.user.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sparta.paymentsystemserver.domain.membership.entity.MembershipGradeType;
import sparta.paymentsystemserver.domain.point.exception.InsufficientPointException;
import sparta.paymentsystemserver.domain.point.exception.InvalidPointException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserEntityTest {

    private User user;
    private User oauthUser;

    @BeforeEach
    void setUp() {
        user = new User("팀스파르타", "test@test.com",
                "password", "010-1234-5678", "USR-001");

        oauthUser = new User("내배캠", "coding@test.com", "010-0000-0000", "USR-002", null, null);
    }

    @Test
    @DisplayName("정보 수정 성공 - 사용자 정보가 정상적으로 변경된다.")
    void edit_userInfo_success() {
        // given
        String name = "르탄이";
        String phone = "010-7777-7777";

        // when
        user.update(name, phone);

        // then
        assertThat(user.getName()).isEqualTo(name);
        assertThat(user.getPhone()).isEqualTo(phone);
    }

    @Test
    @DisplayName("정보 수정 성공 - 이름이 null이면 기존 이름 정보가 유지된다.")
    void edit_userInfo_name_is_null_success() {
        // given
        String name = null;
        String phone = "010-7777-7777";

        // when
        user.update(name, phone);

        // then
        assertThat(user.getName()).isEqualTo("팀스파르타");
        assertThat(user.getPhone()).isEqualTo(phone);
    }

    @Test
    @DisplayName("정보 수정 성공 - 전화번호가 null이면 기존 전화번호 정보가 유지된다.")
    void edit_userInfo_phone_is_null_success() {
        // given
        String name = "르탄이";
        String phone = null;

        // when
        user.update(name, phone);

        // then
        assertThat(user.getName()).isEqualTo("르탄이");
        assertThat(user.getPhone()).isEqualTo("010-1234-5678");
    }

    @Test
    @DisplayName("포인트 적립 성공 - 포인트 잔액이 정상적으로 증가한다.")
    void addPoint_success() {
        // given
        Long pointBalance = user.getPointBalance();
        long amount = 10000L;

        // when
        user.addPoint(amount);

        // then
        assertThat(user.getPointBalance()).isEqualTo(pointBalance + amount);
    }

    @Test
    @DisplayName("포인트 적립 실패 - 적립하려는 포인트가 0 이하이면 예외가 발생한다.")
    void addPoint_amountIsZeroOrLess_fail() {
        // given
        long amount = 0;

        // when & then
        assertThatThrownBy(() -> user.addPoint(amount))
                .isInstanceOf(InvalidPointException.class)
                .hasMessage("포인트 금액이 올바르지 않습니다.");
    }

    @Test
    @DisplayName("포인트 차감 성공 - 포인트가 정상적으로 차감된다.")
    void subtractPoint_success() {
        // given
        user.addPoint(20000L);
        long amount = 10000L;

        // when
        user.subtractPoint(amount);

        // then
        assertThat(user.getPointBalance()).isEqualTo(10000L);
    }

    @Test
    @DisplayName("포인트 차감 실패 - 차감하려는 포인트가 0 이하이면 예외가 발생한다.")
    void subtractPoint_amountIsZeroOrLess_fail() {
        // given
        long amount = 0L;

        // when & then
        assertThatThrownBy(() -> user.subtractPoint(amount))
                .isInstanceOf(InvalidPointException.class)
                .hasMessage("포인트 금액이 올바르지 않습니다.");
    }

    @Test
    @DisplayName("포인트 차감 실패 - 차감하려는 포인트가 잔여 포인트보다 많으면 예외가 발생한다.")
    void subtractPoint_insufficientBalance_fail() {
        // given
        user.addPoint(10000L);
        long amount = 20000L;

        // when & then
        assertThatThrownBy(() -> user.subtractPoint(amount))
                .isInstanceOf(InsufficientPointException.class)
                .hasMessage("포인트 잔액이 부족합니다.");
    }

    @Test
    @DisplayName("누적 결제금액 차감 성공 - 누적 결제금액이 정상적으로 차감된다.")
    void subtractTotalPaidAmount_success() {
        // given
        user.addTotalPaidAmount(10000L);

        // when
        user.subtractTotalPaidAmount(5000L);

        // then
        assertThat(user.getTotalPaidAmount()).isEqualTo(5000L);
    }

    @Test
    @DisplayName("누적 결제금액 차감 성공 - 잔액보다 많이 차감해도 0 이하로 내려가지 않는다.")
    void subtractTotalPaidAmount_floor_zero() {
        // given
        user.addTotalPaidAmount(10000L);

        // when
        user.subtractTotalPaidAmount(50000L);

        // then
        assertThat(user.getTotalPaidAmount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("누적 결제금액 적립 성공 - 누적 결제금액이 정상적으로 증가된다.")
    void addTotalPaidAmount_success() {
        // given
        long amount = 10000L;

        // when
        user.addTotalPaidAmount(amount);

        // then
        assertThat(user.getTotalPaidAmount()).isEqualTo(10000L);
    }

    @Test
    @DisplayName("멤버십 등급 변경 성공 - 멤버십 등급이 정상적으로 변경된다.")
    void edit_user_membershipGrade_success() {
        // given
        MembershipGradeType gradeType = MembershipGradeType.VIP;

        // when
        user.updateGrade(gradeType);

        // then
        assertThat(user.getMembershipGrade()).isEqualTo(MembershipGradeType.VIP);
    }
}