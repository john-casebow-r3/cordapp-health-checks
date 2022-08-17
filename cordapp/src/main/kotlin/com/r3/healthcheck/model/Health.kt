package com.r3.healthcheck.model

import net.corda.core.identity.CordaX500Name
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
class Health private constructor (
    val status: Status,
    val details: Map<String, Any>,
    val counterPartyHealth: Map<CordaX500Name, Health>) {

    @CordaSerializable
    enum class Status {
        UP, DOWN
    }

    @CordaSerializable
    data class Builder(
        var status: Status? = Status.UP,
        var details: MutableMap<String, Any> = mutableMapOf(),
        var counterPartyHealth: MutableMap<CordaX500Name, Health> = mutableMapOf()) {

        fun up() = apply { this.status = Status.UP }
        fun down() = apply { this.status = Status.DOWN }
        fun withStatus(status: Status) = apply { this.status = status }
        fun withDetails(key: String, value: Any) = apply { this.details.put(key, value) }
        fun withDetails(details: Map<String, Any>) = apply { this.details.putAll(details) }
        fun withCounterpartyHealth(name: CordaX500Name, health: Health) = apply { this.counterPartyHealth.put(name, health) }
        fun withCounterpartyHealth(health: Map<CordaX500Name, Health>) = apply { this.counterPartyHealth.putAll(health) }
        fun build() = Health(status!!, details.toMap(), counterPartyHealth.toMap())
    }

    fun aggregateStatus() : Health.Status {
        if (status == Status.DOWN) return Status.DOWN
        counterPartyHealth.values.map { it.status }.forEach { if (it == Status.DOWN) return Status.DOWN }
        return Status.UP
    }
}


