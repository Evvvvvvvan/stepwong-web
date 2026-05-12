package com.example.stepwong.repository;

import com.example.stepwong.entity.ExecutionLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExecutionLogRepository extends JpaRepository<ExecutionLog, Long> {

    List<ExecutionLog> findTop100ByOwnerIdOrderByStartedAtDesc(Long ownerId);
}
