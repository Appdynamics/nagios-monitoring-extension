
package com.appdynamics.monitors.nagios;

import com.appdynamics.monitors.nagios.script.NagiosScript;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

/**
 * @author
 */
public class NagiosMonitor extends AManagedMonitor {
    private static final String METRICS_PREFIX = "Custom Metrics|Monitoring|Nagios|Status|"; // Controller Path.
    private static final Logger LOG = Logger.getLogger(NagiosMonitor.class); // The Logger.
    private static final int WORKER_COUNT = 10; // Max worker count can be altered via configuration.

    private String projectFolder; // The project folder.
    private Integer workerCount = WORKER_COUNT;

    private Collection<NagiosScript> scripts; // Collection of tasks (implementing java.lang.Runnable)

    /**
     * This represents the various valid NAGIOS STATUS.
     *
     * @author
     */
    private enum STATES {
        OK, WARNING, CRITICAL, UNKNOWN; // States

        /**
         * @see java.lang.Enum#toString()
         */
        @Override
        public String toString() {
            return String.valueOf(this.ordinal());
        }
    }

    /**
     *
     */
    public void print() {
        LOG.info("Printing metrics.");

        // Publish status to the controller instance.
        for (NagiosScript script : scripts) {
            if (!script.isRunAll()) {
                String exitCode = String.valueOf(script.getExitCode());
                LOG.error("NAME: " + script.getName() + " VALUE: " + exitCode);
                printMetric(
                        script.getName(),
                        exitCode,
                        MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
                        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);
            }
        }
    }

    /**
     * @param xml
     * @throws DocumentException
     */
    public void parseXML(final String xml) throws DocumentException {
        scripts = new LinkedList<NagiosScript>();

        final SAXReader reader = new SAXReader();
        final Document document = reader.read(xml);
        final Element root = document.getRootElement();

        for (Iterator<Element> i = root.elementIterator(); i.hasNext(); ) {
            Element element = i.next();

            if (element.getName().equals("script")) {
                Iterator<Element> elementIterator = element.elementIterator();

                if (element.attributeCount() > 0 && element.attribute(0).getName().equals("run-all")) {
                    String path = null;
                    int period = -1;
                    int maxWaitTime = -1;
                    for (Iterator<Element> j = elementIterator; j.hasNext(); ) {
                        element = j.next();
                        if (element.getName().equals("path")) {
                            path = element.getText();
                        } else if (element.getName().equals("period")) {
                            period = Integer.parseInt(element.getText());
                        } else if (element.getName().equals("max-wait-time")) {
                            maxWaitTime = Integer.parseInt(element.getText());
                        }
                    }

                    try {
                        iterateFolder(new File(path), period, maxWaitTime);
                    } catch (IOException e) {
                        LOG.error("Failed to gather scripts from the path given: " + path, e);
                    }
                } else {
                    final NagiosScript script = new NagiosScript();
                    for (Iterator<Element> j = elementIterator; j.hasNext(); ) {
                        element = j.next();
                        if (element.getName().equals("name")) {
                            script.setName(element.getText());
                        } else if (element.getName().equals("path")) {
                            script.setPath(element.getText());
                        } else if (element.getName().equals("period")) {
                            script.setPeriod(Integer.parseInt(element.getText()));
                        } else if (element.getName().equals("script-arguments")) {
                            script.setArguments(element.getText());
                        } else if (element.getName().equals("max-wait-time")) {
                            script.setMaxWaitTime(Integer.parseInt(element.getText()));
                        }
                    }

                    script.setId(UUID.randomUUID().toString());
                    scripts.add(script);
                }
            }
        }
    }

    /**
     * @param folder
     * @param period
     * @throws IOException
     */
    public void iterateFolder(final File folder, final Integer period, final Integer maxWaitTime) throws IOException {
        for (final File file : folder.listFiles()) {
            if (file.isDirectory()) {
                iterateFolder(file, period, maxWaitTime);
            } else if (!file.isHidden() && !file.getName().startsWith(".") && file.getName() != null) {
                final NagiosScript script = new NagiosScript();
                script.setId(UUID.randomUUID().toString());
                script.setName(file.getName());
                script.setPath(file.getAbsolutePath());
                script.setPeriod(period);
                script.setMaxWaitTime(maxWaitTime);
                scripts.add(script);
            }
        }
    }

    /**
     * Returns the metric to the AppDynamics Controller.
     *
     * @param metricName  Name of the Metric
     * @param metricValue Value of the Metric
     * @param aggregation Average OR Observation OR Sum
     * @param timeRollup  Average OR Current OR Sum
     * @param cluster     Collective OR Individual
     */
    public void printMetric(
            final String metricName,
            final Object metricValue,
            final String aggregation,
            final String timeRollup,
            final String cluster) {
        final MetricWriter metricWriter = getMetricWriter(
                getMetricPrefix() + metricName, aggregation, timeRollup, cluster);
        metricWriter.printMetric(String.valueOf(metricValue));
    }

    /**
     * @return
     */
    protected String getMetricPrefix() {
        return METRICS_PREFIX;
    }

    /**
     * @see com.singularity.ee.agent.systemagent.api.ITask#execute(java.util.Map, com.singularity.ee.agent.systemagent.api.TaskExecutionContext)
     */
    public TaskOutput execute(final Map<String, String> args, final TaskExecutionContext context)
            throws TaskExecutionException {
        parseArgs(args);

        // Parse the scripts.xml.
        try {
            parseXML(projectFolder + "/conf/scripts.xml");
        } catch (DocumentException e) {
            LOG.error("Failed to parse XML." + e.toString());
        }

        final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(this.workerCount);

        // Add the tasks to execute them in some time in future.
        for (final NagiosScript script : scripts) {
            //Submit the periodic task according to period specified.
            scheduledThreadPoolExecutor.scheduleAtFixedRate(script, 0, script.getPeriod(), TimeUnit.SECONDS);
        }

        // Send the result across the controller.
        final ScheduledThreadPoolExecutor outputScheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);
        outputScheduledThreadPoolExecutor.scheduleAtFixedRate(new Runnable() {
            public void run() {
                print();
            }
        }, 0, 60, TimeUnit.SECONDS);

        //As this is continuous extension, make this thread wait indefinitely.
        CountDownLatch infiniteWait = new CountDownLatch(1);
        try {
            infiniteWait.await();   //Will make this thread to wait till the CountDownLatch reaches to 0.
        } catch (InterruptedException e) {
            LOG.error("Failed to wait indefinitely ", e);
        }

        return new TaskOutput("Finished executing Nagios Monitor");
    }

    /**
     * @param args
     */
    public void parseArgs(final Map<String, String> args) {
        // Get the path.
        this.projectFolder = args.get("project_path");

        final String strWorkerCount = args.get("worker_count");

        if (isNotEmpty(strWorkerCount)) {
            try {
                this.workerCount = Integer.parseInt(strWorkerCount);
            } catch (NumberFormatException e) {
                // Ignore.
            }
        }
    }

    /**
     * @param input
     * @return
     */
    public static boolean isNotEmpty(final String input) {
        return null != input && !"".equals(input.trim());
    }

}
