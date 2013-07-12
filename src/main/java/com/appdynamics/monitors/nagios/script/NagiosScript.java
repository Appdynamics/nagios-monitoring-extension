package com.appdynamics.monitors.nagios.script;

public class NagiosScript {
	public String id;
	public String name;
	public String path;
	public Integer period = 0;
	public String arguments;
	public Integer period_passed = 0;
	public boolean started;
	public Thread thread;
	public boolean run_all = false;
	public boolean force_stop = true;
}
