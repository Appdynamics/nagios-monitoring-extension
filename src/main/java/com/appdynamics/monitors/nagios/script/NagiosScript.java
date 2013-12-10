package com.appdynamics.monitors.nagios.script;

import com.appdynamics.monitors.nagios.NagiosMonitor;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;

/**
 * @author
 */
public class NagiosScript implements Runnable {
    public static Logger LOG = Logger.getLogger(NagiosMonitor.class);

    private String id;
    private String name;
    private String path;
    private Integer period = 0;
    private String arguments;
    private Integer periodPassed = 0;
    private boolean started;
    private boolean runAll = false;
    private boolean forceStop = true;
    private Integer exitCode = 3; // UNKNOWN for Nagios.
    private Integer maxWaitTime;

    /**
     * @param id
     * @param name
     * @param path
     * @param period
     * @param arguments
     * @param periodPassed
     * @param started
     * @param runAll
     * @param forceStop
     * @param maxWaitTime
     */
    public NagiosScript(final String id, final String name, final String path, final Integer period, final String arguments, final Integer periodPassed, final boolean started,
                        final boolean runAll, final boolean forceStop, final Integer maxWaitTime) {
        this.id = id;
        this.name = name;
        this.path = path;
        this.period = period;
        this.arguments = arguments;
        this.periodPassed = periodPassed;
        this.started = started;
        this.runAll = runAll;
        this.forceStop = forceStop;
        this.maxWaitTime = maxWaitTime;
    }

    public NagiosScript() {
        super();
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {
        Runtime runtime = Runtime.getRuntime();
        Process process = null;
        try {
            process = runtime.exec(this.path + " " + this.arguments);
        } catch (IOException e) {
            LOG.error("Unable to run script: " + this.name, e);
        }

        CountDownLatch doneSignal = new CountDownLatch(1);
        Worker worker = new Worker(this.name, process, doneSignal);
        worker.start();
        try {
            doneSignal.await(this.maxWaitTime, TimeUnit.SECONDS);

            if (worker.getExitCode() != null) {

                if (worker.getExitCode() > 3) { //Nagios status codes are 0, 1, 2, 3
                    this.exitCode = 3;
                } else {
                    this.exitCode = worker.getExitCode();
                }
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Exit code for script : " + this.name + " is :" + this.exitCode);
                }
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("No Exit code for script : " + this.name);
                }
            }
        } catch (InterruptedException e) {
            worker.interrupt();
            Thread.currentThread().interrupt();
            LOG.error("Unable to run script: " + this.name, e);
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    /**
     * @return
     */
    public String getId() {
        return id;
    }

    /**
     * @param id
     */
    public void setId(final String id) {
        this.id = id;
    }

    /**
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * @return
     */
    public String getPath() {
        return path;
    }

    /**
     * @param path
     */
    public void setPath(final String path) {
        this.path = path;
    }

    /**
     * @return
     */
    public Integer getPeriod() {
        return period;
    }

    /**
     * @param period
     */
    public void setPeriod(final Integer period) {
        this.period = period;
    }

    /**
     * @return
     */
    public String getArguments() {
        return arguments;
    }

    /**
     * @param arguments
     */
    public void setArguments(final String arguments) {
        this.arguments = arguments;
    }

    /**
     * @return
     */
    public Integer getPeriodPassed() {
        return periodPassed;
    }

    /**
     * @param periodPassed
     */
    public void setPeriodPassed(final Integer periodPassed) {
        this.periodPassed = periodPassed;
    }

    /**
     * @return
     */
    public boolean isStarted() {
        return started;
    }

    /**
     * @param started
     */
    public void setStarted(final boolean started) {
        this.started = started;
    }

    /**
     * @return
     */
    public boolean isRunAll() {
        return runAll;
    }

    /**
     * @param runAll
     */
    public void setRunAll(final boolean runAll) {
        this.runAll = runAll;
    }

    /**
     * @return
     */
    public boolean isForceStop() {
        return forceStop;
    }

    /**
     * @param forceStop
     */
    public void setForceStop(final boolean forceStop) {
        this.forceStop = forceStop;
    }

    /**
     * @return
     */
    public Integer getExitCode() {
        return exitCode;
    }

    /**
     * @param exitCode
     */
    public void setExitCode(final Integer exitCode) {
        this.exitCode = exitCode;
    }

    /**
     * @return
     */
    public Integer getMaxWaitTime() {
        return maxWaitTime;
    }

    /**
     * @param maxWaitTime
     */
    public void setMaxWaitTime(Integer maxWaitTime) {
        if(maxWaitTime <= 0) {
            maxWaitTime = 5;  //If not specified, wait for 5 seconds
        }
        this.maxWaitTime = maxWaitTime;
    }

    class Worker extends Thread {
        private final Process process;
        private Integer exitCode;
        private final CountDownLatch countDownLatch;
        private final String scriptName;

        Worker(String scriptName, Process process, CountDownLatch countDownLatch) {
            this.process = process;
            this.countDownLatch = countDownLatch;
            this.scriptName = scriptName;
        }

        public void run() {
            try {
                exitCode = process.waitFor();
            } catch (InterruptedException ignore) {
                // NOOP
            } finally {
                countDownLatch.countDown();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Exit code from worker for script : " + this.scriptName + " is :" + this.exitCode);
                }
            }
        }

        public Integer getExitCode() {
            return exitCode;
        }
    }
}


