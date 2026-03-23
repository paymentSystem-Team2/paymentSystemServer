package sparta.paymentsystemserver.domain.order.service;

import sparta.paymentsystemserver.domain.order.dto.CreateOrderRequest;
import sparta.paymentsystemserver.domain.order.dto.CreateOrderResponse;
import sparta.paymentsystemserver.domain.order.dto.GetOrderDetailResponse;
import sparta.paymentsystemserver.domain.order.dto.GetOrderListResponse;

import java.util.List;

public interface OrderService {
    CreateOrderResponse createOrder(Long userId, CreateOrderRequest request);

    List<GetOrderListResponse> getMyOrders(Long userId);

    GetOrderDetailResponse getMyOrderDetail(Long userId, String orderId);
}
