/**
 * Helper class for interacting with the Storm Runner Functional API
 */
package com.serena.air.plugin.srf

import com.serena.air.StepFailedException
import com.serena.air.http.HttpBaseClient
import com.serena.air.http.HttpResponse
import com.sun.javafx.fxml.builder.URLBuilder
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
    String accessToken

    SRFHelper(String serverUrl, String username, String password) {
        super(serverUrl, username, password)
    }

    @Override
    protected String getFullServerUrl(String serverUrl) {
         return serverUrl + "/api/v1"
    }

    protected String getAccessToken() {
        return this.accessToken
    }
    protected String setAccessToken(String accessToken) {
        this.accessToken = accessToken
        if (debug) { println "DEBUG - Access Token is: ${accessToken}" }
    }

    /**
     * Login to StormRunner Functional
     */
    def login() {

        def jsonBody = JsonOutput.toJson([loginName: username, password: password])
        URIBuilder builder = getUriBuilder("/access-tokens")
        //builder.addParameter("TENANTID", Long.toString(tenantId))
        HttpPost method = new HttpPost(builder.build())
        HttpEntity body = new StringEntity(jsonBody.toString(), ContentType.APPLICATION_JSON)
        method.entity = body
        if (debug) { println "DEBUG - Executing: ${method.toString()}"}
        HttpResponse response = execMethod(method)

        checkStatusCode(response.code)

        if (response.code != 200) {
            throw new StepFailedException("Unable to login")
        }

        this.setAccessToken(response.body.replaceAll('"', ''))

        /*
        // set LWSSO_COOKIE_KEY for subsequent requests
        BasicCookieStore cookieStore = new BasicCookieStore()
        defaultContext.cookieStore = cookieStore
        defaultContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore)
        BasicClientCookie cookie = new BasicClientCookie("LWSSO_COOKIE_KEY ", authToken)
        URL url=serverUri.toURL();
        cookie.setDomain(url.getHost());
        cookie.setPath(url.getPath());
        cookieStore.addCookie(cookie)
        */
    }

    /**
     * Run a test
     * @param workspaceId the workspace
     * @param testId the test id
     * @return the test run id
     */
    def runTest(String workspaceId, String testId) {
        def jsonBody = JsonOutput.toJson([testEntityIdentifier: testId])
        HttpResponse response = execPost("/workspaces/${workspaceId}/jobs", jsonBody)
        checkStatusCode(response.code)

        if (response.code != 200) {
            def json = new JsonSlurper().parseText(response.body)
            def message = json?.message
            if (debug) println "ERROR - running test: ${message}"
            if (message) {
                throw new StepFailedException("Error running test: ${message}")
            }
        } else {
            def json = new JsonSlurper().parseText(response.body)
            if (debug) println "DEBUG - JSON Response: ${json}"
            def jobs = json?.jobs
            def runId = jobs[0].testRunYac
            return runId
        }
    }

    /**
     * Get the status of a test run
     * @param workspaceId the workspace id
     * @param runId the test run id
     * @return a string containing the test run status
     */
    def runStatus(String workspaceId, String runId) {
        URI uri = new URIBuilder(this.serverUri.toString() + "/workspaces/${workspaceId}/testruns")
            .addParameter("entityIdentifier", runId)
            .addParameter("order", "asc")
            .build()
        HttpGet method = new HttpGet(uri)
        method.addHeader("Authorization", "Bearer${accessToken}")
        if (debug) { println "DEBUG - Executing: ${method.toString()}"}
        HttpResponse response = exec(method)
        checkStatusCode(response.code)

        if (response.code != 200) {
            throw new StepFailedException("Error retrieving test run status - ${response}")
        }

        def json = new JsonSlurper().parseText(response.body)
        if (debug) println "DEBUG - JSON Response: ${json}"
        def entities = json?.entities
        def status = entities[0].status
        return status
    }

    /**
     * Get the detailed results of a test run
     * @param workspaceId the workspace id
     * @param runId the test run id
     * @return a JSON string with the detailed results
     */
    def runResults(String workspaceId, String runId) {
        URI uri = new URIBuilder(this.serverUri.toString() + "/workspaces/${workspaceId}/testruns")
                .addParameter("entityIdentifier", runId)
                .addParameter("order", "asc")
                .build()
        HttpGet method = new HttpGet(uri)
        method.addHeader("Authorization", "Bearer${accessToken}")
        if (debug) { println "DEBUG - Executing: ${method.toString()}"}
        HttpResponse response = exec(method)
        checkStatusCode(response.code)

        if (response.code != 200) {
            throw new StepFailedException("Error retrieving test run status - ${response}")
        }

        def json = new JsonSlurper().parseText(response.body)
        if (debug) println "DEBUG - JSON Response: ${json}"
        return json
    }

    //
    // static methods
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
        //builder.addParameter("TENANTID", Long.toString(tenantId))
        HttpGet method = new HttpGet(builder.build())
        method.addHeader("Authorization", "Bearer${accessToken}")
        if (debug) { println "DEBUG - Executing: ${method.toString()}"}
        return execMethod(method)
    }

    private HttpResponse execPost(def url, def json) {
        URIBuilder builder = getUriBuilder(url.toString())
        //builder.addParameter("TENANTID", Long.toString(tenantId))
        HttpPost method = new HttpPost(builder.build())
        method.addHeader("Authorization", "Bearer${accessToken}")
        if (json) {
            HttpEntity body = new StringEntity(json.toString(), ContentType.APPLICATION_JSON)
            method.entity = body
        }
        if (debug) { println "DEBUG - Executing: ${method.toString()}"}
        return execMethod(method)
    }

    private HttpResponse execPut(def url, def json) {
        URIBuilder builder = getUriBuilder(url.toString())
        //builder.addParameter("TENANTID", Long.toString(tenantId))
        HttpPut method = new HttpGet(builder.build())
        method.addHeader("Authorization", "Bearer${accessToken}")
        if (json) {
            HttpEntity body = new StringEntity(json.toString(), ContentType.APPLICATION_JSON)
            method.entity = body
        }
        if (debug) { println "DEBUG - Executing: ${method.toString()}"}
        return execMethod(method)
    }

}
