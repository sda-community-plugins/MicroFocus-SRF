/**
 * Helper class for interacting with the Storm Runner Functional API
 */
package com.serena.air.plugin.srf

import com.serena.air.StepFailedException
import com.serena.air.http.HttpBaseClient
import com.serena.air.http.HttpResponse
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.apache.http.HttpEntity
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpPut
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.client.utils.URIBuilder
import org.apache.http.conn.HttpHostConnectException
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.BasicCookieStore
import org.apache.http.impl.cookie.BasicClientCookie

class SRFHelper extends HttpBaseClient {

    long tenantId = 101
    boolean debug = false

    SRFHelper(String serverUrl, String username, String password) {
        super(serverUrl, username, password)
    }

    @Override
    protected String getFullServerUrl(String serverUrl) {
         return serverUrl
    }

    /**
     * Login to StormRunner Load
     */
    def login() {
        def jsonBody = JsonOutput.toJson([user: username, password: password])

        HttpResponse response = execPost("/v1/auth", jsonBody)
        checkStatusCode(response.code)

        if (response.code != 200) {
            throw new StepFailedException("Unable to login")
        }

        def json = new JsonSlurper().parseText(response.body)
        String authToken = json?.token

        // set LWSSO_COOKIE_KEY for subsequent requests
        BasicCookieStore cookieStore = new BasicCookieStore()
        defaultContext.cookieStore = cookieStore
        defaultContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore)
        BasicClientCookie cookie = new BasicClientCookie("LWSSO_COOKIE_KEY ", authToken)
        URL url=serverUri.toURL();
        cookie.setDomain(url.getHost());
        cookie.setPath(url.getPath());
        cookieStore.addCookie(cookie)
    }

    def runTest(String projectId, String testId) {
        HttpResponse response = execPost("/v1/projects/${projectId}/load-tests/${testId}/runs", null)
        checkStatusCode(response.code)

        if (response.code != 200) {
            def json = new JsonSlurper().parseText(response.body)
            def message = json?.message
            if (debug) println "Error running test: ${message}"
            if (message) {
                throw new StepFailedException("Error running test: ${message}")
            }

        } else {
            def json = new JsonSlurper().parseText(response.body)
            def runId = json?.runId
            if (debug) println "Started test ${testId} as run Id: ${runId}"
            return runId
        }
    }

    def runStatus(String runId) {
        HttpResponse response = execGet("/v1/test-runs/${runId}/status")

        checkStatusCode(response.code)
        def json = new JsonSlurper().parseText(response.body)

        if (response.code != 200) {
            if (debug) println "Error retrieving status: ${json?.message}"
            return null
        }

        if (debug) println "Status of test run ${runId} is ${json?.status} - ${json?.detailedStatus}"
        return json?.status
    }

    def runResults(String runId) {
        HttpResponse response = execGet("/v1/test-runs/${runId}/results")
        checkStatusCode(response.code)
        def json = new JsonSlurper().parseText(response.body)

        if (response.code != 200) {
            if (debug) println "Error retrieving status: ${json?.message}"
            return null
        }

        if (debug) println "Result of test run ${runId} was ${json?.status}"
        return json
    }

    //

    static List<String> csvToList(String csv) {
        if (!csv.replaceAll(',', '').trim()) {
            throw new StepFailedException('List of IDs is empty!')
        }

        def result = []

        csv.split(',').each {
            def trimmedValue = it.trim()

            if (trimmedValue) {
                result << trimmedValue
            }
        }

        return result
    }

    static def fNull(def value) {
        return (value == null ? "0" : value)
    }

    //
    // private methods
    //

    private def getProjectsJson() {
        HttpResponse response = execGet("/v1/projects")

        if (response.code != 200) {
            return null
        }

        checkStatusCode(response.code)

        return new JsonSlurper().parseText(response.body)
    }

    private def getLoadTestsJson(String projectId) {
        HttpResponse response = execGet("/v1/projects/${projectId}/load-tests")

        if (response.code != 200) {
            return null
        }

        checkStatusCode(response.code)

        return new JsonSlurper().parseText(response.body)
    }


    //
    // HTTP Methods
    //

    private HttpResponse execMethod(def method) {
        try {
            return exec(method)
        } catch (UnknownHostException e) {
            throw new StepFailedException("Unknown host: ${e.message}")
        } catch (HttpHostConnectException ignore) {
            throw new StepFailedException('Connection refused!')
        }
    }

    private HttpResponse execGet(def url) {
        URIBuilder builder = getUriBuilder(url.toString())
        builder.addParameter("TENANTID", Long.toString(tenantId))
        HttpGet method = new HttpGet(builder.build())
        return execMethod(method)
    }

    private HttpResponse execPost(def url, def json) {
        URIBuilder builder = getUriBuilder(url.toString())
        builder.addParameter("TENANTID", Long.toString(tenantId))
        HttpPost method = new HttpPost(builder.build())
        if (json) {
            HttpEntity body = new StringEntity(json.toString(), ContentType.APPLICATION_JSON)
            method.entity = body
        }
        return execMethod(method)
    }

    private HttpResponse execPut(def url, def json) {
        URIBuilder builder = getUriBuilder(url.toString())
        builder.addParameter("TENANTID", Long.toString(tenantId))
        HttpPut method = new HttpGet(builder.build())
        if (json) {
            HttpEntity body = new StringEntity(json.toString(), ContentType.APPLICATION_JSON)
            method.entity = body
        }
        return execMethod(method)
    }

}
