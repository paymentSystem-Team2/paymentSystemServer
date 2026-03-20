package sparta.paymentsystemserver.global.client.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 포트원 결제 취소 dto
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PortOneCancelRequest {
    private String storeId;
    private String reason;
}
