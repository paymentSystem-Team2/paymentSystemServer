package sparta.paymentsystemserver.domain.membership.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sparta.paymentsystemserver.domain.membership.dto.MembershipResponse;
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
}

