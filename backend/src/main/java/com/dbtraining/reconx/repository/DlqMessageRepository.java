package com.dbtraining.reconx.repository;

import com.dbtraining.reconx.repository.entity.DlqMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DlqMessageRepository extends JpaRepository<DlqMessage, Long> {

    Optional<DlqMessage> findByEventId(UUID eventId);

    List<DlqMessage> findAllByOrderByFirstSeenAsc();
}
