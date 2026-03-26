package sparta.paymentsystemserver.domain.payment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sparta.paymentsystemserver.domain.order.entity.Order;
import sparta.paymentsystemserver.domain.order.entity.OrderStatus;
import sparta.paymentsystemserver.domain.order.repository.OrderRepository;
import sparta.paymentsystemserver.domain.payment.dto.CreatePaymentRequest;
import sparta.paymentsystemserver.domain.payment.dto.CreatePaymentResponse;
import sparta.paymentsystemserver.domain.payment.dto.PaymentResultResponse;
import sparta.paymentsystemserver.domain.payment.entity.Payment;
import sparta.paymentsystemserver.domain.payment.entity.PaymentProvider;
import sparta.paymentsystemserver.domain.payment.entity.PaymentStatus;
import sparta.paymentsystemserver.domain.payment.exception.PaymentException;
import sparta.paymentsystemserver.domain.payment.repository.PaymentRepository;
import sparta.paymentsystemserver.domain.user.entity.User;
import sparta.paymentsystemserver.domain.user.repository.UserRepository;
import sparta.paymentsystemserver.global.client.PortOnePaymentClient;
import sparta.paymentsystemserver.global.client.dto.PortOnePaymentInfo;
import sparta.paymentsystemserver.global.exception.ErrorCode;
import sparta.paymentsystemserver.global.util.PublicIdGenerator;


// 결제 생성, 결제 확정 요청 진입, 외부 결제 상태 검증 흐름을 조율하는 서비스
// 실제 최종 상태 반영은 PaymentFinalizeService에 위임함
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final PublicIdGenerator publicIdGenerator;
    private final PortOnePaymentClient portOnePaymentClient;
    private final PaymentTransactionProcessor paymentTransactionProcessor;
    private final PaymentFinalizeService paymentFinalizeService;

    // 결제 시도 생성 메서드
    // 이 단계에서는 실제 결제를 확정하지 않고, 프론트가 포트원 결제창을 열기 전에 서버에 READY 상태 결제를 미리 저장하는 용도
    @Transactional
    public CreatePaymentResponse createPayment(CreatePaymentRequest request, Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new PaymentException(ErrorCode.USER_NOT_FOUND));

        Order order = orderRepository.findByOrderId(request.orderId())
                .orElseThrow(() -> new PaymentException(ErrorCode.ORDER_NOT_FOUND));

        // 로그인한 사용자의 주문이 아니면 접근 불가
        if (!order.getUser().getId().equals(userId)) {
            throw new PaymentException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        // 결제 대기 상태가 아닌 주문은 결제 생성 불가
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new PaymentException(ErrorCode.ORDER_NOT_PAYABLE);
        }

        // 프론트 금액과 서버 주문 금액 비교
        if (!order.getTotalAmount().equals(request.totalAmount())) {
            throw new PaymentException(ErrorCode.ORDER_AMOUNT_MISMATCH);
        }

        // 사용 포인트 기본 검증
        if (request.pointsToUse() < 0 || request.pointsToUse() > order.getTotalAmount()) {
            throw new PaymentException(ErrorCode.INVALID_POINTS_TO_USE);
        }

        // 보유 포인트보다 많이 사용하려는 경우
        if (request.pointsToUse() > user.getPointBalance()) {
            throw new PaymentException(ErrorCode.INSUFFICIENT_POINTS);
        }

        long externalAmount = order.getTotalAmount() - request.pointsToUse();
        PaymentProvider provider = externalAmount == 0 ? PaymentProvider.INTERNAL : PaymentProvider.PORTONE;
        String paymentId = publicIdGenerator.generate("PAY");

        Payment payment = Payment.ready(
                paymentId,
                order,
                user,
                provider,
                order.getTotalAmount(),
                request.pointsToUse(),
                externalAmount,
                "KRW"
        );

        paymentRepository.save(payment);

        return new CreatePaymentResponse(
                true,
                payment.getPaymentId(),
                order.getOrderId(),
                payment.getTotalAmount(),
                payment.getPointsToUse(),
                payment.getExternalAmount(),
                payment.getStatus().name()
        );
    }

    // 사용자가 직접 결제 확정을 요청하는 진입 메서드
    // - 최초 진입에서는 payment를 조회해서 권한만 검증 -> 외부 포트원 조회는 db 락 없이 먼저 수행
    // -> 실제 상태 변경이 필요한 시점에만 별도 메서드에서 비관적 락을 잡음
    public PaymentResultResponse confirmPayment(String paymentId, Long userId) {
        Payment payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new PaymentException(ErrorCode.PAYMENT_NOT_FOUND));

        // 본인 결제만 확정할 수 있도록 권한을 먼저 검증한다.
        if (!payment.getUser().getId().equals(userId)) {
            throw new PaymentException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        return confirmPaymentInternal(paymentId);
    }

    // 웹훅에서 결제 확정을 위임받아서 처리하는 진입 메서드
    // 웹훅 처리도 payment 존재만 확인하고 실제 상태 변경은 최종 반영 단계에서만 락 잡아서 처리
    public PaymentResultResponse confirmPaymentByWebhook(String paymentId) {
        paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new PaymentException(ErrorCode.PAYMENT_NOT_FOUND));

        return confirmPaymentInternal(paymentId);
    }



    // 실제 결제 확정 공통 로직
    // 1. payment를 락 없이 조회해서 현재 상태 확인
    // 2. 이미 PAID면 멱등하게 성공 응답 반환
    // 3. READY 상태가 아니면 확정 불가로 봄
    // 4. INTERNAL 결제는 외부 조회 없이 바로 최종 반영 단계로 감
    // 5. 포트원 결제는 외부 결제 상태를 먼저 조회함
    // 6. 외부 결제 상태와 금액 검증이 끝난 뒤에만 최종 반영 메서드에서 락 잡음
    private PaymentResultResponse confirmPaymentInternal(String paymentId) {
        Payment snapshot = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new PaymentException(ErrorCode.PAYMENT_NOT_FOUND));

        // 이미 결제 완료된 상태라면 멱등하게 성공 응답을 반환한다.
        if (snapshot.getStatus() == PaymentStatus.PAID) {
            return new PaymentResultResponse(
                    true,
                    snapshot.getPaymentId(),
                    snapshot.getOrder().getOrderId(),
                    snapshot.getStatus().name()
            );
        }

        // READY 상태인 결제만 확정 가능하다.
        if (snapshot.getStatus() != PaymentStatus.READY) {
            throw new PaymentException(ErrorCode.PAYMENT_CONFIRM_NOT_ALLOWED);
        }

        // 외부 PG 호출이 필요 없는 내부 결제는 바로 최종 반영 단계로 넘긴다.
        if (snapshot.getProvider() == PaymentProvider.INTERNAL) {
            return paymentFinalizeService.finalizeInternalPayment(paymentId);
        }

        // PortOne 결제는 외부 API를 먼저 호출해 실제 결제 상태를 검증한다.
        PortOnePaymentInfo paymentInfo = portOnePaymentClient.getPayment(paymentId);

        if (!paymentInfo.isPaid()) {
            paymentTransactionProcessor.handleVerificationFailure(
                    paymentId,
                    "PortOne 결제 상태가 PAID가 아닙니다."
            );
            throw new PaymentException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
        }

        if (paymentInfo.amount() != snapshot.getExternalAmount()) {
            paymentTransactionProcessor.handleVerificationFailure(
                    paymentId,
                    "PortOne 확인 금액과 서버 계산 금액이 일치하지 않습니다."
            );
            throw new PaymentException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        try {
            return paymentFinalizeService.finalizePortOnePayment(paymentId, paymentInfo.transactionId());
        } catch (Exception exception) {
            handleFinalizeFailureCompensation(snapshot);
            throw exception;
        }
    }

    // 결제 최종 반영이 실패한 뒤 수행하는 보상 처리
    // PaymentFinalizeService의 트랜잭션이 끝난 뒤에 호출되니까 여기서는 payment row 락이 해제된 상태에서 외부 취소 api를 호출함
    // - 내부 결제면 바로 보상 트랜잭션만 수행
    // - 포트원 결제면 외부 취소 api를 먼저 호출
    // - 외부 취소 성송하면 보상 트랜잭션 수행
    // - 외부 취소 실패하면 실패 이력 남김
    private void handleFinalizeFailureCompensation(Payment payment) {
        if (payment.getProvider() != PaymentProvider.PORTONE) {
            paymentTransactionProcessor.compensateAfterCancel(
                    payment.getPaymentId(), "결제 후처리 실패로 인한 내부 결제 보상"
            );
            return;
        }

        try {
            portOnePaymentClient.cancelPayment(
                    payment.getPaymentId(), "결제 확정 실패로 인한 자동 취소"
            );

            paymentTransactionProcessor.compensateAfterCancel(
                    payment.getPaymentId(), "결제 확정 실패로 인한 자동 취소"
            );
        } catch (Exception cancelException) {
            paymentTransactionProcessor.markCompensationFailure(
                    payment.getPaymentId(), "결제 후처리 실패 및 PortOne 자동 취소 실패"
            );

            throw new PaymentException(ErrorCode.REFUND_PROCESS_FAILED);
        }
    }

}
