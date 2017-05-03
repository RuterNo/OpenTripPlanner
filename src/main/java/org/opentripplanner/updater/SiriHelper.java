package org.opentripplanner.updater;

import org.rutebanken.siri20.util.SiriXml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import uk.org.siri.siri20.*;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.DocumentBuilderFactory;
import java.time.ZonedDateTime;
import java.util.UUID;

public class SiriHelper {
    private static final Logger LOG =
            LoggerFactory.getLogger(SiriHelper.class);

    private static Siri createSiriObject() {
        Siri request = new Siri();
        request.setVersion("2.0");

        return request;
    }



    public static String createSXServiceRequestAsXml(String requestorRef) throws JAXBException {
        Siri request = createSXServiceRequest(requestorRef);
        return SiriXml.toXml(request);
    }

    public static String createVMServiceRequestAsXml(String requestorRef) throws JAXBException {
        Siri request = createVMServiceRequest(requestorRef);
        return SiriXml.toXml(request);
    }

    public static String createETServiceRequestAsXml(String requestorRef) throws JAXBException {
        Siri request = createETServiceRequest(requestorRef);
        return SiriXml.toXml(request);
    }

    private static Siri createSXServiceRequest(String requestorRefValue) {
        Siri request = createSiriObject();

        ServiceRequest serviceRequest = new ServiceRequest();
        serviceRequest.setRequestTimestamp(ZonedDateTime.now());

        RequestorRef requestorRef = new RequestorRef();
        requestorRef.setValue(requestorRefValue);
        serviceRequest.setRequestorRef(requestorRef);

        SituationExchangeRequestStructure sxRequest = new SituationExchangeRequestStructure();
        sxRequest.setRequestTimestamp(ZonedDateTime.now());
        sxRequest.setVersion("2.0");

        MessageQualifierStructure messageIdentifier = new MessageQualifierStructure();
        messageIdentifier.setValue(UUID.randomUUID().toString());

        sxRequest.setMessageIdentifier(messageIdentifier);
        serviceRequest.getSituationExchangeRequests().add(sxRequest);

        request.setServiceRequest(serviceRequest);

        return request;
    }

    private static Siri createETServiceRequest(String requestorRefValue) {
        Siri request = createSiriObject();

        ServiceRequest serviceRequest = new ServiceRequest();
        serviceRequest.setRequestTimestamp(ZonedDateTime.now());

        RequestorRef requestorRef = new RequestorRef();
        requestorRef.setValue(requestorRefValue);
        serviceRequest.setRequestorRef(requestorRef);

        EstimatedTimetableRequestStructure etRequest = new EstimatedTimetableRequestStructure();
        etRequest.setRequestTimestamp(ZonedDateTime.now());
        etRequest.setVersion("2.0");

        MessageQualifierStructure messageIdentifier = new MessageQualifierStructure();
        messageIdentifier.setValue(UUID.randomUUID().toString());

        etRequest.setMessageIdentifier(messageIdentifier);
        serviceRequest.getEstimatedTimetableRequests().add(etRequest);

        request.setServiceRequest(serviceRequest);

        return request;
    }

    private static Siri createVMServiceRequest(String requestorRefValue) {
        Siri request = createSiriObject();

        ServiceRequest serviceRequest = new ServiceRequest();
        serviceRequest.setRequestTimestamp(ZonedDateTime.now());

        RequestorRef requestorRef = new RequestorRef();
        requestorRef.setValue(requestorRefValue);
        serviceRequest.setRequestorRef(requestorRef);

        VehicleMonitoringRequestStructure vmRequest = new VehicleMonitoringRequestStructure();
        vmRequest.setRequestTimestamp(ZonedDateTime.now());
        vmRequest.setVersion("2.0");

        MessageQualifierStructure messageIdentifier = new MessageQualifierStructure();
        messageIdentifier.setValue(UUID.randomUUID().toString());

        vmRequest.setMessageIdentifier(messageIdentifier);
        serviceRequest.getVehicleMonitoringRequests().add(vmRequest);

        request.setServiceRequest(serviceRequest);

        return request;
    }

}
