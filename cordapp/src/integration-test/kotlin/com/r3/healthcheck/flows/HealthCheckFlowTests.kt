package com.r3.healthcheck.flows

import com.r3.healthcheck.model.Health
import org.junit.Test
import java.util.concurrent.Future
import kotlin.test.assertEquals

class HealthCheckFlowTests : NetworkSetup() {

    @Test
    fun healthcheck_runs_all_nodes_up() {
        var node1Future: Future<Health> = node1.startFlow(HealthCheckFlow())
        var node2Future: Future<Health> = node2.startFlow(HealthCheckFlow())
        var node3Future: Future<Health> = node3.startFlow(HealthCheckFlow())
        network.runNetwork()

        assertEquals(Health.Status.UP, node1Future.get().status)
        assertEquals(Health.Status.UP, node2Future.get().status)
        assertEquals(Health.Status.UP, node3Future.get().status)

        // Wait for the next scheduled check
        Thread.sleep(12000)
        network.runNetwork()

        Thread.sleep(2000)
        node1Future = node1.startFlow(HealthCheckFlow())
        network.runNetwork()

        assertEquals(Health.Status.UP, node1Future.get().status)
        assertEquals(Health.Status.UP, node1Future.get().aggregateStatus())
    }

    @Test
    fun healthcheck_returns_down_when_counterparty_is_down() {
        // Mark node2 down
        node2.startFlow(SetStatusFlow(Health.Builder().down().build()))
        network.runNetwork()

        // Wait for node1 to notice that node2 is down
        Thread.sleep(12000)
        network.runNetwork()

        var future: Future<Health> = node1.startFlow(HealthCheckFlow())
        network.runNetwork()

        val health = future.get()
        assertEquals(Health.Status.UP, health.status)
        assertEquals(Health.Status.DOWN, health.aggregateStatus())
    }
}