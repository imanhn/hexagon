package com.hexagonkt.http.server

import com.hexagonkt.http.client.Client
import com.hexagonkt.http.client.ahc.AhcAdapter
import com.hexagonkt.http.server.jetty.JettyServletAdapter
import com.hexagonkt.injection.forceBind
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.net.HttpCookie

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MockServerRoutesTest {

    val server by lazy {
        MockServer("openapi_test.json").server
    }
    val client by lazy {
        Client(AhcAdapter(), endpoint = "http://localhost:${server.runtimePort}")
    }

    @BeforeAll fun setUp() {
        forceBind(ServerPort::class, JettyServletAdapter())
        server.start()
        assert(server.started())
    }

    @Test fun `Basic routes are created correctly`() {
        val response = client.get("/ping")
        assert(response.status == 200)
        assert(response.body == "pong")
    }

    @Test fun `Examples are fetched from mediatype schema correctly`() {
        val response = client.get("/get-example-from-schema")
        assert(response.status == 200)
        assert(response.body == "response")
    }

    @Test fun `Examples are fetched from mediatype correctly`() {
        val response = client.get("/get-example-from-mediatype")
        assert(response.status == 200)
        assert(response.body == "response")
    }

    @Test fun `Examples are fetched from multiple examples correctly`() {
        val response = client.get("/get-from-multiple-examples")
        assert(response.status == 200)
        assert(response.body in listOf("foo", "bar"))
    }

    @Test fun `X-Mock-Response-Example is fetched from multiple examples correctly`() {
        val headers = mapOf("X-Mock-Response-Example" to listOf("example2"))
        val response = client.get("/get-from-multiple-examples", headers = headers)
        assert(response.status == 200)
        assert(response.body == "bar")
    }

    @Test fun `Empty string is returned if no examples specified`() {
        val response = client.get("/get-from-no-examples")
        assert(response.status == 500)
    }

    @Test fun `Paths not present in OpenAPI spec return 404`() {
        val response = client.get("/unknown-path")
        assert(response.status == 404)
    }

    @Test fun `Required query params are verified correctly`() {
        val response1 = client.get("/check-query-param")
        assert(response1.status == 400)
        assert(response1.body == "invalid or missing query param")

        val response2 = client.get("/check-query-param?queryParam=aValidValue")
        assert(response2.status == 200)
        assert(response2.body == "success")

        val response3 = client.get("/check-query-param?queryParam=anInvalidValue")
        assert(response3.status == 400)
        assert(response3.body == "invalid or missing query param")
    }

    @Test fun `Optional query params are verified correctly`() {
        val response1 = client.get("/check-optional-query-param")
        assert(response1.status == 200)
        assert(response1.body == "success")

        val response2 = client.get("/check-optional-query-param?queryParam=aValidValue")
        assert(response2.status == 200)
        assert(response2.body == "success")

        val response3 = client.get("/check-optional-query-param?queryParam=anInvalidValue")
        assert(response3.status == 400)
        assert(response3.body == "invalid or missing query param")
    }

    @Test fun `Path params are verified correctly`() {
        val response1 = client.get("/check-path-param/aValidValue")
        assert(response1.status == 200)
        assert(response1.body == "success")

        val response2 = client.get("/check-path-param/anInvalidValue")
        assert(response2.status == 400)
        assert(response2.body == "invalid or missing path param")
    }

    @Test fun `Required header params are verified correctly`() {
        val response1 = client.get("/check-header-param")
        assert(response1.status == 400)
        assert(response1.body == "invalid or missing header param")

        val validHeaders = mapOf("headerParam" to listOf("aValidValue"))
        val response2 = client.get("/check-header-param", headers = validHeaders)
        assert(response2.status == 200)
        assert(response2.body == "success")

        val invalidHeaders = mapOf("headerParam" to listOf("anInvalidValue"))
        val response3 = client.get("/check-header-param", headers = invalidHeaders)
        assert(response3.status == 400)
        assert(response3.body == "invalid or missing header param")
    }

    @Test fun `Optional header params are verified correctly`() {
        val response1 = client.get("/check-optional-header-param")
        assert(response1.status == 200)
        assert(response1.body == "success")

        val validHeaders = mapOf("headerParam" to listOf("aValidValue"))
        val response2 = client.get("/check-optional-header-param", headers = validHeaders)
        assert(response2.status == 200)
        assert(response2.body == "success")

        val invalidHeaders = mapOf("headerParam" to listOf("anInvalidValue"))
        val response3 = client.get("/check-optional-header-param", headers = invalidHeaders)
        assert(response3.status == 400)
        assert(response3.body == "invalid or missing header param")
    }

    @Test fun `Required cookies are verified correctly`() {
        client.cookies["cookieParam"] = HttpCookie("cookieParam", "aValidValue")
        val response1 = client.get("/check-cookie-param")
        assert(response1.status == 200)
        assert(response1.body == "success")
        client.cookies.clear()

        client.cookies["cookieParam"] = HttpCookie("cookieParam", "anInvalidValue")
        val response2 = client.get("/check-cookie-param")
        assert(response2.status == 400)
        assert(response2.body == "invalid or missing cookie param")
        client.cookies.clear()

        val response3 = client.get("/check-cookie-param")
        assert(response3.status == 400)
        assert(response3.body == "invalid or missing cookie param")
    }

    @Test fun `Optional cookies are verified correctly`() {
        client.cookies["cookieParam"] = HttpCookie("cookieParam", "aValidValue")
        val response1 = client.get("/check-optional-cookie-param")
        assert(response1.status == 200)
        assert(response1.body == "success")
        client.cookies.clear()

        client.cookies["cookieParam"] = HttpCookie("cookieParam", "anInvalidValue")
        val response2 = client.get("/check-optional-cookie-param")
        assert(response2.status == 400)
        assert(response2.body == "invalid or missing cookie param")
        client.cookies.clear()

        val response3 = client.get("/check-optional-cookie-param")
        assert(response3.status == 200)
        assert(response3.body == "success")
    }

    @Test fun `Body is verified correctly`() {
        val response1 = client.get("/check-body", body = "Some body content")
        assert(response1.status == 200)
        assert(response1.body == "success")

        val response2 = client.get("/check-body")
        assert(response2.status == 400)
        assert(response2.body == "invalid or missing request body")
    }

    @AfterAll fun tearDown() {
        server.stop()
    }
}
