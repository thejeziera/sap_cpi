import com.sap.gateway.ip.core.customdev.util.Message;
import java.util.HashMap;
import org.w3c.dom.Node;
import groovy.xml.*

def Message processData(Message message) {

    def map = message.getHeaders();
    def messageLog = messageLogFactory.getMessageLog(message);

//fetch existing X-Message-ID
    def X_Message_ID = map.get("X-Message-ID");

//IF X-MESSAGE-ID already exist catch it for monitoring ELSE set CPI ID as new X-MESSAGE-ID for monitoring and beyond
    if(messageLog != null){

        if(X_Message_ID != null){

            messageLog.addCustomHeaderProperty("X-Message-ID", X_Message_ID);
        }
        else {
            def CPI_ID = map.get("SAP_MessageProcessingLogID");
            messageLog.addCustomHeaderProperty("X-Message-ID", CPI_ID);
            message.setHeader("X-Message-ID", CPI_ID);
        }
    }

    return message;
}