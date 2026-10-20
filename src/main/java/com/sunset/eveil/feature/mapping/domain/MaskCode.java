package com.sunset.eveil.feature.mapping.domain;

import com.sunset.eveil.global.mask.MaskingType;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 마스킹 코드. 유형코드(대문자) + 번호 (예: NAME1). 번호는 대화별로 1부터 서버가 매긴다.
 */
public record MaskCode(MaskingType type, int number) {

	private static final Pattern FORMAT = Pattern.compile("^([A-Z_]+)([1-9]\\d*)$");

	public MaskCode {
		if (type == null) {
			throw new IllegalArgumentException("type must not be null");
		}
		if (number < 1) {
			throw new IllegalArgumentException("number must be >= 1");
		}
	}

	/** "NAME1" 같은 문자열을 파싱한다. 형식·유형이 맞지 않으면 IllegalArgumentException. */
	public static MaskCode parse(String text) {
		if (text == null) {
			throw new IllegalArgumentException("maskCode must not be null");
		}
		Matcher matcher = FORMAT.matcher(text);
		if (!matcher.matches()) {
			throw new IllegalArgumentException("invalid maskCode format");
		}
		return new MaskCode(MaskingType.valueOf(matcher.group(1)), Integer.parseInt(matcher.group(2)));
	}

	public String text() {
		return type.name() + number;
	}
}
