package sparta.paymentsystemserver.domain.subscription.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sparta.paymentsystemserver.domain.subscription.entity.PaymentMethod;

import java.util.List;
import java.util.Optional;

// 빌링키 조회용 레포지토리
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    // 포트원 연동 때 customerUid 기준으로 기존 빌링키가 있는지 확인할 때 사용
    List<PaymentMethod> findAllByCustomerUid(String customerUid);

    // 특정 사용자의 활성화되어 있는 결제수단을 조회한다
    List<PaymentMethod> findAllByUserIdAndActiveTrue(Long userId);

    // 사용자가 이미 등록한 빌링키인지 체크할 때 씀
    Optional<PaymentMethod> findByUserIdAndBillingKey(Long userId, String billingKey);

}
