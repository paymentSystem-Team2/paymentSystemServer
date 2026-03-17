package sparta.paymentsystemserver.domain.payment.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import sparta.paymentsystemserver.global.config.BaseEntity;

// 환불 이력 엔티티
// 결제 1건당 환불 1건 허용. 디비에 유니크 제약으로도 중복 환불을 막음
@Getter
@Entity
@Table(name = "refunds")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Refund extends BaseEntity {

    // DB 내부 기본키
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 외부 응답이랑 로그에 사용할 공개 환불 ID
    @Column(nullable = false, unique = true, length = 40)
    private String refundId;

    // 어떤 결제에 대한 환불인지 가리키는 1:1 참조
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false, unique = true)
    private Payment payment;

    // 환불된 금액
    @Column(nullable = false)
    private long refundAmount;

    // 환불 사유 메모
    @Column(length = 300)
    private String reason;

    // 외부 PG가 내려준 환불 거래 번호
    @Column(length = 100)
    private String providerRefundId;

    @Builder
    private Refund(
            String refundId,
            Payment payment,
            long refundAmount,
            String reason,
            String providerRefundId
    ) {
        this.refundId = refundId;
        this.payment = payment;
        this.refundAmount = refundAmount;
        this.reason = reason;
        this.providerRefundId = providerRefundId;
    }

    // 환불 이력을 생성하는 정적 팩토리 메서드
    public static Refund create(String refundId, Payment payment, long refundAmount, String reason, String providerRefundId) {
        return Refund.builder()
                .refundId(refundId)
                .payment(payment)
                .refundAmount(refundAmount)
                .reason(reason)
                .providerRefundId(providerRefundId)
                .build();
    }



}
