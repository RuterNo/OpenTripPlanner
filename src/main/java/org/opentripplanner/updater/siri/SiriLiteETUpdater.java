/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.updater.siri;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.*;
import org.opentripplanner.updater.stoptime.TimetableSnapshotSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.EstimatedTimetableDeliveryStructure;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Update OTP stop time tables from some (realtime) source
 *
 * Usage example ('rt' name is an example) in file 'Graph.properties':
 *
 * <pre>
 * rt.type = stop-time-updater
 * rt.frequencySec = 60
 * rt.sourceType = gtfs-http
 * rt.url = http://host.tld/path
 * rt.feedId = TA
 * </pre>
 *
 */
public class SiriLiteETUpdater extends SiriETUpdater {

    private static final Logger LOG = LoggerFactory.getLogger(SiriLiteETUpdater.class);

    /**
     * Update streamer
     */
    private EstimatedTimetableSource updateSource;

    @Override
    public void configurePolling(Graph graph, JsonNode config) throws Exception {
        super.configurePolling(graph, config);

        updateSource = new SiriLiteETHttpTripUpdateSource();

        // Configure update source
        if (updateSource instanceof JsonConfigurable) {
            ((JsonConfigurable) updateSource).configure(graph, config);
        } else {
            throw new IllegalArgumentException(
                    "Unknown update streamer source type: " + updateSource);
        }

        LOG.info("Creating stop time updater (SIRI Lite ET) running every {} seconds : {}", frequencySec, updateSource);
    }

    /**
     * Repeatedly makes blocking calls to an UpdateStreamer to retrieve new stop time updates, and
     * applies those updates to the graph.
     */
    @Override
    public void runPolling() {
        // Get update lists from update source
        List<EstimatedTimetableDeliveryStructure> updates = updateSource.getUpdates();
        boolean fullDataset = updateSource.getFullDatasetValueOfLastUpdates();

        if (updates != null) {
            // Handle trip updates via graph writer runnable
            EstimatedTimetableGraphWriterRunnable runnable =
                    new EstimatedTimetableGraphWriterRunnable(fullDataset, updates);
            super.updaterManager.execute(runnable);
        }
    }

    @Override
    public void teardown() {
    }

    public String toString() {
        String s = (updateSource == null) ? "NONE" : updateSource.toString();
        return "Polling SIRI Lite ET updater with update source = " + s;
    }
}
