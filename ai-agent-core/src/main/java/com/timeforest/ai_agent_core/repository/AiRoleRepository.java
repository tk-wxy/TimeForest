package com.timeforest.ai_agent_core.repository;

import com.timeforest.ai_agent_core.entity.AiRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AiRoleRepository extends JpaRepository<AiRole, Long> {

    /** 返回系统角色（userId=null）+ 指定用户的角色，按 id 升序 */
    @Query("SELECT r FROM AiRole r WHERE r.active = true AND (r.userId IS NULL OR r.userId = :userId) ORDER BY r.id ASC")
    List<AiRole> findAccessibleByUserId(@Param("userId") Long userId);

    List<AiRole> findByActiveTrueOrderByIdAsc();

    Optional<AiRole> findByIdAndUserId(Long id, Long userId);
}
