package com.r3.healthcheck.model

import net.corda.core.identity.CordaX500Name
import org.hamcrest.Matchers.equalTo
import org.junit.Assert
import org.junit.Test

class HealthTest {

    @Test
    fun testBuilderConstruction() {
        val health = Health.Builder()
            .up()
            .withDetails("foo", "bar")
            .build()

        Assert.assertThat(Health.Status.UP, equalTo(health.status))
        Assert.assertThat("bar", equalTo(health.details["foo"] as String))
    }

    @Test
    fun testBuilderAggregationWithCounterpartyUp() {
        val counterparty = Health.Builder().up().withDetails("foo", "bar").build()
        val builder = Health.Builder().up().withDetails("bar", "buzz")
        val agg = builder.withCounterpartyHealth(CordaX500Name("CP", "Test", "GB"), counterparty).build()

        Assert.assertThat(Health.Status.UP, equalTo(agg.status))
        Assert.assertThat(Health.Status.UP, equalTo(agg.aggregateStatus()))
    }

    @Test
    fun testBuilderAggregationWithCounterpartyDown() {
        val counterparty = Health.Builder().down().withDetails("foo", "bar").build()
        val builder = Health.Builder().up().withDetails("bar", "buzz")
        val agg = builder.withCounterpartyHealth(CordaX500Name("CP", "Test", "GB"), counterparty).build()

        Assert.assertThat(Health.Status.UP, equalTo(agg.status))
        Assert.assertThat(Health.Status.DOWN, equalTo(agg.aggregateStatus()))
    }

    @Test
    fun testBuilderAggregationWithSelfDownAndCounterpartyUp() {
        val counterparty = Health.Builder().up().withDetails("foo", "bar").build()
        val builder = Health.Builder().down().withDetails("bar", "buzz")
        val agg = builder.withCounterpartyHealth(CordaX500Name("CP", "Test", "GB"), counterparty).build()

        Assert.assertThat(Health.Status.DOWN, equalTo(agg.status))
        Assert.assertThat(Health.Status.DOWN, equalTo(agg.aggregateStatus()))
    }
}