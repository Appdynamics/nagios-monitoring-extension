
package com.appdynamics.monitors.nagios.script;

import java.io.IOException;

import org.apache.log4j.Logger;

import com.appdynamics.monitors.nagios.NagiosMonitor;

/**
 * @author
 *
 */
public class NagiosScript implements Runnable
{
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

    /**
     * @param id
     * @param name
     * @param path
     * @param period
     * @param arguments
     * @param periodPassed
     * @param started
     * @param thread
     * @param runAll
     * @param forceStop
     */
    public NagiosScript(
        final String id,
        final String name,
        final String path,
        final Integer period,
        final String arguments,
        final Integer periodPassed,
        final boolean started,
        final boolean runAll,
        final boolean forceStop)
    {
        this.id = id;
        this.name = name;
        this.path = path;
        this.period = period;
        this.arguments = arguments;
        this.periodPassed = periodPassed;
        this.started = started;
        this.runAll = runAll;
        this.forceStop = forceStop;
    }

    public NagiosScript()
    {
        super();
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run()
    {
        try {
            this.exitCode = Runtime.getRuntime().exec(this.path + " " + this.arguments).waitFor();
        }
        catch (IOException e) {
            LOG.error("Unable to run script: " + this.name, e);
        }
        catch (InterruptedException e) {
            LOG.error("Unable to run script: " + this.name, e);
        }

        LOG.info("Exit code for script: " + this.name + " : " + this.exitCode);
    }

    /**
     * @return
     */
    public String getId()
    {
        return id;
    }

    /**
     * @param id
     */
    public void setId(final String id)
    {
        this.id = id;
    }

    /**
     * @return
     */
    public String getName()
    {
        return name;
    }

    /**
     * @param name
     */
    public void setName(final String name)
    {
        this.name = name;
    }

    /**
     * @return
     */
    public String getPath()
    {
        return path;
    }

    /**
     * @param path
     */
    public void setPath(final String path)
    {
        this.path = path;
    }

    /**
     * @return
     */
    public Integer getPeriod()
    {
        return period;
    }

    /**
     * @param period
     */
    public void setPeriod(final Integer period)
    {
        this.period = period;
    }

    /**
     * @return
     */
    public String getArguments()
    {
        return arguments;
    }

    /**
     * @param arguments
     */
    public void setArguments(final String arguments)
    {
        this.arguments = arguments;
    }

    /**
     * @return
     */
    public Integer getPeriodPassed()
    {
        return periodPassed;
    }

    /**
     * @param periodPassed
     */
    public void setPeriodPassed(final Integer periodPassed)
    {
        this.periodPassed = periodPassed;
    }

    /**
     * @return
     */
    public boolean isStarted()
    {
        return started;
    }

    /**
     * @param started
     */
    public void setStarted(final boolean started)
    {
        this.started = started;
    }

    /**
     * @return
     */
    public boolean isRunAll()
    {
        return runAll;
    }

    /**
     * @param runAll
     */
    public void setRunAll(final boolean runAll)
    {
        this.runAll = runAll;
    }

    /**
     * @return
     */
    public boolean isForceStop()
    {
        return forceStop;
    }

    /**
     * @param forceStop
     */
    public void setForceStop(final boolean forceStop)
    {
        this.forceStop = forceStop;
    }

    /**
     * @return
     */
    public Integer getExitCode()
    {
        return exitCode;
    }

    /**
     * @param exitCode
     */
    public void setExitCode(final Integer exitCode)
    {
        this.exitCode = exitCode;
    }

}
