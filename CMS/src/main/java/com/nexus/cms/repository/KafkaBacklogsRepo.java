package com.nexus.cms.repository;

import com.nexus.cms.model.entities.KafkaBacklogs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface KafkaBacklogsRepo extends JpaRepository<KafkaBacklogs, Long> {

    @Query("SELECT k FROM KafkaBacklogs k WHERE k.uuid = :uuid")
    @Transactional(readOnly = true)
    Optional<KafkaBacklogs> findByUuid(String uuid);

    @Query(value = """
            SELECT 
                TO_CHAR(message_received_at, 'Mon') AS month,
                TO_CHAR(message_received_at, 'YYYY-MM') AS sort_key,
                COUNT(*) AS count
            FROM cms.t_kafka_backlogs
            WHERE template_param = :templateParam
              AND message_received_at >= NOW() - INTERVAL '6 months'
            GROUP BY TO_CHAR(message_received_at, 'Mon'), TO_CHAR(message_received_at, 'YYYY-MM')
            ORDER BY sort_key ASC
            """, nativeQuery = true)
    List<Object[]> findMonthlyCountByTemplateParam(
            @Param("templateParam") String templateParam
    );

    @Query(value = """
            SELECT 
                status,
                COUNT(*) AS count
            FROM cms.t_kafka_backlogs
            WHERE template_param = :templateParam
                AND org_id = :orgId
            GROUP BY status
            """, nativeQuery = true)
    List<Object[]> findStatusCountByTemplateParamAndOrgId(
            @Param("templateParam") String templateParam,
            @Param("orgId") Long orgId
    );
}
