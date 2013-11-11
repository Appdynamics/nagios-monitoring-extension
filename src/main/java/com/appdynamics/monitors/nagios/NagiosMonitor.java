
package com.appdynamics.monitors.nagios;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.appdynamics.monitors.nagios.script.NagiosScript;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;

/**
 * @author
 */
public class NagiosMonitor extends AManagedMonitor
{
    private static final String METRICS_PREFIX = "Custom Metrics|Monitoring|Nagios|Status|"; // Controller Path.
    private static final Logger LOG = Logger.getLogger(NagiosMonitor.class); // The Logger.
    private static final int WORKER_COUNT = 10; // Max worker count can be altered via configuration.
    private static final int MAX_WAIT_TIME = 5; // Max wait time in minutes for the workers to go down.

    private String projectFolder; // The prokect folder.
    private Integer workerCount = WORKER_COUNT;
    private Integer maxWaitTime = MAX_WAIT_TIME;

    private Collection<NagiosScript> scripts; // Collection of tasks (implementing java.lang.Runnable)
    private final HashMap<String, String> results = new HashMap<String, String>(); // The results map.

    /**
     * This represents the various valid NAGIOS STATUS.
     * 
     * @author 
     */
    private enum STATES
    {
        OK, WARNING, CRITICAL, UNKNOWN; // States

        /**
         * @see java.lang.Enum#toString()
         */
        @Override
        public String toString()
        {
            return String.valueOf(this.ordinal());
        }
    }

    /**
     * 
     */
    public void print()
    {
        LOG.info("Printing metrics.");

        // Publish status to the controller instance.
        for (NagiosScript script : scripts) {
            if (!script.isRunAll()) {
                LOG.error("NAME: " + script.getName() + " VALUE: " + results.get(script.getId()));
                printMetric(
                    script.getName(),
                    (results.get(script.getId()) == null) ? STATES.UNKNOWN : results.get(script.getId()),
                    MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
                    MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);
            }
        }
    }

    /**
     * @param xml
     * @throws DocumentException
     */
    public void parseXML(final String xml) throws DocumentException
    {
        scripts = new LinkedList<NagiosScript>();

        final SAXReader reader = new SAXReader();
        final Document document = reader.read(xml);
        final Element root = document.getRootElement();

        for (Iterator<Element> i = root.elementIterator(); i.hasNext();) {
            Element element = (Element) i.next();

            if (element.getName().equals("script")) {
                Iterator<Element> elementIterator = element.elementIterator();

                if (element.attributeCount() > 0 && element.attribute(0).getName().equals("run-all")) {
                    String path = null;
                    int period = -1;
                    for (Iterator<Element> j = elementIterator; j.hasNext();) {
                        element = (Element) j.next();
                        if (element.getName().equals("path")) {
                            path = element.getText();
                        }
                        else if (element.getName().equals("period")) {
                            period = Integer.parseInt(element.getText());
                        }
                    }

                    try {
                        iterateFolder(new File(path), period);
                    }
                    catch (IOException e) {
                        LOG.error("Failed to gather scripts from the path given: " + path, e);
                    }
                }
                else {
                    final NagiosScript script = new NagiosScript();
                    for (Iterator<Element> j = elementIterator; j.hasNext();) {
                        element = (Element) j.next();
                        if (element.getName().equals("name")) {
                            script.setName(element.getText());
                        }
                        else if (element.getName().equals("path")) {
                            script.setPath(element.getText());
                        }
                        else if (element.getName().equals("period")) {
                            script.setPeriod(Integer.parseInt(element.getText()));
                        }
                        else if (element.getName().equals("script-arguments")) {
                            script.setArguments(element.getText());
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
    public void iterateFolder(final File folder, final Integer period) throws IOException
    {
        for (final File file : folder.listFiles()) {
            if (file.isDirectory()) {
                iterateFolder(file, period);
            }
            else if (!file.isHidden() && !file.getName().startsWith(".") && file.getName() != null) {
                final NagiosScript script = new NagiosScript();
                script.setId(UUID.randomUUID().toString());
                script.setName(file.getName());
                script.setPath(file.getAbsolutePath());
                script.setPeriod(period);
                scripts.add(script);
            }
        }
    }

    /**
     * Returns the metric to the AppDynamics Controller.
     * @param 	metricName		Name of the Metric
     * @param 	metricValue		Value of the Metric
     * @param 	aggregation		Average OR Observation OR Sum
     * @param 	timeRollup		Average OR Current OR Sum
     * @param 	cluster			Collective OR Individual
     */
    public void printMetric(
        final String metricName,
        final Object metricValue,
        final String aggregation,
        final String timeRollup,
        final String cluster)
    {
        final MetricWriter metricWriter = getMetricWriter(
            getMetricPrefix() + metricName, aggregation, timeRollup, cluster);
        metricWriter.printMetric(String.valueOf(metricValue));
    }

    /**
     * @return
     */
    protected String getMetricPrefix()
    {
        return METRICS_PREFIX;
    }

    /**
     * @see com.singularity.ee.agent.systemagent.api.ITask#execute(java.util.Map, com.singularity.ee.agent.systemagent.api.TaskExecutionContext)
     */
    public TaskOutput execute(final Map<String, String> args, final TaskExecutionContext context)
        throws TaskExecutionException
    {
        parseArgs(args);

        // Parse the scripts.xml.
        try {
            parseXML(projectFolder + "/conf/scripts.xml");
        }
        catch (DocumentException e) {
            LOG.error("Failed to parse XML." + e.toString());
        }

        for (;;) {
            // Initiate the pool with the pre defined work count.
            final ExecutorService executor = Executors.newFixedThreadPool(this.workerCount);

            // Add the tasks to execute them in some time in future.
            for (final NagiosScript script : scripts) {
                executor.execute(script);
            }

            // Ask the executor to gracefully shutdown after every thing is finished.
            executor.shutdown();

            // Now see that this pool shutsdown and does not go to eternity.
            try {
                executor.awaitTermination(this.maxWaitTime * 60, TimeUnit.SECONDS);
            }
            catch (InterruptedException e) {
                LOG.error("Failed to shutdown the executor in max wait time(in minutes):" + MAX_WAIT_TIME, e);
            }

            // After every thing has been done gracefully,
            // check the status of nagios script execution and send it to controller.
            // Gather status.
            for (final NagiosScript script : scripts) {
                this.results.put(script.getId(), String.valueOf(script.getExitCode()));

                if (script.getExitCode() > 2) {
                    LOG.warn("Unknown exit code for script: " + script.getName());
                }
            }

            // Send them across the controller.
            print();

            LOG.info("Scripts executed.");
        }
    }

    /**
     * @param args
     */
    public void parseArgs(final Map<String, String> args)
    {
        // Get the path.
        this.projectFolder = args.get("project_path");

        // worker_count, max_wait_time
        final String strMaxWaitTime = args.get("max_wait_time");
        final String strWorkerCount = args.get("worker_count");

        if (isNotEmpty(strMaxWaitTime)) {
            try {
                this.maxWaitTime = Integer.parseInt(strMaxWaitTime);
            }
            catch (IllegalArgumentException e) {
                // Ignore.
            }
        }

        if (isNotEmpty(strWorkerCount)) {
            try {
                this.workerCount = Integer.parseInt(strWorkerCount);
            }
            catch (IllegalArgumentException e) {
                // Ignore.
            }
        }
    }

    /**
     * @param input
     * @return
     */
    public static boolean isNotEmpty(final String input)
    {
        return null != input && !"".equals(input.trim());
    }

}
