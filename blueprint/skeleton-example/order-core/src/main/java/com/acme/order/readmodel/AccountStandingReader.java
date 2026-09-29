package com.acme.order.readmodel;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Hesabin aktif olup olmadigini okur. */
@Component
public class AccountStandingReader {

    private final JdbcTemplate jdbc;

    public AccountStandingReader(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public boolean isActive(UUID accountId) {
        Boolean active = jdbc.queryForObject(
                "SELECT active FROM \"subscription\".account_status WHERE account_id = ?", Boolean.class, accountId);
        return Boolean.TRUE.equals(active);
    }
}
