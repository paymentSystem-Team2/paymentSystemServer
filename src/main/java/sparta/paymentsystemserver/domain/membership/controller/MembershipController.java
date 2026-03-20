package sparta.paymentsystemserver.domain.membership.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sparta.paymentsystemserver.domain.auth.dto.LoginUserData;
import sparta.paymentsystemserver.domain.membership.dto.MembershipResponse;
import sparta.paymentsystemserver.domain.membership.dto.MyMembershipResponse;
import sparta.paymentsystemserver.domain.membership.service.MembershipService;

import java.util.List;

@RestController
@RequestMapping("/api/memberships")
@RequiredArgsConstructor
public class MembershipController {

    private final MembershipService membershipService;

    // 멤버십 정책 목록 조회
    @GetMapping
    public ResponseEntity<List<MembershipResponse>> getMembershipPolicies() {
        return ResponseEntity.ok(membershipService.getMembershipPolicies());
    }

    // 내 멤버십 등급 조회(현재 등급, 누적 결제금액, 적립률 반환)
    @GetMapping("/me")
    public ResponseEntity<MyMembershipResponse> getMyGrade(
            @AuthenticationPrincipal LoginUserData loginUserData
    ) {
        return ResponseEntity.ok(membershipService.getMyGrade(loginUserData.userId()));
    }
}

