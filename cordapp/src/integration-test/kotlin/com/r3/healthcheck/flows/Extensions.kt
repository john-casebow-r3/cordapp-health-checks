package com.r3.healthcheck

import com.r3.healthcheck.model.Health
import com.r3.healthcheck.services.HealthCheckService

fun HealthCheckService.overrideHealthWith(health: Health) {
    currentHealth = health
}