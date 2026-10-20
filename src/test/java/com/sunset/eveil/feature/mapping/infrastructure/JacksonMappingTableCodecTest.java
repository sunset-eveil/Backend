package com.sunset.eveil.feature.mapping.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunset.eveil.feature.mapping.domain.MappingTable;
import com.sunset.eveil.global.mask.MaskingType;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class JacksonMappingTableCodecTest {

	private final JacksonMappingTableCodec codec = new JacksonMappingTableCodec();

	@Test
	void CLAUDE_md_4번_평문_JSON_형식으로_직렬화한다() {
		MappingTable table = MappingTable.empty();
		table.assign("홍길동", MaskingType.NAME);
		table.assign("010-1111-1111", MaskingType.PHONE);

		String json = new String(codec.encode(table), StandardCharsets.UTF_8);

		assertThat(json).isEqualTo(
				"{\"mappings\":[{\"maskCode\":\"NAME1\",\"type\":\"NAME\",\"value\":\"홍길동\"},"
						+ "{\"maskCode\":\"PHONE1\",\"type\":\"PHONE\",\"value\":\"010-1111-1111\"}],"
						+ "\"counters\":{\"NAME\":1,\"PHONE\":1}}");
	}

	@Test
	void 직렬화_후_복원하면_매핑과_counters가_유지된다() {
		MappingTable table = MappingTable.empty();
		table.assign("홍길동", MaskingType.NAME);
		table.assign("홍길다", MaskingType.NAME);

		MappingTable restored = codec.decode(codec.encode(table));

		assertThat(restored.mappings()).isEqualTo(table.mappings());
		assertThat(restored.counters()).isEqualTo(table.counters());
		assertThat(restored.assign("세번째", MaskingType.NAME).text()).isEqualTo("NAME3");
	}

	@Test
	void 빈_테이블도_왕복된다() {
		MappingTable restored = codec.decode(codec.encode(MappingTable.empty()));

		assertThat(restored.isEmpty()).isTrue();
		assertThat(restored.counters()).isEmpty();
	}
}
