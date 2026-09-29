package com.acme.dbsecurity;

import java.util.UUID;
import java.util.function.Function;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * RLS baglami (referans Bolum 10.1): hesap kimligi TX icinde SET LOCAL ile verilir ve TX bitince silinir.
 * set_config(name, value, is_local=true) SET LOCAL'in parametre baglanabilir esdegeridir (SET LOCAL literal ister).
 *
 * NEDEN duz SET degil: SET oturuma yazar; PgBouncer transaction mode'da oturum bir sonraki istegin server
 * baglantisi olabilir ve baska hesabin baglami sizar (DbRoleSecurityIT bunu gosterir). Uygulama katmanindaki
 * ownership kontrolu (Bolum 9.7) kalkmaz; RLS onu tamamlar.
 */
public final class AccountScope {

    static final String SETTING = "app.account_id";

    private final TransactionTemplate tx;
    private final JdbcTemplate jdbc;

    public AccountScope(DataSource dataSource) {
        this.tx = new TransactionTemplate(new JdbcTransactionManager(dataSource));
        this.jdbc = new JdbcTemplate(dataSource);
    }

    /** Isi, verilen hesabin RLS baglamiyla tek bir TX icinde kosar. */
    public <T> T inAccount(UUID accountId, Function<JdbcTemplate, T> work) {
        return tx.execute(status -> {
            jdbc.queryForObject("SELECT set_config(?, ?, true)", String.class, SETTING, accountId.toString());
            return work.apply(jdbc);
        });
    }
}
