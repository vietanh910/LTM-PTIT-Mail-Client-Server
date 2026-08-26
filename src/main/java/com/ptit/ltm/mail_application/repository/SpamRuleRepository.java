package com.ptit.ltm.mail_application.repository;

import com.ptit.ltm.mail_application.entity.SpamRule;
import com.ptit.ltm.mail_application.entity.RuleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpamRuleRepository extends JpaRepository<SpamRule, Long> {
    List<SpamRule> findByActiveTrue();
    List<SpamRule> findByRuleTypeAndActiveTrue(RuleType ruleType);
    List<SpamRule> findByUserIdAndActiveTrue(Long userId);
}
