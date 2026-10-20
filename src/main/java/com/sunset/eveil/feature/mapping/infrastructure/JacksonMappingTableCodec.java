package com.sunset.eveil.feature.mapping.infrastructure;

import com.sunset.eveil.feature.mapping.domain.MaskCode;
import com.sunset.eveil.feature.mapping.domain.Mapping;
import com.sunset.eveil.feature.mapping.domain.MappingTable;
import com.sunset.eveil.global.mask.MaskingType;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * MappingTable ↔ 매핑 평문 JSON (CLAUDE.md 4번 "매핑 평문").
 * {"mappings":[{"maskCode":"NAME1","type":"NAME","value":"홍길동"}],"counters":{"NAME":1}}
 * 저장 포맷 전용이라 웹 JSON 설정과 분리된 자체 JsonMapper를 쓴다.
 */
@Component
public class JacksonMappingTableCodec {

	private final JsonMapper jsonMapper = JsonMapper.builder().build();

	public byte[] encode(MappingTable table) {
		List<MappingJson> mappings = table.mappings().stream()
				.map(mapping -> new MappingJson(mapping.maskCode().text(), mapping.type().name(), mapping.value()))
				.toList();
		Map<String, Integer> counters = new java.util.LinkedHashMap<>();
		table.counters().forEach((type, number) -> counters.put(type.name(), number));
		return jsonMapper.writeValueAsString(new MappingTableJson(mappings, counters)).getBytes(StandardCharsets.UTF_8);
	}

	public MappingTable decode(byte[] json) {
		MappingTableJson parsed;
		try {
			parsed = jsonMapper.readValue(json, MappingTableJson.class);
		} catch (JacksonException e) {
			throw new IllegalStateException("stored mapping JSON is malformed");
		}
		List<Mapping> mappings = parsed.mappings().stream()
				.map(item -> new Mapping(MaskCode.parse(item.maskCode()), item.value()))
				.toList();
		Map<MaskingType, Integer> counters = new EnumMap<>(MaskingType.class);
		parsed.counters().forEach((name, number) -> counters.put(MaskingType.valueOf(name), number));
		return MappingTable.restore(mappings, counters);
	}

	record MappingTableJson(List<MappingJson> mappings, Map<String, Integer> counters) {
		MappingTableJson {
			mappings = mappings == null ? List.of() : mappings;
			counters = counters == null ? Map.of() : counters;
		}
	}

	record MappingJson(String maskCode, String type, String value) {
	}
}
