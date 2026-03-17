package sparta.paymentsystemserver.domain.point.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "point_transactions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Point {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String point_transaction_id;

    private Long user_id;
    private Long refund_id;
    private Long payment_id;

    @Enumerated(EnumType.STRING)
    private PointTransactionType type;

    private Long points;

    private LocalDateTime expires_at;
    private LocalDateTime expiration_processed_at;
}