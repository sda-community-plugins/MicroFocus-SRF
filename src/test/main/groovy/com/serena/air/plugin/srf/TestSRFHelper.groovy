package test.com.serena.air.plugin.srf

import com.serena.air.plugin.srf.SRFHelper

def srServerUrl = "https://ftaas-eu1.saas.hpe.com"
def srUser = ""
def srPassword = ""
def srTenantId = 711050852
def workspaceId = "1000"

SRFHelper srfClient = new SRFHelper(srServerUrl, srUser, srPassword)
srfClient.setTenantId(srTenantId)
srfClient.setSSL()
srfClient.setDebug(true)
srfClient.login()

def testId = "1"

def runId = srfClient.runTest(workspaceId, testId)
println "Test run id is ${runId}"

String runStatus = srfClient.runStatus(workspaceId, runId)
while (runStatus.equals("running")) {
    println "Test run ${runId} is in progress... sleeping..."
    sleep(10000)
    runStatus = srfClient.runStatus(workspaceId, runId)
}
println "Test ${runId} final status: ${runStatus}"

def runResults = srfClient.runResults(workspaceId, runId)
println runResults

def entity = runResults?.entities[0]
println entity
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
    span.errored { color: #df1e1e; }
    span.completed { color: #32ad12; }
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
sb << "<tr> <td> Test Name </td> <td> ${entity?.name} </td> </tr>"
sb << "<tr> <td> Test Type </td> <td> ${entity?.testType} </td> </tr>"
sb << "<tr> <td> Date & time </td> <td> ${entity?.start} </td> </tr>"
sb << "<tr> <td> Status </td> <td> <span class=${entity?.status}> ${entity?.status} </span> </td> </tr>"
sb << "<tr> <td> User </td> <td> ${entity?.user?.name} </td> </tr>"
sb << "<tr> <td> Total Environments </td> <td> " + srfClient.fNull(entity?.additionalData?.environmentCount) + " </td> </tr>"
sb << "<tr> <td> Total Scripts </td> <td> " + srfClient.fNull(entity?.additionalData?.scriptCount) + " </td> </tr>"
sb << "<tr> <td> Total Scripts Successful </td> <td> " + srfClient.fNull(entity?.additionalData?.scriptStatus?.success) + " </td> </tr>"
sb << "<tr> <td> Total Scripts Failed </td> <td> " + srfClient.fNull(entity?.additionalData?.scriptStatus?.failed) + " </td> </tr>"
String url = "${srServerUrl}/workspace/${workspaceId}/results/${runId}/details?TENANTID=${srTenantId}"
sb << "<tr> <td> Run URL" + "</td> <td> <a href='${url}'  target=_blank>" + url + "</a> </td> </tr>"
sb << "<br>"
sb << "</table> </div>"
sb << "</body>"
sb << "</html>"

String name = "result"
File f = new File(name + ".html")
BufferedWriter bw = new BufferedWriter(new FileWriter(f))
bw.write(sb.toString())
bw.close()



