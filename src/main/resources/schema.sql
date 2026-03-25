-- 멀티 인스턴스 환경에서 스케줄러 중복 실행을 방지하기 위한 ShedLock 전용 테이블
-- 스케줄 작업별 락 상태를 DB에 저장해 한 번만 실행되도록 제어함
CREATE TABLE IF NOT EXISTS shedlock (
    name VARCHAR(64) NOT NULL PRIMARY KEY,
    lock_until TIMESTAMP(3) NOT NULL,
    locked_at TIMESTAMP(3) NOT NULL,
    locked_by VARCHAR(255) NOT NULL
);