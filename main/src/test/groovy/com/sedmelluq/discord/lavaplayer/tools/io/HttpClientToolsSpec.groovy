package com.sedmelluq.discord.lavaplayer.tools.io

import org.apache.http.HttpResponse
import org.apache.http.HttpStatus
import org.apache.http.HttpVersion
import org.apache.http.message.BasicHttpResponse
import org.apache.http.message.BasicStatusLine
import spock.lang.Specification
import spock.lang.Unroll

class HttpClientToolsSpec extends Specification {

    private static HttpResponse responseWithStatus(int code) {
        new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, code, ""))
    }

    @Unroll
    def "getRedirectLocation returns null for non-redirect status #status"() {
        given:
        def response = responseWithStatus(status)

        expect:
        HttpClientTools.getRedirectLocation("http://example.com/", response) == null

        where:
        status << [HttpStatus.SC_OK, HttpStatus.SC_NOT_FOUND, HttpStatus.SC_INTERNAL_SERVER_ERROR]
    }

    @Unroll
    def "getRedirectLocation returns null when Location header is absent for redirect status #status"() {
        given:
        def response = responseWithStatus(status)

        expect:
        HttpClientTools.getRedirectLocation("http://example.com/", response) == null

        where:
        status << [301, 302, 303, 307, 308]
    }

    @Unroll
    def "getRedirectLocation resolves Location for redirect status #status"() {
        given:
        def response = responseWithStatus(status)
        response.addHeader("Location", "http://redirected.example.com/path")

        expect:
        HttpClientTools.getRedirectLocation("http://example.com/", response) == "http://redirected.example.com/path"

        where:
        status << [301, 302, 303, 307, 308]
    }

    def "getRedirectLocation treats 308 as a redirect (permanent redirect)"() {
        given:
        def response = responseWithStatus(308)
        response.addHeader("Location", "https://new.example.com/resource")

        expect:
        HttpClientTools.getRedirectLocation("http://old.example.com/resource", response) == "https://new.example.com/resource"
    }
}
