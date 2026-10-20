package com.sunset.eveil.feature.masking.infrastructure;

import com.sunset.eveil.global.mask.MaskingType;
import com.sunset.eveil.feature.masking.domain.DetectedEntity;
import com.sunset.eveil.feature.masking.domain.Detector;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * 정규식 + 체크섬 탐지기. 전화번호, 이메일, 주민등록번호, 카드번호.
 * 긴 패턴부터 매칭하고, 이미 매칭된 구간과 겹치는 짧은 매칭은 버린다 (카드번호 안의 전화번호 오탐 방지).
 */
@Component
public class RegexDetector implements Detector {

	private record Rule(MaskingType type, Pattern pattern, Predicate<String> checksum) {
	}

	private record Span(int start, int end) {
		boolean overlaps(Span other) {
			return start < other.end && other.start < end;
		}
	}

	/** 우선순위 = 보통 더 긴 패턴. */
	private static final List<Rule> RULES = List.of(
			new Rule(MaskingType.EMAIL,
					Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}"),
					value -> true),
			new Rule(MaskingType.CARD_ACCOUNT,
					Pattern.compile("(?<![\\d-])\\d{4}[- ]?\\d{4}[- ]?\\d{4}[- ]?\\d{4}(?![\\d-])"),
					RegexDetector::passesLuhn),
			new Rule(MaskingType.RRN,
					Pattern.compile("(?<![\\d-])\\d{2}(?:0[1-9]|1[0-2])(?:0[1-9]|[12]\\d|3[01])-?[1-8]\\d{6}(?![\\d-])"),
					RegexDetector::passesRrnChecksum),
			new Rule(MaskingType.PHONE,
					Pattern.compile("(?<![\\d-])0\\d{1,2}-?\\d{3,4}-?\\d{4}(?![\\d-])"),
					value -> true)
	);

	@Override
	public List<DetectedEntity> detect(String text, Set<MaskingType> excludeTypes) {
		Set<MaskingType> excluded = excludeTypes == null ? Set.of() : excludeTypes;
		List<Span> taken = new ArrayList<>();
		Set<DetectedEntity> found = new LinkedHashSet<>();

		for (Rule rule : RULES) {
			if (excluded.contains(rule.type())) {
				continue;
			}
			Matcher matcher = rule.pattern().matcher(text);
			while (matcher.find()) {
				Span span = new Span(matcher.start(), matcher.end());
				if (taken.stream().anyMatch(span::overlaps)) {
					continue;
				}
				String value = matcher.group().trim();
				if (!rule.checksum().test(value)) {
					continue;
				}
				taken.add(span);
				found.add(new DetectedEntity(value, rule.type()));
			}
		}
		return found.stream()
				.sorted(Comparator.comparingInt((DetectedEntity entity) -> entity.value().length()).reversed())
				.toList();
	}

	private static String digitsOnly(String value) {
		return value.replaceAll("\\D", "");
	}

	/** 카드번호 Luhn 검증. */
	static boolean passesLuhn(String value) {
		String digits = digitsOnly(value);
		int sum = 0;
		boolean doubleIt = false;
		for (int i = digits.length() - 1; i >= 0; i--) {
			int digit = digits.charAt(i) - '0';
			if (doubleIt) {
				digit *= 2;
				if (digit > 9) {
					digit -= 9;
				}
			}
			sum += digit;
			doubleIt = !doubleIt;
		}
		return sum % 10 == 0;
	}

	/** 주민등록번호 검증 번호(13번째 자리) 검증. 2020년 10월 이후 발급분은 이 규칙을 따르지 않을 수 있다. */
	static boolean passesRrnChecksum(String value) {
		String digits = digitsOnly(value);
		if (digits.length() != 13) {
			return false;
		}
		int[] weights = {2, 3, 4, 5, 6, 7, 8, 9, 2, 3, 4, 5};
		int sum = 0;
		for (int i = 0; i < 12; i++) {
			sum += (digits.charAt(i) - '0') * weights[i];
		}
		int expected = (11 - (sum % 11)) % 10;
		return expected == digits.charAt(12) - '0';
	}
}
