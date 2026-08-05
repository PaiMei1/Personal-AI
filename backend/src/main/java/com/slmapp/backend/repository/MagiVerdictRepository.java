package com.slmapp.backend.repository;

import com.slmapp.backend.entity.MagiVerdict;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MagiVerdictRepository extends JpaRepository<MagiVerdict, UUID> {
    List<MagiVerdict> findByMessageIdOrderByRoundAsc(UUID messageId);
}
