package com.hexagonkt.server

import org.testng.annotations.Test
import kotlin.test.assertFailsWith
import java.net.InetAddress.getByName as address

@Test class ServerTest {
    fun `default parameters`() {
        val server = Server(VoidAdapter, Router(), "name", address("localhost"), 9999)

        assert(server.serverName == "name")
        assert(server.portName == VoidAdapter.javaClass.simpleName)
        assert(server.bindAddress == address("localhost"))
        assert(server.bindPort == 9999)
    }

    fun `runtime port`() {
        val server = Server(VoidAdapter, Router(), "name", address("localhost"), 9999)

        assertFailsWith<IllegalStateException>("Server is not running") { server.runtimePort }
        assert(!server.started())

        server.run()

        assert(server.started())
        assert(server.runtimePort == 12345)
    }

    fun `parameters map`() {
        val router = router {}
        val server = Server(VoidAdapter, router = router)
        assert(equal (server, Server(VoidAdapter, router, mapOf<String, Any>())))
        val invalidSettings = mapOf("serviceName" to 0, "bindAddress" to 1, "bindPort" to true)
        assert(equal(server, Server(VoidAdapter, Router(), invalidSettings)))

        val settings = mapOf(
            "serviceName" to "name",
            "bindAddress" to "localhost",
            "bindPort" to 12345
        )
        val server1 = Server(VoidAdapter, router, "name", address("localhost"), 12345)
        assert(equal(server1, Server(VoidAdapter, Router(), settings)))
    }

    private fun equal(server1: Server, server2: Server) =
        server1.serverName == server2.serverName &&
        server1.bindAddress == server2.bindAddress &&
        server1.bindPort == server2.bindPort
}
