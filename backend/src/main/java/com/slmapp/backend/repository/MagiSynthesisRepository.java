package com.slmapp.backend.repository;

import com.slmapp.backend.entity.MagiSynthesis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MagiSynthesisRepository extends JpaRepository<MagiSynthesis, UUID> {
    Optional<MagiSynthesis> findByMessageId(UUID messageId);
}
