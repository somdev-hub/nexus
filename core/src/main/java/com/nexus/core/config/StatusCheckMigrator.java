package com.nexus.core.config;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.nexus.core.model.enums.PurchaseOrderStatus;
import com.nexus.core.model.enums.ShipmentStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Repairs manually-created status CHECK constraints so they accept every
 * current enum value (notably PO {@code AWAITING_PICKUP}/{@code PICKED_UP}
 * and shipment {@code PICKED_UP}).
 *
 * <p>
 * {@code spring.jpa.hibernate.ddl-auto=update} never alters an existing CHECK
 * constraint, so without this repair writes of newer statuses fail with
 * {@code violates check constraint "..._status_check"}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StatusCheckMigrator implements ApplicationRunner {

    private record StatusCheck(String schema, String table, String constraint, List<String> values) {
    }

    private static final List<StatusCheck> CHECKS = List.of(
            new StatusCheck("core", "t_purchase_orders", "t_purchase_orders_status_check",
                    Arrays.stream(PurchaseOrderStatus.values()).map(Enum::name).toList()),
            new StatusCheck("core", "t_shipments", "t_shipments_status_check",
                    Arrays.stream(ShipmentStatus.values()).map(Enum::name).toList()));

    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) {
        for (StatusCheck check : CHECKS) {
            repair(check);
        }
    }

    private void repair(StatusCheck check) {
        String existsSql = "SELECT 1 FROM information_schema.table_constraints"
                + " WHERE constraint_schema = '" + check.schema() + "'"
                + " AND table_name = '" + check.table() + "'"
                + " AND constraint_name = '" + check.constraint() + "'";
        String values = check.values().stream().collect(Collectors.joining("','", "'", "'"));
        // Both tables carry their state in a plain `status` column, as seen
        // in the failing UPDATE statements.
        String column = "status";
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            if (!statement.executeQuery(existsSql).next()) {
                log.info("CHECK constraint {} absent — nothing to repair", check.constraint());
                return;
            }
            statement.execute(
                    "ALTER TABLE " + check.schema() + "." + check.table() + " DROP CONSTRAINT "
                            + check.constraint());
            statement.execute(
                    "ALTER TABLE " + check.schema() + "." + check.table() + " ADD CONSTRAINT "
                            + check.constraint() + " CHECK (" + column + " IN (" + values + "))");
            log.info("Repaired CHECK constraint {} to accept {}", check.constraint(), values);
        } catch (Exception e) {
            // Never brick application boot over DDL rights; the failure will
            // surface on write, but this message points straight at the cause.
            log.error(
                    "Could not repair CHECK constraint {}. Writes of newer statuses will fail until it is widened manually: {}",
                    check.constraint(), e.getMessage());
        }
    }
}
