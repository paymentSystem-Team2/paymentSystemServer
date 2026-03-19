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
import sparta.paymentsystemserver.global.exception.ErrorCode;
import sparta.paymentsystemserver.global.util.PublicIdGenerator;


// 결제 시도 생성과 결제 확정을 담당하는 서비스입니다
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final PublicIdGenerator publicIdGenerator;
    private final PortOnePaymentClient portOnePaymentClient;

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
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
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

    // 결제 확정 메서드
    // 클라이언트가 결제 성공이라고 보내더라도 서버가 PortOne 결제 조회 API를 호출해 최종 상태를 검증한 뒤에만 결제를 확정
    @Transactional
    public PaymentResultResponse confirmPayment(String paymentId, Long userId) {

        Payment payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new PaymentException(ErrorCode.PAYMENT_NOT_FOUND));

        // 본인 결제만 확정할 수 있도록 소유권을 검사
        if (!payment.getUser().getId().equals(userId)) {
            throw new PaymentException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        return confirmPaymentInternal(payment);
    }


    // 내부 공용 결제 확정 메서드
    // 클라이언트 쪽의 confirm과 webhook confirm이 같은 로직을 재사용하도록 분리
    @Transactional
    public PaymentResultResponse confirmPaymentByWebhook(String paymentId) {
        Payment payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new PaymentException(ErrorCode.PAYMENT_NOT_FOUND));

        return confirmPaymentInternal(payment);
    }



    // 실제 결제 확정 공통 로직
    // 결제 상태, 포트원 조회 결과, 금액 일치 여부를 검증한 뒤에 최족 확정
    private PaymentResultResponse confirmPaymentInternal(Payment payment) {

        // 이미 결제 완료된 상태라면 멱등하게 그대로 성공 응답 반환
        if (payment.getStatus() == PaymentStatus.PAID) {
            return new PaymentResultResponse(
                    true,
                    payment.getPaymentId(),
                    payment.getOrder().getOrderId(),
                    payment.getStatus().name()
            );
        }

        // READY 상태만 확정 가능
        if (payment.getStatus() != PaymentStatus.READY) {
            throw new PaymentException(ErrorCode.PAYMENT_CONFIRM_NOT_ALLOWED);
        }

        // 내부 포인트 전액 결제는 외부 조회 없이 바로 성공 처리
        if (payment.getProvider() == PaymentProvider.INTERNAL) {
            payment.markPaid("INTERNAL");
            payment.getOrder().complete();

            return new PaymentResultResponse(
                    true,
                    payment.getPaymentId(),
                    payment.getOrder().getOrderId(),
                    payment.getStatus().name()
            );
        }

        // 포트원 재조회
        PortOnePaymentClient.PortOnePaymentInfo paymentInfo =
                portOnePaymentClient.getPayment(payment.getPaymentId());

        if (!paymentInfo.isPaid()) {
            payment.markFailed("PortOne 결제 상태가 PAID가 아닙니다.");
            throw new PaymentException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
        }

        if (paymentInfo.amount() != payment.getExternalAmount()) {
            payment.markFailed("PortOne 승인 금액과 서버 계산 금액이 일치하지 않습니다.");
            throw new PaymentException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        payment.markPaid(paymentInfo.transactionId());
        payment.getOrder().complete();

        return new PaymentResultResponse(
                true,
                payment.getPaymentId(),
                payment.getOrder().getOrderId(),
                payment.getStatus().name()
        );
    }

}
