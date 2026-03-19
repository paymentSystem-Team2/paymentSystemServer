package sparta.paymentsystemserver.domain.order.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sparta.paymentsystemserver.domain.order.dto.CreateOrderItemRequest;
import sparta.paymentsystemserver.domain.order.dto.CreateOrderRequest;
import sparta.paymentsystemserver.domain.order.dto.CreateOrderResponse;
import sparta.paymentsystemserver.domain.order.entity.Order;
import sparta.paymentsystemserver.domain.order.entity.OrderItem;
import sparta.paymentsystemserver.domain.order.entity.OrderStatus;
import sparta.paymentsystemserver.domain.order.repository.OrderItemRepository;
import sparta.paymentsystemserver.domain.order.repository.OrderRepository;
import sparta.paymentsystemserver.domain.product.entity.Product;
import sparta.paymentsystemserver.domain.product.exception.ProductException;
import sparta.paymentsystemserver.domain.product.exception.ProductStockException;
import sparta.paymentsystemserver.domain.product.repository.ProductRepository;
import sparta.paymentsystemserver.domain.user.entity.User;
import sparta.paymentsystemserver.domain.user.service.UserService;
import sparta.paymentsystemserver.global.exception.ErrorCode;
import sparta.paymentsystemserver.global.util.PublicIdGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final PublicIdGenerator publicIdGenerator;
    private final UserService userService;

    public CreateOrderResponse createOrder(Long userId, CreateOrderRequest request){
        User user = userService.findById(userId);
        // 서버가 직접 계산한 총 주문 금액
        long caculatedTotalAmount = 0L;

        // Order 저장 후 한 번에 저장할 OrderItem 목록
        List<OrderItem> orderItems = new ArrayList<>();

        // 요청 상품 목록 순회
        for (CreateOrderItemRequest itemRequest : request.items()){

            // 상품 조회
            Product product = productRepository.findByProductId(itemRequest.productId())
                    .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND));

            // 요청 수량이 재고보다 많으면 주문 생성 실패
            if(product.getStock() < itemRequest.quantity()){
                throw new ProductStockException(ErrorCode.PRODUCT_OUT_OF_STOCK);
            }

            // 서버 기준 총 주문 금액 계산
            caculatedTotalAmount += product.getPrice() * itemRequest.quantity();
        }

        // 주문 ID , 주문 번호 생서
        String orderId = publicIdGenerator.generate("ORD");
        String orderNumber = publicIdGenerator.generate("ONO");

        // 주문 생성 (포인트 사용 전이기 때문에 0, 결제대기)
        Order order = new Order(
                null,
                orderId,
                orderNumber,
                user,
                caculatedTotalAmount,
                0L,
                0L,
                OrderStatus.PENDING_PAYMENT,
                LocalDateTime.now()
        );

        // 주문 저장
        Order saveOrder = orderRepository.save(order);

        // 주문 상품 생성
        for (CreateOrderItemRequest itemRequest : request.items()) {
            Product product = productRepository.findByProductId(itemRequest.productId())
                    .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND));

            OrderItem orderItem = new OrderItem(
                    null,
                    saveOrder,
                    product,
                    itemRequest.quantity(),
                    product.getName(),
                    product.getPrice(),
                    LocalDateTime.now()
            );

            orderItems.add(orderItem);
        }

        // 주문 상품 전체 저장
        orderItemRepository.saveAll(orderItems);

        // 프론트에 필요한 값만 반환
        return new CreateOrderResponse(
                saveOrder.getOrderId(),
                saveOrder.getTotalAmount(),
                saveOrder.getOrderNumber()
        );
    }

}

