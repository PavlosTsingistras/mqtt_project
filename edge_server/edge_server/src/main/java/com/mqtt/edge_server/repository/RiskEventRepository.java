package com.mqtt.edge_server.repository;

import com.mqtt.edge_server.model.RiskEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RiskEventRepository extends JpaRepository<RiskEvent, Long> {
}
