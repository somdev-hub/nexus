package com.nexus.hr.repository;

import com.nexus.hr.model.entities.TimeManagement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TimeManagementRepo extends JpaRepository<TimeManagement, Long> {

    @Query("SELECT tm FROM TimeManagement tm WHERE tm.hrEntity.hrId = :hrId ORDER BY tm.createdOn DESC")
    TimeManagement findLatestByHrEntity_HrId(Long hrId);

    @Query("SELECT tm FROM TimeManagement tm WHERE tm.day = :day AND tm.month = :month AND tm.year = :year AND tm.hrEntity.hrId = :hrId")
    TimeManagement findByDayMonthYearAndHrEntity(Integer day, Integer month, Integer year, Long hrId);

    @Query("SELECT tm FROM TimeManagement tm WHERE tm.hrEntity.hrId = :hrId " +
            "AND (tm.year < :year OR (tm.year = :year AND tm.month < :month) OR " +
            "(tm.year = :year AND tm.month = :month AND tm.day < :day)) " +
            "ORDER BY tm.year DESC, tm.month DESC, tm.day DESC")
    List<TimeManagement> findAllBeforeDate(@Param("hrId") Long hrId,
                                           @Param("year") Integer year,
                                           @Param("month") Integer month,
                                           @Param("day") Integer day);

    @Query("SELECT tm FROM TimeManagement tm WHERE tm.hrEntity.hrId = :hrId " +
            "ORDER BY tm.year DESC, tm.month DESC, tm.day DESC")
    List<TimeManagement> findAllByHrIdOrderByDateDesc(@Param("hrId") Long hrId);
}
