package com.r3.healthcheck.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.healthcheck.model.Health
import com.r3.healthcheck.services.HealthCheckService
import com.r3.healthcheck.services.MyHealthCheckService
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.utilities.unwrap

@InitiatingFlow
@StartableByRPC
class HealthCheckFlow : FlowLogic<Health>() {

    @Suspendable
    override fun call(): Health {
        logger.info("Running flow HealthCheckFlow")
        val healthCheckService: HealthCheckService = serviceHub.cordaService(MyHealthCheckService::class.java)
        return healthCheckService.currentHealth
    }
}

@InitiatingFlow
@StartableByService
class CounterpartyHealthCheckFlowInitiator(private val partyName: CordaX500Name) : FlowLogic<Health?>() {

    @Suspendable
    override fun call(): Health? {
        logger.info("${ourIdentity.name}  - running flow CounterpartyHealthCheckFlowInitiator for $partyName")
        return try {
            // Find the counterparty
            val party: Party = serviceHub.identityService.wellKnownPartyFromX500Name(partyName)
                ?: throw Exception("Cannot lookup counterparty $partyName")

            // Initiate session
            val session: FlowSession = initiateFlow(party)

            // Receive the health from the other party
            session.receive(Health::class.java).unwrap { it }
        } catch (e: Exception) {
            e.printStackTrace()
            return Health.Builder().down().withDetails("Exception", e.message!!).build()
        }
    }
}

@InitiatedBy(CounterpartyHealthCheckFlowInitiator::class)
class CounterpartyHealthCheckFlowResponder(private val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        logger.info("${ourIdentity.name}  - running flow CounterpartyHealthCheckFlowResponder")
        val healthCheckService: HealthCheckService = serviceHub.cordaService(MyHealthCheckService::class.java)
        counterpartySession.send(healthCheckService.currentHealth)
    }
}