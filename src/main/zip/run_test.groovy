// --------------------------------------------------------------------------------
// Run a functional test
// --------------------------------------------------------------------------------

import com.serena.air.StepFailedException
import com.serena.air.StepPropertiesHelper
import com.serena.air.plugin.srf.SRFHelper
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

String srfServerUrl = props.notNull('srfServerUrl')
String srfUser = props.notNull('srfUser')
String srfPassword = props.notNull('srfPassword')
long tenantId = props.notNullInt('tenantId')
long workspaceId = props.notNullInt('workspaceId')
long testId = props.notNullInt('testId')
boolean debugMode = props.optionalBoolean("debugMode", false)

println "----------------------------------------"
println "-- STEP INPUTS"
println "----------------------------------------"

//
// Print out each of the property values.
//
println "Working directory: ${workDir.canonicalPath}"
println "StormRunner Server URL: ${srfServerUrl}"
println "StormRunner User: ${srfUser}"
println "StormRunner Password: ${srfPassword}"
println "Tenant Id: ${tenantId}"
println "Workspace Id: ${workspaceId}"
println "Test Id: ${testId}"
println "Debug mode value: ${debugMode}"
if (debugMode) { props.setDebugLoggingMode() }

println "----------------------------------------"
println "-- STEP EXECUTION"
println "----------------------------------------"

def exitCode = 0
def testRunId

//
// The main body of the plugin step - wrap it in a try/catch statement for handling any exceptions.
//
try {
    SRFHelper srfClient = new SRFHelper(srfServerUrl, srfUser, srfPassword)
    srfClient.setTenantId(tenantId)
    srfClient.setSSL()
    srfClient.login()
    srfClient.setDebug(debugMode)

    testRunId = srfClient.runTest(Long.toString(workspaceId), Long.toString(testId))
    String url = "${srfServerUrl}/workspace/${workspaceId}/results/${testRunId}/details?TENANTID=${tenantId}"
    println "Successfully started test ${testId} as run id: ${testRunId}"
    println "For details see: ${url}"

} catch (StepFailedException e) {
    //
    // Catch any exceptions we find and print their details out.
    //
    println "ERROR - ${e.message}"
    // An exit with a non-zero value will be deemed a failure
    System.exit 1
}

println "----------------------------------------"
println "-- STEP OUTPUTS"
println "----------------------------------------"

apTool.setOutputProperty("testRunId", testRunId)
println("Setting \"testRunId\" output property to \"${testRunId}\"")
apTool.setOutputProperties()

//
// An exit with a zero value means the plugin step execution will be deemed successful.
//
System.exit(exitCode)
