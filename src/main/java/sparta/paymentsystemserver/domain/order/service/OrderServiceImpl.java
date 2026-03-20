package sparta.paymentsystemserver.domain.order.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sparta.paymentsystemserver.domain.order.dto.*;
import sparta.paymentsystemserver.domain.order.entity.Order;
import sparta.paymentsystemserver.domain.order.entity.OrderItem;
import sparta.paymentsystemserver.domain.order.entity.OrderStatus;
import sparta.paymentsystemserver.domain.order.exception.OrderException;
import sparta.paymentsystemserver.domain.order.repository.OrderItemRepository;
import sparta.paymentsystemserver.domain.order.repository.OrderRepository;
import sparta.paymentsystemserver.domain.product.entity.Product;
import sparta.paymentsystemserver.domain.product.exception.ProductStockException;
import sparta.paymentsystemserver.domain.product.service.ProductService;
import sparta.paymentsystemserver.domain.user.entity.User;
import sparta.paymentsystemserver.domain.user.service.UserService;
import sparta.paymentsystemserver.global.exception.ErrorCode;
import sparta.paymentsystemserver.global.util.PublicIdGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductService productService;
    private final PublicIdGenerator publicIdGenerator;
    private final UserService userService;

    public CreateOrderResponse createOrder(Long userId, CreateOrderRequest request) {
        User user = userService.findById(userId);

        List<String> productIds = request.items().stream().map(CreateOrderItemRequest::productId).toList();

        List <Product> products = productService.getValidProductList(productIds);
        Map<String, Product> productMap = generateProductMap(products);

        long calculatedTotalAmount = calculateTotalAmount(productMap, request);

        // 주문 ID , 주문 번호 생성
        String orderId = publicIdGenerator.generate("ORD");
        String orderNumber = publicIdGenerator.generate("ONO");

        // 주문 생성
        Order order = new Order(
                orderId,
                orderNumber,
                user,
                calculatedTotalAmount,
                OrderStatus.PENDING,
                LocalDateTime.now()
        );

        // 주문 저장
        Order saveOrder = orderRepository.save(order);
        createOrderItems(productMap,order,request);
        return new CreateOrderResponse(
                saveOrder.getOrderId(),
                saveOrder.getTotalAmount(),
                saveOrder.getOrderNumber()
        );
    }

    private Long calculateTotalAmount(Map<String, Product> productMap, CreateOrderRequest request) {
        long totalAmount = 0L;

        for (CreateOrderItemRequest item : request.items()) {
            Product product = productMap.get(item.productId());
            if (product.getStock() < item.quantity()) {
                throw new ProductStockException(ErrorCode.PRODUCT_OUT_OF_STOCK);
            }
            totalAmount += product.getPrice() * item.quantity();
        }
        return totalAmount;
    }

    // order item 생성 여기서 재고 차감
    private void createOrderItems(Map<String, Product> productMap, Order order, CreateOrderRequest request) {
        List<OrderItem> orderItems = new ArrayList<>();

        for (CreateOrderItemRequest item : request.items()) {
            Product product = productMap.get(item.productId());
            OrderItem orderItem = new OrderItem(
                    order,
                    product,
                    item.quantity(),
                    product.getName(),
                    product.getPrice()
            );
            product.decreaseStock(item.quantity());
            orderItems.add(orderItem);
        }
        orderItemRepository.saveAll(orderItems);
    }

    private Map<String, Product> generateProductMap(List<Product> productList) {
        return productList.stream()
                .collect(Collectors.toMap(Product::getProductId, Function.identity()));
    }

    // 주문 목록 조회
    @Transactional(readOnly = true)
    public List<GetOrderListResponse> getMyOrders(Long userId) {
        return orderRepository.findByUserIdOrderByOrderedAtDesc(userId)
                .stream()
                .map(order -> new GetOrderListResponse(
                        order.getOrderNumber(),
                        order.getOrderId(),
                        order.getTotalAmount(),
                        order.getUsedPoints(),
                        order.getTotalAmount() - order.getPointDiscountAmount(),
                        0L,
                        "KRW",
                        order.getStatus().name(),
                        order.getOrderedAt()
                ))
                .toList();
    }

    // 주문 상세 조회
    @Transactional(readOnly = true)
    public GetOrderDetailResponse getMyOrderDetail(Long userId, String orderId) {

        // 외부용 주문 ID로 주문 조회
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new OrderException(ErrorCode.ORDER_NOT_FOUND));

        // 본인 주문인지 검증
        if (!order.getUser().getId().equals(userId)) {
            throw new OrderException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        // 해당 주문에 포함된 주문 상품 목록 조회
        List<GetOrderItemResponse> items = orderItemRepository.findByOrder(order)
                .stream()
                .map(orderItem -> new GetOrderItemResponse(
                        orderItem.getProduct().getProductId(),
                        orderItem.getProductName(),
                        orderItem.getProductPrice(),
                        orderItem.getQuantity()
                ))
                .toList();

        // 주문 기본 정보 + 주문 상품 목록 반환
        return new GetOrderDetailResponse(
                order.getOrderId(),
                order.getOrderNumber(),
                order.getTotalAmount(),
                order.getUsedPoints(),
                order.getPointDiscountAmount(),
                order.getStatus().name(),
                order.getOrderedAt(),
                items
        );
    }
}
