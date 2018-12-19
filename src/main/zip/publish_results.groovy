// --------------------------------------------------------------------------------
// Publish the results of a test run to a file in HTML format
// --------------------------------------------------------------------------------

import com.serena.air.StepFailedException
import com.serena.air.StepPropertiesHelper
import com.serena.air.plugin.srl.SRLHelper
import com.urbancode.air.AirPluginTool

//
// Create some variables that we can use throughout the plugin step.
// These are mainly for checking what operating system we are running on.
//
final def PLUGIN_HOME = System.getenv()['PLUGIN_HOME']
final String lineSep = System.getProperty('line.separator')
final String osName = System.getProperty('os.name').toLowerCase(Locale.US)
final String pathSep = System.getProperty('path.separator')
final boolean windows = (osName =~ /windows/)
final boolean vms = (osName =~ /vms/)
final boolean os9 = (osName =~ /mac/ && !osName.endsWith('x'))
final boolean unix = (pathSep == ':' && !vms && !os9)
File workDir = new File('.').canonicalFile

//
// Initialise the plugin tool and retrieve all the properties that were sent to the step.
//
final def  apTool = new AirPluginTool(this.args[0], this.args[1])
final def  props  = new StepPropertiesHelper(apTool.getStepProperties(), true)

String srlServerUrl = props.notNull('srlServerUrl')
String srlUser = props.notNull('srlUser')
String srlPassword = props.notNull('srlPassword')
long tenantId = props.notNullInt('tenantId')
long projectId = props.notNullInt('projectId')
long runId = props.notNullInt('runId')
String outputFile = props.notNull("outputFile")
boolean debugMode = props.optionalBoolean("debugMode", false)

println "----------------------------------------"
println "-- STEP INPUTS"
println "----------------------------------------"

//
// Print out each of the property values.
//
println "Working directory: ${workDir.canonicalPath}"
println "StormRunner Server URL: ${srlServerUrl}"
println "StormRunner User: ${srlUser}"
println "StormRunner Password: ${srlPassword}"
println "Tenant Id: ${tenantId}"
println "Project Id: ${projectId}"
println "Test Run Id: ${runId}"
println "Output File: ${outputFile}"
println "Debug mode value: ${debugMode}"
if (debugMode) { props.setDebugLoggingMode() }

println "----------------------------------------"
println "-- STEP EXECUTION"
println "----------------------------------------"

def exitCode = 0

//
// The main body of the plugin step - wrap it in a try/catch statement for handling any exceptions.
//
try {

    SRLHelper srlClient = new SRLHelper(srlServerUrl, srlUser, srlPassword)
    srlClient.setTenantId(tenantId)
    srlClient.setSSL()
    srlClient.login()
    srlClient.setDebug(debugMode)

    def runResults = srlClient.runResults(Long.toString(runId))
    def sb = new StringBuilder()
    sb << """
<style type='text/css'>
    body { padding: 10px; 	background: #F5F5F5; font-family: Arial; }
    div { 
        background: #fff; box-shadow: 4px 4px 10px 0px rgba(119, 119, 119, 0.3);
        -moz-box-shadow: 4px 4px 10px 0px rgba(119, 119, 119, 0.3);
        -webkit-box-shadow: 4px 4px 10px 0px rgba(119, 119, 119, 0.3);
        margin-bottom: 20px; }
    a { color: #337ab7; text-decoration: underline; }
    a:hover { text-decoration: none; }
    span { font-weight: bold; }
    span.failed { color: #df1e1e; }
    span.success { color: #32ad12; }
    table { border-collapse: collapse; width: 100%; color: #333; }
    table td, table th { border: 1px solid #ddd; }
    table th { padding: 10px; text-align: center; background: #eee; font-size: 16px; }
    table td { padding: 7px; font-size: 12px; } " +
    table tr th:first-child { max-width: 200px; min-width: 200px; }
    table tr td:first-child { width: 200px; text-align: right; font-weight: bold;}
    table tr td:first-child:after {content: ':' }
</style>
"""
    sb << "<div> <table> <tr> <th colspan='2'>Test Run: ${runId} </th> </tr>"
    sb << "<tr> <td> Date & time </td> <td> ${runResults?.dateTime} </td> </tr>"
    sb << "<tr> <td> Status </td> <td> <span class=" + (("PASSED".equalsIgnoreCase(runResults?.status)) ? "success >" : "failed >") +  runResults?.status + "</td> </tr>"
    sb << "<tr> <td> Duration </td> <td> ${runResults?.duration} </td> </tr>"
    sb << "<tr> <td> GUI Vusers </td> <td> " + srlClient.fNull(runResults?.uiVusers) + " </td> </tr>"
    sb << "<tr> <td> GUI VUH </td> <td> " + srlClient.fNull(runResults?.uiVUH) + " </td> </tr>"
    sb << "<tr> <td> Average Throughput </td> <td> " + srlClient.fNull(runResults?.averageThroughput) + " </td> </tr>"
    sb << "<tr> <td> Total Throughput </td> <td> " + srlClient.fNull(runResults?.totalThroughput) + " </td> </tr>"
    sb << "<tr> <td> Average Hits </td> <td> " + srlClient.fNull(runResults?.averageHits) + " </td> </tr>"
    sb << "<tr> <td> Total Hits </td> <td> " + srlClient.fNull(runResults?.totalHits) + " </td> </tr>"
    sb << "<tr> <td> Total Transactions Passed </td> <td> " + srlClient.fNull(runResults?.totalTransactionsPassed) + " </td> </tr>"
    sb << "<tr> <td> Total Transactions Failed </td> <td> " + srlClient.fNull(runResults?.totalTransactionsFailed) + " </td> </tr>"
    String url = "${srlServerUrl}/run-overview/${runId}/dashboard/?TENANTID=${tenantId}&projectId=${projectId}"
    sb << "<tr> <td> Run URL" + "</td> <td> <a href='${url}'  target=_blank>" + url + "</a> </td> </tr>"
    sb << "<br>"
    sb << "</table> </div>"
    sb << "</body>"
    sb << "</html>"

    File f = new File(outputFile)
    BufferedWriter bw = new BufferedWriter(new FileWriter(f))
    bw.write(sb.toString())
    bw.close()

    println "Succesfully created file ${outputFile} in work area..."

} catch (StepFailedException e) {
    //
    // Catch any exceptions we find and print their details out.
    //
    println "ERROR - ${e.message}"
    // An exit with a non-zero value will be deemed a failure
    System.exit 1
}

//
// An exit with a zero value means the plugin step execution will be deemed successful.
//
System.exit(exitCode)
