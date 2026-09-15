package com.minibank.notificationservice.repository;

import com.minibank.notificationservice.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificaRepository extends JpaRepository<Notification, Long> {
}
