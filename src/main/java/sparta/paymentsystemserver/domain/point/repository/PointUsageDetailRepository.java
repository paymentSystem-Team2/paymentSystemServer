package sparta.paymentsystemserver.domain.point.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sparta.paymentsystemserver.domain.point.entity.PointTransaction;
import sparta.paymentsystemserver.domain.point.entity.PointUsageDetail;

public interface PointUsageDetailRepository extends JpaRepository<PointUsageDetail, Long> {

    @Query("""
            SELECT COALESCE(SUM(p.usedPoints), 0)
            FROM PointUsageDetail p
            WHERE p.earnedTransaction = :earned
            """)
    long sumUsedPointsByEarned(@Param("earned") PointTransaction earned);
}
