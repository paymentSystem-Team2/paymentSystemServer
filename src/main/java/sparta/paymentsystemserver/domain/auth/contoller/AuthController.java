package sparta.paymentsystemserver.domain.auth.contoller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sparta.paymentsystemserver.domain.auth.dto.LoginUser;
import sparta.paymentsystemserver.domain.auth.dto.LoginUserResponse;
import sparta.paymentsystemserver.domain.auth.dto.TokenResponse;
import sparta.paymentsystemserver.domain.auth.service.AuthService;
import sparta.paymentsystemserver.domain.auth.dto.LoginRequest;
import sparta.paymentsystemserver.domain.user.dto.UserRequestDto;
import sparta.paymentsystemserver.domain.user.dto.UserResponseDto;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<UserResponseDto> signUp(@Valid @RequestBody UserRequestDto requestDto) {
        UserResponseDto responseDto = authService.signUp(requestDto);
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
    public ResponseEntity<TokenResponse> refresh(
            @RequestHeader("Refresh-Token") String refreshToken) {
        return ResponseEntity.ok(authService.reissue(refreshToken));
    }
}
