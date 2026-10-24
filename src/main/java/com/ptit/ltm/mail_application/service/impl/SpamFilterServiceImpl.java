package com.ptit.ltm.mail_application.service.impl;

import com.ptit.ltm.mail_application.entity.RuleType;
import com.ptit.ltm.mail_application.entity.SpamRule;
import com.ptit.ltm.mail_application.model.Email;
import com.ptit.ltm.mail_application.repository.SpamRuleRepository;
import com.ptit.ltm.mail_application.service.SpamFilterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpamFilterServiceImpl implements SpamFilterService {

    private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    private final SpamRuleRepository spamRuleRepository;

    /**
     * Chuẩn hóa chuỗi tiếng Việt: bỏ dấu, chuyển chữ thường, thay đ/Đ thành d, thu gọn khoảng trắng thừa.
     * Cho kết quả giống hệt nhau giữa văn bản có dấu và không dấu.
     * Ví dụ:
     * - "Trúng Thưởng" -> "trung thuong"
     * - "trung thuong" -> "trung thuong"
     * - "Độc Đắc" -> "doc dac"
     */
    public static String removeVietnameseTones(String str) {
        if (str == null) return "";
        String nfd = Normalizer.normalize(str, Normalizer.Form.NFD);
        String noTones = DIACRITICS_PATTERN.matcher(nfd).replaceAll("");
        return noTones.replace('đ', 'd')
                .replace('Đ', 'd')
                .toLowerCase()
                .replaceAll("\\s+", " ")
                .trim();
    }

    @Override
    public boolean isSpam(Email email) {
        if (email == null) return false;

        List<SpamRule> activeRules = spamRuleRepository.findByActiveTrue();
        if (activeRules == null || activeRules.isEmpty()) {
            return false;
        }

        String from = email.getFromAddress() != null ? email.getFromAddress() : "";
        String normFrom = removeVietnameseTones(from);

        String subject = email.getSubject() != null ? email.getSubject() : "";
        String normSubject = removeVietnameseTones(subject);

        String rawContent = email.getContent() != null ? email.getContent() : "";
        String plainContent = "";
        try {
            plainContent = Jsoup.parse(rawContent).text();
        } catch (Exception e) {
            plainContent = rawContent;
        }
        String normRawContent = removeVietnameseTones(rawContent);
        String normPlainContent = removeVietnameseTones(plainContent);

        for (SpamRule rule : activeRules) {
            if (rule.getPattern() == null || rule.getPattern().trim().isEmpty()) {
                continue;
            }

            String rawPattern = rule.getPattern().trim();
            String normPattern = removeVietnameseTones(rawPattern);

            if (normPattern.isEmpty()) {
                continue;
            }

            if (rule.getRuleType() == RuleType.DOMAIN_BLOCK) {
                String domainPattern = normPattern.startsWith("@") ? normPattern.substring(1) : normPattern;
                if (normFrom.endsWith("@" + domainPattern) || normFrom.contains("@" + domainPattern)) {
                    log.info("Spam detected by DOMAIN_BLOCK [{}]: email={}", rawPattern, from);
                    return true;
                }
            } else if (rule.getRuleType() == RuleType.SENDER_BLOCK) {
                if (normFrom.equals(normPattern) || normFrom.contains(normPattern)) {
                    log.info("Spam detected by SENDER_BLOCK [{}]: email={}", rawPattern, from);
                    return true;
                }
            } else if (rule.getRuleType() == RuleType.KEYWORD_BLOCK) {
                // Kiểm tra từ khóa trong tiêu đề và nội dung (hỗ trợ có dấu và không dấu như nhau)
                if (normSubject.contains(normPattern)
                        || normPlainContent.contains(normPattern)
                        || normRawContent.contains(normPattern)) {
                    log.info("Spam detected by KEYWORD_BLOCK [{}]: matched in subject='{}'", rawPattern, subject);
                    return true;
                }
            }
        }
        return false;
    }
}
