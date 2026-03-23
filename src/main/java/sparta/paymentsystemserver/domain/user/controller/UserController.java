package sparta.paymentsystemserver.domain.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import sparta.paymentsystemserver.domain.auth.dto.LoginUserData;
import sparta.paymentsystemserver.domain.membership.dto.MyMembershipResponse;
import sparta.paymentsystemserver.domain.point.dto.PointHistoryResponse;
import sparta.paymentsystemserver.domain.point.service.PointService;
import sparta.paymentsystemserver.domain.user.dto.UserResponse;
import sparta.paymentsystemserver.domain.user.dto.UserUpdateRequest;
import sparta.paymentsystemserver.domain.user.entity.User;
import sparta.paymentsystemserver.domain.user.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PointService pointService;

    // 내 정보 조회: 로그인한 사용자의 정보를 반환
    @GetMapping("/me/info")
    public ResponseEntity<UserResponse> getMyInfo(
            @AuthenticationPrincipal LoginUserData loginUserData) {
        // 토큰에서 꺼낸 userId로 사용자 조회
        UserResponse userResponse = userService.getUser(loginUserData.userId());
        return ResponseEntity.ok(userResponse);
    }

    @GetMapping("/me/grades")
    public ResponseEntity<MyMembershipResponse> getMyGrade(
            @AuthenticationPrincipal LoginUserData loginUserData) {
        // 토큰에서 꺼낸 userId로 사용자 조회
        User user = userService.findById(loginUserData.userId());
        return ResponseEntity.ok(MyMembershipResponse.of(user));
    }

    @GetMapping("/me/points")
    public ResponseEntity<List<PointHistoryResponse>> getPointHistory(
            @AuthenticationPrincipal LoginUserData loginUserData) {

        return ResponseEntity.status(HttpStatus.OK).body(pointService.getPointHistory(loginUserData.userId()));
    }

    // 내 정보 수정
    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateMyInfo(
            @AuthenticationPrincipal LoginUserData loginUserData,
            @RequestBody @Valid UserUpdateRequest requestDto) {

        UserResponse response = userService.update(loginUserData.userId(), requestDto);
        return ResponseEntity.ok(response);
    }
}