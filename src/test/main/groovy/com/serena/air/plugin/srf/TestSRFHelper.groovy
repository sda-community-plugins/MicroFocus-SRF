package test.com.serena.air.plugin.srf

import com.serena.air.plugin.srl.SRFHelper

def srServerUrl = "https://stormrunner-load.saas.hpe.com"
def srUser = ""
def srPassword = ""
def srTenantId = 688301833

SRFHelper srfClient = new SRFHelper(srServerUrl, srUser, srPassword)
srfClient.setTenantId(srTenantId)
srfClient.setSSL()
srfClient.setDebug(true)
srfClient.login()

def projectId = "1"
def testId = "1"

String runId = srfClient.runTest(projectId, testId)
println "Test run id is ${runId}"
String runStatus = srfClient.runStatus(runId)
while (runStatus.equals("in-progress")) {
    println "Test run ${runId} is in progress... sleeping..."
    sleep(10000)
    runStatus = srfClient.runStatus(runId)
}
println "Test ${runId} final status: ${runStatus}"

def runResults = srfClient.runResults(runId)
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
sb << "<tr> <td> GUI Vusers </td> <td> " + srfClient.fNull(runResults?.uiVusers) + " </td> </tr>"
sb << "<tr> <td> GUI VUH </td> <td> " + srfClient.fNull(runResults?.uiVUH) + " </td> </tr>"
sb << "<tr> <td> Average Throughput </td> <td> " + srfClient.fNull(runResults?.averageThroughput) + " </td> </tr>"
sb << "<tr> <td> Total Throughput </td> <td> " + srfClient.fNull(runResults?.totalThroughput) + " </td> </tr>"
sb << "<tr> <td> Average Hits </td> <td> " + srfClient.fNull(runResults?.averageHits) + " </td> </tr>"
sb << "<tr> <td> Total Hits </td> <td> " + srfClient.fNull(runResults?.totalHits) + " </td> </tr>"
sb << "<tr> <td> Total Transactions Passed </td> <td> " + srfClient.fNull(runResults?.totalTransactionsPassed) + " </td> </tr>"
sb << "<tr> <td> Total Transactions Failed </td> <td> " + srfClient.fNull(runResults?.totalTransactionsFailed) + " </td> </tr>"
String url = "${srServerUrl}/run-overview/${runId}/dashboard/?TENANTID=${srTenantId}&projectId=${projectId}"
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



