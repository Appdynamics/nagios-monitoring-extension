# AppDynamics Nagios Monitoring Extension

* [Use Case](nagios-readme.md#use-case)
* [Installation](nagios-readme.md#nstallation)
* [Nagios Script Return Codes](nagios-readme.md#nagios-script-return-codes)
* [Dirctory/File Information](nagios-readme.md#dirctoryfile-information)
* [XML Examples](nagios-readme.md#xml-examples)
    - [monitor.xml](nagios-readme.md#monitorxml)
    - [scripts.xml](nagios-readme.md#scripts-xml)
* [Metric Browser](nagios-readme.md#metric-browser)
* [Contributing](nagios-readme.md#contributing)

##Use Case

Nagios is an open source computer system, network, and infrastructure monitoring software application.

The Nagios monitoring extension executes various Nagios scripts at user configurable
intervals and display their results in the AppDynamics Metric Browser.


##Installation
1.  In <machine-agent-home>/monitors create a subdirectory for the Nagios monitoring extension.
2.  Copy the contents in the 'dist' directory to the directory created in step 1.
3.  Edit the monitor.xml file and add the project path to the directory created in step 1.
4.  Edit the nagios.xml file and add the appropriate scripts such as those in [XML Examples](nagios-readme.md#xml-examples).
5.  Restart the Machine Agent.
6.  Metrics will be uploaded to "Custom Metrics|Monitoring|Nagios|Status|\<script name\>"

##Nagios Script Return Codes

The Nagios scripts should return the following codes:

| Code | Value |
| --- | --- |
| 0 | OK |
| 1 | WARNING |
| 2 | CRITICAL |
| 3 | UNKNOWN |

**Note**: The Nagios scripts must have read access by the Machine Agent for the monitoring extension to execute them.

##Dirctory/File Information

| Directory/File | Description|
| --- | --- |
|Main Java File|src/com/appdynamics/monitors/nagios/NagiosMonitor.java - This file contains source to execute and print results to the controller.|
| bin | Contains class file |
| conf | Contains the monitor.xml and scripts.xml |
| lib | Contains Third-Party project references |
| src | Contains source code to Nagios Custom Monitor |
| dist | Contains the distribution package (monitor.xml and jar) |
| build.xml | Ant build script to package the project (only required if changing java code) |

##XML Examples

###  monitor.xml


| Param | Description |
| ----- | ----- |
| project\_path | Location of the Nagios script root directory |



    <monitor>
	    <name>NagiosMonitor</name>
	    <type>managed</type>
	    <description>Nagios server monitor</description>
	    <monitor-configuration></monitor-configuration>
	    <monitor-run-task>
		    <execution-style>continuous</execution-style>
		    <name>Nagios Monitor Run Task</name>
		    <display-name>Nagios Monitor Task</display-name>
		    <description>Nagios Monitor Task</description>
		    <type>java</type>
		    <java-task>
			    <classpath>nagios.jar;lib/dom4j-2.0.0-ALPHA-2.jar</classpath>
			    <impl-class>com.appdynamics.monitors.nagios.NagiosMonitor</impl-class>
		    </java-task>
		    <task-arguments>
			    <argument name="project_path" is-required="true" default-value="/path/to/nagios/project"/>
		    </task-arguments>
	    </monitor-run-task>
    </monitor>

###scripts.xml

| Param | Description |
| ---- | ---- |
| \<name\> | Name of the metric being returned by the shell |
| \<path\>  | Path to the shell file |
| \<period\>  | (seconds) - Delay between consecutive  calls. Collect the metric every certain period |
| \<task-arguments\> | Arguments that will be passed to the script |



    <nagios-monitor>
	<script>
	    <name>MySQL Health Check</name>
	    <path>~/scripts/mysql.sh</path>
	    <period>5</period>
	    <task-arguments>-w 1 -c 2</task-arguments>
	</script>
	<script>
	    <name>Oracle DB Health Check</name>
	    <path>~/scripts/oracle.sh</path>
	    <period>5</period>
	</script>
	    .
	    .
	    .
    </nagios-monitor>

##Metric Browser


![](images/nagios.png)



##Contributing

Always feel free to fork and contribute any changes directly via GitHub.


##Support

For any support questions, please contact ace@appdynamics.com.
