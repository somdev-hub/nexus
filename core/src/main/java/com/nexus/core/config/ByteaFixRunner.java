package com.nexus.core.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ByteaFixRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            log.info("[ByteaFix] Checking t_supplier_catalog column types...");
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
            // Logistics tables (PART-03): Hibernate ddl-auto=update may create
            // String columns as bytea, which breaks LOWER(...) LIKE searches
            // with "function lower(bytea) does not exist". No entity uses
            // byte[], so every bytea column here is a mis-mapped String.
            fixByteaColumns("t_fleet_assets");
            fixByteaColumns("t_drivers");
            fixByteaColumns("t_maintenance_records");
            fixByteaColumns("t_shipment_quotes");
            fixByteaColumns("t_freight_rates");
            fixByteaColumns("t_shipment_incidents");
            fixByteaColumns("t_proof_of_delivery");
            fixByteaColumns("t_consolidation_groups");
            fixByteaColumns("t_capacity_forecasts");
            fixByteaColumns("t_carrier_payables");
        } catch (Exception e) {
            log.warn("[ByteaFix] Failed to check/fix columns: {}", e.getMessage());
        }
    }

    private void fixByteaColumns(String table) {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT column_name FROM information_schema.columns WHERE table_schema='core' AND table_name=? AND data_type='bytea'",
                    table);
            log.info("[ByteaFix] Table {} has {} bytea column(s)", table, rows.size());
            for (Map<String, Object> row : rows) {
                String col = String.valueOf(row.get("column_name"));
                try {
                    jdbcTemplate.execute(String.format(
                            "ALTER TABLE core.%s ALTER COLUMN %s TYPE TEXT USING %s::text", table, col, col));
                    log.info("[ByteaFix] Fixed {}.{} to TEXT", table, col);
                } catch (Exception e) {
                    log.info("[ByteaFix] {}.{} fix not needed: {}", table, col, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("[ByteaFix] Failed to inspect table {}: {}", table, e.getMessage());
        }
    }
}
