import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonOutput
import groovy.xml.XmlUtil

def Message processData(Message message) {
    def body = message.getBody(String.class)

    // Fetch filters from headers. Assumes headers are in the format "filter_field/path/example: (123|321)&(!333|!111)"
    def filters = message.getHeaders()
            .findAll { it.key.startsWith("filter_") }
            .collectEntries { [(it.key - "filter_"): it.value] }

    // Parse XML
    def xmlParser = new XmlSlurper().parseText(body)

    // Apply filters
    filters.each { field, criteriaExpression ->
        // Create a GPath from the field
        def gpath = new groovy.util.GPathResult(null, field, null, null)

        // Split the expression into parts by '&' and '|'
        def andParts = criteriaExpression.split("&")
        def orParts = criteriaExpression.split("\\|")

        // Find and remove nodes that don't match the filter criteria
        xmlParser.depthFirst().findAll { node ->
            it[GPathResult.IDENTITY].name() == gpath.name() && !evaluateExpression(node.text(), andParts, orParts)
        }.replaceNode {}
    }

    // Convert filtered XML to JSON
    def jsonOutput = JsonOutput.toJson(xmlParser)

    // Set the JSON as the message body
    message.setBody(jsonOutput)

    return message
}

def boolean evaluateExpression(String nodeValue, List<String> andParts, List<String> orParts) {
    def andResult = andParts.every { part ->
        def value = part.replace("(", "").replace(")", "")
        value.startsWith("!") ? nodeValue != value.substring(1) : nodeValue == value
    }

    def orResult = orParts.any { part ->
        def value = part.replace("(", "").replace(")", "")
        value.startsWith("!") ? nodeValue != value.substring(1) : nodeValue == value
    }

    return andResult || orResult
}
