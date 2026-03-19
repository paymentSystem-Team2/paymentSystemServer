package sparta.paymentsystemserver.domain.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.InternalException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sparta.paymentsystemserver.domain.auth.dto.SignupResponse;
import sparta.paymentsystemserver.domain.user.dto.UserRequest;
import sparta.paymentsystemserver.domain.user.dto.UserResponse;
import sparta.paymentsystemserver.domain.user.dto.UserUpdateRequest;
import sparta.paymentsystemserver.domain.user.entity.User;
import sparta.paymentsystemserver.domain.user.exception.DuplicateEmailException;
import sparta.paymentsystemserver.domain.user.exception.UserNotFoundException;
import sparta.paymentsystemserver.domain.user.repository.UserRepository;
import sparta.paymentsystemserver.global.exception.ErrorCode;
import sparta.paymentsystemserver.global.util.PublicIdGenerator;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PublicIdGenerator publicIdGenerator;

    // 회원가입: 이메일 중복 확인 후 사용자 저장
    @Transactional
    public SignupResponse save(UserRequest requestDto) {

        // 이메일 중복 검사: 이미 존재하는 이메일 예외 발생
        if (userRepository.existsByEmail(requestDto.getEmail())) {
            throw new DuplicateEmailException(ErrorCode.DUPLICATE_EMAIL);
        }

        // 서버에서 customerUid 직접 생성
        // PublicIdGenerator로 고유값 보장, 프론트 위조 방지
        String customerUid = publicIdGenerator.generate("USR");

        // 비밀번호 암호화 후 사용자 생성
        User user = new User(
                requestDto.getName(),
                requestDto.getEmail(),
                passwordEncoder.encode(requestDto.getPassword()),
                requestDto.getPhone(),
                customerUid
        );

        try {
            userRepository.save(user);
            return new SignupResponse(
                    true, "성공적으로 회원 가입하였습니다.");
        } catch (Exception e){
            log.error("[AUTH : 회원 가입 로직 중 에러 발생] " +  e.getMessage());
            throw new InternalException("알수 없는 오류");
        }
    }

    // 이메일로 사용자 조회 -> 없으면 UserNotFoundException 발생
    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(
                () -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND)
        );
    }

    // ID 로 사용자 조회 -> 없으면 UserNotFoundException 발생
    @Transactional(readOnly = true)
    public User findById(Long userId) {
        return userRepository.findById(userId).orElseThrow(
                () -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND)
        );
    }

    @Transactional
    public UserResponse update(Long userId, UserUpdateRequest requestDto) {
        // userId로 사용자 조회, 없으면 UserNotFoundException 발생
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND));

        // null이 아닌 필드만 선택적으로 업데이트 (PATCH 방식)
        user.update(requestDto.getName(), requestDto.getPhone());

        // 수정된 사용자 정보를 DTO로 변환하여 반환
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone()
        );
    }
}