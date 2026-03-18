package sparta.paymentsystemserver.domain.payment.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.dialect.unique.CreateTableUniqueDelegate;
import sparta.paymentsystemserver.domain.user.entity.User;
import sparta.paymentsystemserver.global.config.BaseEntity;

import java.time.LocalDateTime;

// 결제 엔티티는 외부 결제 시도와 내부 확정 결과를 함께 관리
// 주문과 분리해 두고 같은 주문에 대한 재시도 이력, 실패 이력, 환불 이력을 결제 단위로 남기겠습니다
@Getter
@Entity
@Table(name = "payments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

    // 데이터베이스 내부 기본키
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 외부 결제 조회와 내부 응답에 모두 사용하는 공개 결제 식별자
    @Column(nullable = false, unique = true, length = 40)
    private String paymentId;

    // 어떤 주문에 대한 결제 시도인지 연결하는 참조
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "order_id")
//    private Order order;

    // 결제를 수행한 사용자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 외부 PortOne 결제인지, 포인트 전액 결제용 내부 처리인지 구분
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentProvider provider;

    // 결제 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    // 포인트 사용 전 주문 기준의 총 결제 금액
    @Column(nullable = false)
    private Long totalAmount;

    // 이번 결제에서 사용하기로 한 포인트 금액
    @Column(nullable = false)
    private long pointsToUse;

    // 외부 PG가 실제로 청구해야 하는 금액. 포인트
    @Column(nullable = false)
    private long externalAmount;

    // 통화 코드
    @Column(nullable = false, length = 10)
    private String currency;

    // PortOne 등 외부 결제사에서 내려준 거래 번호
    @Column(length = 100)
    private String providerTransactionId;

    // 결제가 실제 승인된 시각
    private LocalDateTime approvedAt;

    // 환불이 완료된 시각
    private LocalDateTime refundedAt;

    // 결제 실패 시 원인을 남기는 필드
    @Column(length = 300)
    private String failureReason;
//
//    @Builder
//    private Payment(
//            String paymentId,
//            Order order,
//            User user,
//            PaymentProvider provider,
//            PaymentStatus status,
//            long totalAmount,
//            long pointsToUse,
//            long externalAmount,
//            String currency
//    ) {
//        this.paymentId = paymentId;
//        this.order = order;
//        this.user = user;
//        this.provider = provider;
//        this.status = status;
//        this.totalAmount = totalAmount;
//        this.pointsToUse = pointsToUse;
//        this.externalAmount = externalAmount;
//        this.currency = currency;
//    }

    // 결제 시도 생성 시점에는 아직 확정 전이라 READY 상태로 시작
//    public static Payment ready(
//            String paymentId,
//            Order order,
//            User user,
//            PaymentProvider provider,
//            long totalAmount,
//            long pointsToUse,
//            long externalAmount,
//            String currency
//    ) {
//        return Payment.builder()
//                .paymentId(paymentId)
//                .order(order)
//                .user(user)
//                .provider(provider)
//                .status(PaymentStatus.READY)
//                .totalAmount(totalAmount)
//                .pointsToUse(pointsToUse)
//                .externalAmount(externalAmount)
//                .currency(currency)
//                .build();
//    }

    // 외부 결제 또는 내부 포인트 결제가 최종 승인할 시에 호출
    public void markPaid(String providerTransactionId) {
        this.status = PaymentStatus.PAID;
        this.providerTransactionId = providerTransactionId;
        this.approvedAt = LocalDateTime.now();
        this.failureReason = null;
    }

    // 외부 검증 실패나 내부 검증 실패 시 실패 사유 같이 남김
    public void markFailed(String failureReason) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = failureReason;
    }

    // 같은 주문에서 더이상 사용하지 않을 레디 결제를 정리할 때 호출
    public void expireReady(String failureReason) {
        if (this.status == PaymentStatus.READY) {
            this.status = PaymentStatus.FAILED;
            this.failureReason = failureReason;
        }
    }

    // 환불 완료 후에 같은 결제를 다시 정상 결제로 보지 않도록 상태 변경
    public void markRefunded() {
        this.status = PaymentStatus.REFUNDED;
        this.refundedAt = LocalDateTime.now();
    }

}
