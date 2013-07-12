package com.appdynamics.monitors.nagios;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

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

public class NagiosMonitor extends AManagedMonitor
{
	private String project_folder;
	private static ArrayList<NagiosScript> scripts;
	public static Logger logger = Logger.getLogger(NagiosMonitor.class);
	private enum STATES {
		OK,
		WARNING,
		CRITICAL,
		UNKNOWN;

		@Override
		public String toString() {
			return String.valueOf(this.ordinal());
		}
	}

	public static final HashMap<String, String> results = new HashMap<String, String>();
	private static int duration = 0;

	public void print() {
		logger.error("PRINTING METRICS");
		for (NagiosScript script : scripts) {
			if (!script.run_all) {
				logger.error("NAME: " + script.name + " VALUE: " + results.get(script.id));
				printMetric(script.name,
					(results.get(script.id) == null) ? STATES.UNKNOWN : results.get(script.id),
					MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
					MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
					MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE
				);
			}
		}
	}

	public void parseXML(String xml) throws DocumentException {
		scripts = new ArrayList<NagiosScript>();
		SAXReader reader = new SAXReader();
		Document document = reader.read(xml);
		Element root = document.getRootElement();
		for (Iterator<Element> i = root.elementIterator(); i.hasNext();) {
			Element element = (Element)i.next();
			if (element.getName().equals("script")) {
				Iterator<Element> elementIterator = element.elementIterator();
				NagiosScript script = new NagiosScript();
				if (element.attributeCount() > 0 && element.attribute(0).getName().equals("run-all")) {
					script.run_all = true;
					script.force_stop = false;
				}
				for (Iterator<Element> j = elementIterator; j.hasNext();) {
					element = (Element)j.next();
					if (element.getName().equals("name")) {
						script.name = element.getText();
					}
					else if (element.getName().equals("path")) {
						script.path = element.getText();
					}
					else if (element.getName().equals("period")) {
						script.period = Integer.parseInt(element.getText());
					}
					else if (element.getName().equals("script-arguments")) {
						script.arguments = element.getText();
					}
				}
				script.id = UUID.randomUUID().toString();
				scripts.add(script);
			}
		}
	}

	public void iterateFolder(File folder, Integer period) throws IOException {
		for (File file : folder.listFiles()) {
			if (file.isDirectory()) {
				iterateFolder(file, period);
			} 
			else if (!file.isHidden() && !file.getName().startsWith(".") && file.getName() != null) {
				NagiosScript script = new NagiosScript();
				script.id = UUID.randomUUID().toString();
				script.name = file.getName();
				script.path = file.getAbsolutePath();
				script.period = period;
				scripts.add(script);
			}
		}
	}

	public void executeScript(final NagiosScript script, final int i) throws IOException {

		logger.error("---SCRIPT PARAMS---");
		logger.error(script.arguments);
		logger.error(script.force_stop);
		logger.error(script.id);
		logger.error(script.name);
		logger.error(script.path);
		logger.error(script.period);
		logger.error(script.period_passed);
		logger.error(script.run_all);
		logger.error(script.started);
		logger.error(script.thread);
		logger.error("-------------------");
		
		if (script.run_all) {
			iterateFolder(new File(script.path), script.period);
			scripts.remove(i);
			return;
		}

		if (scripts.get(i).thread == null) 
		{
			scripts.get(i).thread = new Thread(new Runnable() 
			{
				@Override
				public void run() {
					try {
						logger.error("Running thread for script: " + script.name);
						Process p = Runtime.getRuntime().exec(script.path + " " + script.arguments);
	
						int exitCode = p.waitFor();
						logger.error("EXIT CODE: " + exitCode + " for " + script.name);
						results.put(script.id, String.valueOf(exitCode));
						Thread.sleep(script.period * 1000);
					}
					catch (IOException ex) {
						logger.error(ex);
							System.err.println(ex);
					} catch (InterruptedException e) {
						logger.error(e);
						System.out.println("Interrupt Received");
					} finally {
						scripts.get(i).period_passed = 0;
						scripts.get(i).started = false;
						logger.error("Thread for script: " + script.name + " finished");
					}
				}
			});
		}
		scripts.get(i).started = true;
		scripts.get(i).thread.start();
	}

	/**
	 * Returns the metric to the AppDynamics Controller.
	 * @param 	metricName		Name of the Metric
	 * @param 	metricValue		Value of the Metric
	 * @param 	aggregation		Average OR Observation OR Sum
	 * @param 	timeRollup		Average OR Current OR Sum
	 * @param 	cluster			Collective OR Individual
	 */
	public void printMetric(String metricName, Object metricValue, String aggregation, String timeRollup, String cluster)
	{
		MetricWriter metricWriter = getMetricWriter(getMetricPrefix() + metricName, 
			aggregation,
			timeRollup,
			cluster
		);

		metricWriter.printMetric(String.valueOf(metricValue));
	}

	protected String getMetricPrefix()
	{
		return "Custom Metrics|Monitoring|Nagios|Status|";
	}

	@Override
	public TaskOutput execute(Map<String, String> args,
			TaskExecutionContext arg1) throws TaskExecutionException {
		project_folder = args.get("project_path");
		logger.error("BEGIN Execution");
		try {
			parseXML(project_folder + "/conf/scripts.xml");
		} catch (DocumentException e) {
			logger.error("Failed to parse XML." + e.toString());
		}
		while (true) 
		{
			try 
			{
				if (duration == 60) {
					print();
					duration = 0;
				}
				for (int i = 0; i < scripts.size(); i++) {
					if (!scripts.get(i).started) {
						logger.error("Executing script: " + scripts.get(i).name);
						executeScript(scripts.get(i), i);
					} 
					else if (scripts.get(i).started && scripts.get(i).period_passed > 10 && scripts.get(i).force_stop) {
						logger.error("Script running too long. Terminating: " + scripts.get(i).name);
						scripts.get(i).thread.interrupt();
						scripts.get(i).period_passed = 0;
					}
					scripts.get(i).period_passed++;
				}
				logger.error("Duration: " + duration);
				Thread.sleep(1000);
				duration++;
			}
			catch (IOException e) {
				logger.error(e);
				System.err.println(e);
			}
			catch (InterruptedException e) {
				logger.error(e);
				System.err.println(e);
			}
		}
	}
}
