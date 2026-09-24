package com.nexus.core.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ByteaFixRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            log.info("[ByteaFix] Checking t_supplier_catalog column types...");
            var cols = jdbcTemplate.queryForList(
                    "SELECT column_name, data_type FROM information_schema.columns WHERE table_schema='core' AND table_name='t_supplier_catalog' AND column_name IN ('name','code','category','family','sku')", Object.class);
            // Use simple query to fix bytea columns
            String[] fixCols = {"name", "code", "category", "family", "sku"};
            for (String col : fixCols) {
                try {
                    String sql = String.format("ALTER TABLE core.t_supplier_catalog ALTER COLUMN %s TYPE TEXT USING %s::text", col, col);
                    jdbcTemplate.execute(sql);
                    log.info("[ByteaFix] Fixed column {} to TEXT", col);
                } catch (Exception e) {
                    log.info("[ByteaFix] Column {} already TEXT or fix not needed: {}", col, e.getMessage());
                }
            }
            // Also fix other potential bytea text columns
            for (String col : new String[]{"description", "attributes", "specifications", "allowed_partner_org_ids"}) {
                try {
                    String sql = String.format("ALTER TABLE core.t_supplier_catalog ALTER COLUMN %s TYPE TEXT USING %s::text", col, col);
                    jdbcTemplate.execute(sql);
                    log.info("[ByteaFix] Fixed column {} to TEXT", col);
                } catch (Exception e) {
                    // ignore
                }
            }
        } catch (Exception e) {
            log.warn("[ByteaFix] Failed to check/fix columns: {}", e.getMessage());
        }
    }
}
