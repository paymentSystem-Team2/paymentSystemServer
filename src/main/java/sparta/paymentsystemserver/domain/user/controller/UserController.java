package sparta.paymentsystemserver.domain.user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import sparta.paymentsystemserver.domain.user.service.UserService;

@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
}