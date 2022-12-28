package com.joshlong.spring.blogs.metadata;

import lombok.RequiredArgsConstructor;
import org.springframework.integration.metadata.MetadataStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

@RequiredArgsConstructor
public class DataSourceMetadataStore implements MetadataStore {

	private final JdbcTemplate ds;

	private final TransactionTemplate tx;

	@Override
	public void put(String key, String value) {
		this.ds.update("""
				insert into spring_integration_metadata_store(key, value) values (?,?)
				on conflict( key) do update set value = excluded.value
				""", ps -> {
			ps.setString(1, key);
			ps.setString(2, value);
			ps.execute();
		});
	}

	@Override
	public String get(String key) {
		var list = this.ds.query("select * from spring_integration_metadata_store where key = ?",
				(rs, rowNum) -> rs.getString("value"), key);
		if (list.size() > 0)
			return list.get(0);
		return null;
	}

	@Override
	public String remove(String key) {
		return tx.execute(status -> {
			var old = get(key);
			if (old != null) {
				ds.update("delete from spring_integration_metadata_store where key = ?", key);
			}
			return old;
		});
	}

}
