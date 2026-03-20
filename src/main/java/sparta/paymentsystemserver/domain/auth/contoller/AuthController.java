package sparta.paymentsystemserver.domain.auth.contoller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sparta.paymentsystemserver.domain.auth.dto.*;
import sparta.paymentsystemserver.domain.auth.service.AuthService;
import sparta.paymentsystemserver.domain.user.dto.UserRequest;

import java.time.Duration;

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

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", loginUser.refreshToken())
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofDays(7))
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        return ResponseEntity.status(HttpStatus.OK).header("Authorization" ,"Bearer " + loginUser.token()).body(loginUserResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(
            @RequestHeader("Refresh-Token") String refreshToken, HttpServletResponse response) {
        TokenResponse tokenResponse = authService.reissue(refreshToken);

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", tokenResponse.newRefreshToken())
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofDays(7))
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        return ResponseEntity.status(HttpStatus.OK).header("Authorization" ,"Bearer " + tokenResponse.newAccessToken()).build();
    }
}
