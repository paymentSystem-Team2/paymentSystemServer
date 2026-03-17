package sparta.paymentsystemserver.domain.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import sparta.paymentsystemserver.domain.user.dto.LoginRequestDto;
import sparta.paymentsystemserver.domain.user.dto.UserRequestDto;
import sparta.paymentsystemserver.domain.user.dto.UserResponseDto;
import sparta.paymentsystemserver.domain.user.service.UserService;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 회원가입
    @PostMapping("/signup")
    public ResponseEntity<UserResponseDto> createUser(@Valid @RequestBody UserRequestDto requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.save(requestDto));
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<Void> login(@Valid @RequestBody LoginRequestDto requestDto) {
        userService.login(requestDto);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}