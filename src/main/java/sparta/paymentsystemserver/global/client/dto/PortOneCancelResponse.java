package sparta.paymentsystemserver.global.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 포트원 결제 취소 응답 dto
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PortOneCancelResponse {

    @JsonProperty("id")
    private String paymentId;

    private String status;
    private String cancelId;
    private Long canceledAmount;
}
