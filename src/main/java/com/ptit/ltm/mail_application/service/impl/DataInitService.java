package com.ptit.ltm.mail_application.service.impl;

import com.ptit.ltm.mail_application.entity.RuleType;
import com.ptit.ltm.mail_application.entity.SpamRule;
import com.ptit.ltm.mail_application.entity.User;
import com.ptit.ltm.mail_application.repository.SpamRuleRepository;
import com.ptit.ltm.mail_application.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataInitService implements CommandLineRunner {

    private final UserRepository userRepository;
    private final SpamRuleRepository spamRuleRepository;

    @Override
    public void run(String... args) {
        initDefaultUsers();
        initDefaultSpamRules();
    }

    private void initDefaultUsers() {
        if (userRepository.count() == 0) {
            log.info("Initializing default users in MySQL database...");
            User user1 = User.builder()
                    .username("user1")
                    .password("user1")
                    .fullName("Nguyễn Văn User 1")
                    .email("user1@domain1.com")
                    .phoneNumber("0912345678")
                    .address("Hà Nội")
                    .role("ROLE_USER")
                    .status("ACTIVE")
                    .build();

            User user2 = User.builder()
                    .username("user2")
                    .password("user2")
                    .fullName("Trần Thị User 2")
                    .email("user2@domain1.com")
                    .phoneNumber("0987654321")
                    .address("Hà Nội")
                    .role("ROLE_USER")
                    .status("ACTIVE")
                    .build();

            userRepository.saveAll(List.of(user1, user2));
            log.info("Default users initialized successfully.");
        }
    }

    private void initDefaultSpamRules() {
        if (spamRuleRepository.count() == 0) {
            log.info("Initializing default spam filter rules...");
            SpamRule rule1 = SpamRule.builder()
                    .ruleType(RuleType.KEYWORD_BLOCK)
                    .pattern("trúng thưởng")
                    .description("Chặn thư có từ khóa trúng thưởng")
                    .action("MOVE_TO_SPAM")
                    .active(true)
                    .build();

            SpamRule rule2 = SpamRule.builder()
                    .ruleType(RuleType.KEYWORD_BLOCK)
                    .pattern("khuyến mãi sốc")
                    .description("Chặn thư quảng cáo khuyến mãi")
                    .action("MOVE_TO_SPAM")
                    .active(true)
                    .build();

            SpamRule rule3 = SpamRule.builder()
                    .ruleType(RuleType.DOMAIN_BLOCK)
                    .pattern("spam-domain.com")
                    .description("Chặn domain rác")
                    .action("MOVE_TO_SPAM")
                    .active(true)
                    .build();

            spamRuleRepository.saveAll(List.of(rule1, rule2, rule3));
            log.info("Default spam rules initialized successfully.");
        }
    }
}
