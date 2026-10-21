package com.sunset.eveil.feature.mapping.domain;

import com.sunset.eveil.global.mask.MaskingType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 한 대화(chatId)의 매핑 전체. 번호 매기기와 기존 값 재사용 규칙을 가진다 (CLAUDE.md 6번).
 * counters는 유형별 마지막 번호. 다음 번호 = counters + 1.
 */
public class MappingTable {

	private final List<Mapping> mappings;
	private final Map<MaskingType, Integer> counters;

	private MappingTable(List<Mapping> mappings, Map<MaskingType, Integer> counters) {
		this.mappings = new ArrayList<>(mappings);
		this.counters = new EnumMap<>(MaskingType.class);
		this.counters.putAll(counters);
	}

	public static MappingTable empty() {
		return new MappingTable(List.of(), Map.of());
	}

	/** 저장소에서 복호화한 값을 되살린다. */
	public static MappingTable restore(List<Mapping> mappings, Map<MaskingType, Integer> counters) {
		return new MappingTable(mappings, counters);
	}

	/**
	 * 값에 코드를 배정한다. 이미 매핑된 값이면 기존 코드를 그대로 돌려주고, 새 값이면 유형의 다음 번호를 매긴다.
	 */
	public MaskCode assign(String rawValue, MaskingType type) {
		if (type == null) {
			throw new IllegalArgumentException("type must not be null");
		}
		String value = requireText(rawValue);
		Optional<Mapping> existing = findByValue(value);
		if (existing.isPresent()) {
			return existing.get().maskCode();
		}
		int next = counters.merge(type, 1, Integer::sum);
		MaskCode code = new MaskCode(type, next);
		mappings.add(new Mapping(code, value));
		return code;
	}

	/** 같은 유형은 기존 코드를 유지하고, 유형이 바뀌면 새 유형의 다음 번호를 배정한다. */
	public MaskCode changeType(String rawValue, MaskingType type) {
		if (type == null) {
			throw new IllegalArgumentException("type must not be null");
		}
		String value = requireText(rawValue);
		findByValue(value).filter(mapping -> mapping.maskCode().type() != type)
				.ifPresent(mapping -> remove(mapping.maskCode()));
		return assign(value, type);
	}

	public Optional<Mapping> findByValue(String value) {
		return mappings.stream().filter(mapping -> mapping.value().equals(value)).findFirst();
	}

	public Optional<Mapping> findByMaskCode(MaskCode maskCode) {
		return mappings.stream().filter(mapping -> mapping.maskCode().equals(maskCode)).findFirst();
	}

	/** 번호 카운터는 유지해 삭제된 코드를 재사용하지 않는다. */
	public boolean remove(MaskCode maskCode) {
		return mappings.removeIf(mapping -> mapping.maskCode().equals(maskCode));
	}

	public List<Mapping> mappings() {
		return Collections.unmodifiableList(mappings);
	}

	public Map<MaskingType, Integer> counters() {
		return Collections.unmodifiableMap(counters);
	}

	public boolean isEmpty() {
		return mappings.isEmpty();
	}

	private static String requireText(String rawValue) {
		if (rawValue == null || rawValue.isBlank()) {
			throw new IllegalArgumentException("value must not be blank");
		}
		return rawValue.trim();
	}
}
