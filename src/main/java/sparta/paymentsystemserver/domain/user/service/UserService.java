package sparta.paymentsystemserver.domain.user.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sparta.paymentsystemserver.domain.user.dto.LoginRequestDto;
import sparta.paymentsystemserver.domain.user.dto.UserRequestDto;
import sparta.paymentsystemserver.domain.user.dto.UserResponseDto;
import sparta.paymentsystemserver.domain.user.entity.User;
import sparta.paymentsystemserver.domain.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    // 회원가입
    @Transactional
    public UserResponseDto save(@Valid UserRequestDto requestDto) {
        if (userRepository.existsByEmail(requestDto.getEmail())) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }

        User user = new User(
                requestDto.getUsername(),
                requestDto.getEmail(),
                requestDto.getPassword(),
                requestDto.getPhoneNumber()
        );

        User savedUser = userRepository.save(user);
        return new UserResponseDto(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getPhoneNumber()
        );
    }

    // 로그인
    @Transactional
    public void login(@Valid LoginRequestDto requestDto) {
        // 이메일 확인
        User user = userRepository.findByEmail(requestDto.getEmail()).orElseThrow(
                () -> new IllegalArgumentException("이메일 또는 비밀번호가 틀렸습니다.")
        );
        if (!user.getPassword().equals(requestDto.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 틀렸습니다.");
        }
    }
}