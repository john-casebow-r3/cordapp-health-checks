package com.r3.healthcheck.flows

import net.corda.core.identity.CordaX500Name
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before

abstract class NetworkSetup {

    lateinit var network: MockNetwork
    lateinit var node1: StartedMockNode
    lateinit var node2: StartedMockNode
    lateinit var node3: StartedMockNode

    @Before
    fun setup() {
        network = MockNetwork(
            MockNetworkParameters(
                cordappsForAllNodes = listOf(
                    TestCordapp.findCordapp("com.r3.healthcheck.flows")
                )
            ).withNetworkParameters(testNetworkParameters(minimumPlatformVersion = 4))
        )

        node1 = network.createPartyNode(CordaX500Name("Node1", "R3", "GB"))
        node2 = network.createPartyNode(CordaX500Name("Node2", "R3", "GB"))
        node3 = network.createPartyNode(CordaX500Name("Node3", "R3", "GB"))
        network.runNetwork()
    }

    @After
    fun tearDown() {
        if (::network.isInitialized) {
            network.stopNodes()
        }
    }
}