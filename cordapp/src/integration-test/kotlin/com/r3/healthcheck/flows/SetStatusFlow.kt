package com.r3.healthcheck.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.healthcheck.model.Health
import com.r3.healthcheck.overrideHealthWith
import com.r3.healthcheck.services.HealthCheckService
import com.r3.healthcheck.services.MyHealthCheckService
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC


@InitiatingFlow
@StartableByRPC
class SetStatusFlow(val health: Health) : FlowLogic<Unit>() {

    @Suspendable
    override fun call() {
        logger.info("${ourIdentity.name} - running flow SetStatusFlow")
        val healthCheckService: HealthCheckService = serviceHub.cordaService(MyHealthCheckService::class.java)
        healthCheckService.stopTimer()
        healthCheckService.overrideHealthWith(health)
    }
}
