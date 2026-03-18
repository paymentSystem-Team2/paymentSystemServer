package sparta.paymentsystemserver.domain.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sparta.paymentsystemserver.domain.user.dto.UserRequest;
import sparta.paymentsystemserver.domain.user.dto.UserResponse;
import sparta.paymentsystemserver.domain.user.entity.User;
import sparta.paymentsystemserver.domain.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse save(UserRequest requestDto) {
        if (userRepository.existsByEmail(requestDto.getEmail())) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }

        User user = new User(
                requestDto.getName(),
                requestDto.getEmail(),
                passwordEncoder.encode(requestDto.getPassword()),
                requestDto.getPhone(),
                requestDto.getCustomerUid()
        );

        User savedUser = userRepository.save(user);
        return new UserResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getPhone(),
                savedUser.getCustomerUid()
        );
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(
                () -> new IllegalArgumentException("존재하지 않은 유저입니다.")
        );
    }
}