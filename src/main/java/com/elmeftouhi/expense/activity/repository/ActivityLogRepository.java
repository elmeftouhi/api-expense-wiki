package com.elmeftouhi.expense.activity.repository;

import com.elmeftouhi.expense.activity.entity.ActivityLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long>, JpaSpecificationExecutor<ActivityLog> {

	List<ActivityLog> findByTenantIdOrderByOccurredAtAscIdAsc(Long tenantId);
}

