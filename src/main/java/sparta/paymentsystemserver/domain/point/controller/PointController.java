package sparta.paymentsystemserver.domain.point.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sparta.paymentsystemserver.domain.auth.dto.LoginUserData;
import sparta.paymentsystemserver.domain.point.dto.response.PointHistoryResponse;
import sparta.paymentsystemserver.domain.point.service.PointService;

import java.util.List;

@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;

    @GetMapping("/me/history")
    public ResponseEntity<List<PointHistoryResponse>> getPointHistory(
            @AuthenticationPrincipal LoginUserData loginUserData) {

        return ResponseEntity.status(HttpStatus.OK).body(pointService.getPointHistory(loginUserData.userId()));
    }
}
