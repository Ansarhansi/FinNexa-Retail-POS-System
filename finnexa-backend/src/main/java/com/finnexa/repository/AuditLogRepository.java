package com.finnexa.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.finnexa.entity.AuditLog;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
	List<AuditLog> findAllByOrderByCreatedAtDesc();

	List<AuditLog> findByUsernameOrderByCreatedAtDesc(String username);

	List<AuditLog> findByActionOrderByCreatedAtDesc(String action);

	List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, Long entityId);
}