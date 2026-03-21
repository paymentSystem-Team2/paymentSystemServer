package sparta.paymentsystemserver.global.client.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 포트원 결제 취소 요청 dto
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PortOneCancelRequest {
    private String storeId;
    private String reason;

    // 환불 사유 비어 있으면 사용자 요청 환불로 채워서 생성
    public static PortOneCancelRequest of(String storeId, String reason) {
        String cancelReason = (reason == null || reason.isBlank()) ? "사용자 요청 환불" : reason;
        return new PortOneCancelRequest(storeId, cancelReason);
    }
}
