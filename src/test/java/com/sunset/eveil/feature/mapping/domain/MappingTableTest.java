package com.sunset.eveil.feature.mapping.domain;

import com.sunset.eveil.global.mask.MaskingType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MappingTableTest {

	@Test
	void 새_값은_유형별로_1부터_번호를_받는다() {
		MappingTable table = MappingTable.empty();

		assertThat(table.assign("홍길동", MaskingType.NAME).text()).isEqualTo("NAME1");
		assertThat(table.assign("010-1111-1111", MaskingType.PHONE).text()).isEqualTo("PHONE1");
		assertThat(table.assign("홍길다", MaskingType.NAME).text()).isEqualTo("NAME2");
		assertThat(table.counters()).containsEntry(MaskingType.NAME, 2).containsEntry(MaskingType.PHONE, 1);
	}

	@Test
	void 같은_값은_기존_코드를_그대로_돌려주고_번호를_올리지_않는다() {
		MappingTable table = MappingTable.empty();
		MaskCode first = table.assign("홍길동", MaskingType.NAME);

		MaskCode again = table.assign("홍길동", MaskingType.NAME);

		assertThat(again).isEqualTo(first);
		assertThat(table.mappings()).hasSize(1);
		assertThat(table.counters()).containsEntry(MaskingType.NAME, 1);
	}

	@Test
	void 값은_앞뒤_공백을_제거해_비교한다() {
		MappingTable table = MappingTable.empty();
		MaskCode first = table.assign("홍길동", MaskingType.NAME);

		assertThat(table.assign("  홍길동  ", MaskingType.NAME)).isEqualTo(first);
	}

	@Test
	void 복원된_테이블은_기존_번호_다음부터_이어서_매긴다() {
		MappingTable table = MappingTable.restore(
				List.of(new Mapping(MaskCode.parse("NAME1"), "홍길동")),
				Map.of(MaskingType.NAME, 1));

		assertThat(table.assign("홍길다", MaskingType.NAME).text()).isEqualTo("NAME2");
		assertThat(table.findByMaskCode(MaskCode.parse("NAME1"))).map(Mapping::value).contains("홍길동");
	}

	@Test
	void 빈_값은_거부한다() {
		assertThatThrownBy(() -> MappingTable.empty().assign("   ", MaskingType.NAME))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void MaskCode_parse는_형식과_유형을_검증한다() {
		assertThat(MaskCode.parse("CARD_ACCOUNT12")).isEqualTo(new MaskCode(MaskingType.CARD_ACCOUNT, 12));
		assertThatThrownBy(() -> MaskCode.parse("NAME0")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> MaskCode.parse("name1")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> MaskCode.parse("FOO1")).isInstanceOf(IllegalArgumentException.class);
	}
}
