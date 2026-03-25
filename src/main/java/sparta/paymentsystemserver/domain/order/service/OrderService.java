package sparta.paymentsystemserver.domain.order.service;

import org.jspecify.annotations.Nullable;
import sparta.paymentsystemserver.domain.order.dto.*;

import java.util.List;

public interface OrderService {
    CreateOrderResponse createOrder(Long userId, CreateOrderRequest request);

    List<GetOrderListResponse> getMyOrders(Long userId);

    GetOrderDetailResponse getMyOrderDetail(Long userId, String orderId);

    updateOrderStatusResponse processDelivery(String orderId);

    void confirmOrder(String orderId);

    List<GetOrderListResponse> getAllOrders();
}
