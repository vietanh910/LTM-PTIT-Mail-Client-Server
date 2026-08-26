package com.ptit.ltm.mail_application.service.impl;

import com.ptit.ltm.mail_application.entity.RuleType;
import com.ptit.ltm.mail_application.entity.SpamRule;
import com.ptit.ltm.mail_application.model.Email;
import com.ptit.ltm.mail_application.repository.SpamRuleRepository;
import com.ptit.ltm.mail_application.service.SpamFilterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpamFilterServiceImpl implements SpamFilterService {

    private final SpamRuleRepository spamRuleRepository;

    @Override
    public boolean isSpam(Email email) {
        if (email == null) return false;

        List<SpamRule> activeRules = spamRuleRepository.findByActiveTrue();
        String from = email.getFromAddress() != null ? email.getFromAddress().toLowerCase() : "";
        String subject = email.getSubject() != null ? email.getSubject().toLowerCase() : "";
        String content = email.getContent() != null ? email.getContent().toLowerCase() : "";

        for (SpamRule rule : activeRules) {
            String pattern = rule.getPattern().toLowerCase();
            if (rule.getRuleType() == RuleType.DOMAIN_BLOCK) {
                if (from.endsWith("@" + pattern) || from.contains("@" + pattern)) {
                    log.info("Spam detected by DOMAIN_BLOCK [{}]: email={}", pattern, from);
                    return true;
                }
            } else if (rule.getRuleType() == RuleType.SENDER_BLOCK) {
                if (from.equals(pattern) || from.contains(pattern)) {
                    log.info("Spam detected by SENDER_BLOCK [{}]: email={}", pattern, from);
                    return true;
                }
            } else if (rule.getRuleType() == RuleType.KEYWORD_BLOCK) {
                if (subject.contains(pattern) || content.contains(pattern)) {
                    log.info("Spam detected by KEYWORD_BLOCK [{}]: subject={}", pattern, subject);
                    return true;
                }
            }
        }
        return false;
    }
}
