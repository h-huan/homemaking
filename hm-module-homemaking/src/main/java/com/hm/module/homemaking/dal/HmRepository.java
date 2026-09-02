package com.hm.module.homemaking.dal;

import com.hm.framework.tenant.core.context.TenantContextHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.sql.Statement;
import java.util.*;

/** Explicit tenant predicates also protect JDBC queries, which do not use the MyBatis interceptor. */
@Repository
public class HmRepository {
    private final JdbcTemplate jdbc;
    public JdbcTemplate jdbc(){return jdbc;}
    private static final Set<String> TABLES = Set.of("hm_store", "hm_worker", "hm_service", "hm_order", "hm_booking",
            "hm_customer_address", "hm_review", "hm_aftersale", "hm_settlement");
    public HmRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public long tenant() {
        if (TenantContextHolder.isIgnore()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "需要明确的租户范围");
        long id = TenantContextHolder.getRequiredTenantId();
        if (id <= 0) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无效租户");
        return id;
    }
    public Map<String,Object> require(String table, long id, boolean lock) {
        if (!TABLES.contains(table)) throw new IllegalArgumentException("Unknown business table");
        var rows = jdbc.queryForList("SELECT * FROM " + table + " WHERE tenant_id=? AND id=?" + (lock ? " FOR UPDATE" : ""), tenant(), id);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "记录不存在");
        return rows.get(0);
    }
    public long insert(String sql, Object... args) {
        var key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement(sql, new String[]{"id"});
            for (int i=0; i<args.length; i++) statement.setObject(i+1,args[i]);
            return statement;
        }, key);
        return Objects.requireNonNull(key.getKey()).longValue();
    }
    public static long number(Map<String,Object> row, String key) { return ((Number)row.get(key)).longValue(); }
    public static int cents(Map<String,Object> row, String key) { return Math.toIntExact(number(row,key)); }
    public static void check(boolean ok, String message) { if (!ok) throw new ResponseStatusException(HttpStatus.CONFLICT,message); }
}
