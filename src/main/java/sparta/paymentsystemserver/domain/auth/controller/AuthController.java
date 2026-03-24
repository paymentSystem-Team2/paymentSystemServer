package sparta.paymentsystemserver.domain.auth.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sparta.paymentsystemserver.domain.auth.dto.*;
import sparta.paymentsystemserver.domain.auth.service.AuthService;
import sparta.paymentsystemserver.domain.user.dto.UserRequest;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signUp(@Valid @RequestBody UserRequest requestDto) {
        SignupResponse responseDto = authService.signUp(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginUserResponse> login(@Valid @RequestBody LoginRequest requestDto, HttpServletResponse response) {
        LoginUser loginUser = authService.login(requestDto);
        LoginUserResponse loginUserResponse = new LoginUserResponse(true, loginUser.email());

        return ResponseEntity.status(HttpStatus.OK)
                .header("Authorization" ,"Bearer " + loginUser.token())
                .header("X-Refresh-Token" ,"Bearer " + loginUser.refreshToken())
                .body(loginUserResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(
            @RequestHeader("Refresh-Token") String refreshToken,
            @RequestHeader("Authorization") String accessToken) {
        TokenResponse tokenResponse = authService.reissue(refreshToken,accessToken);

        return ResponseEntity.status(HttpStatus.OK)
                .header("Authorization" ,"Bearer " + tokenResponse.newAccessToken())
                .header("X-Refresh-Token" ,"Bearer " + tokenResponse.newRefreshToken())
                .build();
    }
}
