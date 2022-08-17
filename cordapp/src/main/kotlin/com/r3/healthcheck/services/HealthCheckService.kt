package com.r3.healthcheck.services

import com.r3.healthcheck.flows.CounterpartyHealthCheckFlowInitiator
import com.r3.healthcheck.model.Health
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByService
import net.corda.core.identity.CordaX500Name
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate

abstract class HealthCheckService(private val serviceHub: AppServiceHub) : SingletonSerializeAsToken() {

    val logger: Logger = LoggerFactory.getLogger(HealthCheckService::class.java)

    abstract fun getCounterparties() : List<CordaX500Name>
    abstract fun getSelfHealth() : Health

    var healthTimer: Timer? = null
    var currentHealth: Health = Health.Builder().up().build()

    val myName = serviceHub.myInfo.legalIdentities.first().name

    init {
        serviceHub.register {
            startTimer()
        }
    }

    fun startTimer() {
        if (healthTimer == null) {
            healthTimer = Timer("schedule", true)

            // schedule at a fixed rate
            healthTimer?.scheduleAtFixedRate(1000, 60 * 1000) {
                currentHealth = calculateHealth()
            }
        }
    }

    fun stopTimer() {
        healthTimer?.cancel()
    }

    private fun calculateHealth(): Health {
        // Health is an aggregation of our health plus that of counterparties
        logger.info("$myName - calculating health...")
        val self = getSelfHealth()
        val builder = Health.Builder(self.status, self.details.toMutableMap())

        getCounterparties().forEach {
            logger.info("$myName - getting health from counterparty $it")
            val health = serviceHub.startFlow(CounterpartyHealthCheckFlowInitiator(it)).returnValue.get()
            logger.info("$myName - counterparty $it is ${health!!.status}")
            builder.withCounterpartyHealth(it, health)
        }
        logger.info("$myName - done.")
        return builder.build()
    }
}

@CordaService
class MyHealthCheckService(private val serviceHub: AppServiceHub) : HealthCheckService(serviceHub) {
    override fun getCounterparties(): List<CordaX500Name> {
        val myName = serviceHub.myInfo.legalIdentities.first().name
        return listOf(
            CordaX500Name("Node1", "R3", "GB"),
            CordaX500Name("Node2", "R3", "GB"),
            CordaX500Name("Node3", "R3", "GB")
        ).filter { it.toString() != myName.toString() }
    }

    override fun getSelfHealth(): Health {
        val builder = Health.Builder().up()

        // Notary Check
        if (serviceHub.networkMapCache.notaryIdentities.size == 0) {
            logger.info("$myName - no notaries configured!")
            builder.withStatus(Health.Status.DOWN)
            builder.withDetails("NOTARY", "No notary configured")
        }

        // DB check
        try {
            serviceHub.startFlow(DBCheckFlow())
        } catch (e: Exception) {
            e.printStackTrace()
            logger.info("$myName - cannot connect to DB!")
            builder.withStatus(Health.Status.DOWN)
            builder.withDetails("DB", e.message!!)
        }

        return builder.build()
    }

    @StartableByService
    class DBCheckFlow : FlowLogic<Unit>() {
        override fun call() {
            serviceHub.jdbcSession()
        }
    }
}


