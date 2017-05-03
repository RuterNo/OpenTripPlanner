package org.opentripplanner.updater.siri;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.GraphUpdater;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.stoptime.TimetableSnapshotSource;
import org.rutebanken.siri20.util.SiriXml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.EstimatedTimetableDeliveryStructure;
import uk.org.siri.siri20.Siri;
import uk.org.siri.siri20.VehicleMonitoringDeliveryStructure;

import javax.jms.*;
import javax.xml.bind.JAXBException;
import java.util.List;


public class SiriActiveMQUpdater implements GraphUpdater {
    private static final Logger LOG = LoggerFactory.getLogger(SiriActiveMQUpdater.class);

    String activeMQUrl;

    String topicName;

    /**
     * Parent update manager. Is used to execute graph writer runnables.
     */
    protected GraphUpdaterManager updaterManager;

    private Connection connection;

    @Override
    public void setGraphUpdaterManager(GraphUpdaterManager updaterManager) {
        this.updaterManager = updaterManager;
    }

    @Override
    public void configure(Graph graph, JsonNode config) throws Exception {
        Preconditions.checkNotNull(config.path("activeMQUrl"), "'activeMQUrl' must be set");
        Preconditions.checkNotNull(config.path("topicName"), "'topicName' must be set");

        activeMQUrl = config.path("activeMQUrl").asText();

        // Prepending 'failover:' to support auto-reconnect on failure (ref: http://activemq.apache.org/how-can-i-support-auto-reconnection.html)
        if (!activeMQUrl.startsWith("failover:")) {
            activeMQUrl = "failover:" + activeMQUrl;
        }

        topicName = config.path("topicName").asText();

        if (graph.timetableSnapshotSource == null) {
            TimetableSnapshotSource snapshotSource = new TimetableSnapshotSource(graph);
            // Add snapshot source to graph
            graph.timetableSnapshotSource = (snapshotSource);
        }
    }

    @Override
    public void setup() throws Exception {

    }

    @Override
    public void run() throws Exception {

        // Getting JMS connection
        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(activeMQUrl);
        connection = connectionFactory.createConnection();
        connection.start();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        Topic topic = session.createTopic(topicName);

        final MessageConsumer consumer = session.createConsumer(topic);
        LOG.info("Listening for updates from {} on topic {}", activeMQUrl, topicName);

        consumer.setMessageListener(message -> {
            try {
                if (message instanceof TextMessage) {
                    TextMessage textMessage = (TextMessage) message;
                    Siri siri = null;
                    try {
                        siri = SiriXml.parseXml(textMessage.getText());
                    } catch (JAXBException e) {
                        e.printStackTrace();
                    }
                    if (siri != null && siri.getServiceDelivery() != null) {
                        if (siri.getServiceDelivery().getEstimatedTimetableDeliveries() != null &&
                                !siri.getServiceDelivery().getEstimatedTimetableDeliveries().isEmpty()) {
                            List<EstimatedTimetableDeliveryStructure> updates = siri.getServiceDelivery().getEstimatedTimetableDeliveries();
                            EstimatedTimetableGraphWriterRunnable runnable =
                                    new EstimatedTimetableGraphWriterRunnable(false, updates);
                            updaterManager.execute(runnable);
                        }
                        if (siri.getServiceDelivery().getVehicleMonitoringDeliveries() != null &&
                                !siri.getServiceDelivery().getVehicleMonitoringDeliveries().isEmpty()) {
                            List<VehicleMonitoringDeliveryStructure> updates = siri.getServiceDelivery().getVehicleMonitoringDeliveries();
                            VehicleMonitoringGraphWriterRunnable runnable =
                                    new VehicleMonitoringGraphWriterRunnable(false, updates);
                            updaterManager.execute(runnable);
                        }
                    }
                }
            } catch (JMSException e) {
                LOG.warn("Unable to process incoming SIRI-message", e);
            }
        });
    }

    @Override
    public void teardown() {

        try {
            connection.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }

    }

}
