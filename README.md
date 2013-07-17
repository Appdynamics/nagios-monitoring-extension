# AppDynamics Nagios Monitoring Extension

##Use Case

Nagios is an open source computer system, network, and infrastructure monitoring software application.

The Nagios monitoring extension executes various Nagios scripts at user configurable
intervals and display their results in the AppDynamics Metric Browser.


##Installation
1. Run 'ant package' from the pingdom-monitoring-extension directory
2. Download the file NagiosMonitor.zip found in the 'dist' directory into \<machineagent install dir\>/monitors/
3. Unzip the downloaded file and cd into NagiosMonitor
4. Open the monitor.xml file and edit the project path to the NagiosMonitor/scripts directory that was just created
5. Open the scripts.xml file and add the appropriate scripts such as those in [the scripts.xml example](https://github.com/Appdynamics/nagios-monitoring-extension/blob/master/README.md#scriptsxml).
6. Restart the Machine Agent.
7. In the AppDynamics Metric Browser, look for: Application Infrastructure Performance  | \<Tier\> | Custom Metrics | Monitoring | Nagios | Status | \<script name\>

##Nagios Script Return Codes

The Nagios scripts should return the following codes:

| Code | Value |
| --- | --- |
| 0 | OK |
| 1 | WARNING |
| 2 | CRITICAL |
| 3 | UNKNOWN |

**Note**: The Nagios scripts must have read access by the Machine Agent for the monitoring extension to execute them.

##Files/Folders Included:

<table><tbody>
<tr>
<th align = 'left'> Directory/File </th>
<th align = 'left'> Description </th>
</tr>
<tr>
<td class='confluenceTd'> conf </td>
<td class='confluenceTd'> Contains the monitor.xml </td>
</tr>
<tr>
<td class='confluenceTd'> lib </td>
<td class='confluenceTd'> Contains third-party project references </td>
</tr>
<tr>
<td class='confluenceTd'> src </td>
<td class='confluenceTd'> Contains source code to Nagios Monitoring Extension </td>
</tr>
<tr>
<td class='confluenceTd'> dist </td>
<td class='confluenceTd'> Only obtained when using ant. Run 'ant build' to get binaries. Run 'ant package' to get the distributable .zip file </td>
</tr>
<tr>
<td class='confluenceTd'> build.xml </td>
<td class='confluenceTd'> Ant build script to package the project (required only if changing Java code) </td>
</tr>
</tbody>
</table>

##XML Examples

###  monitor.xml


| Param | Description |
| ----- | ----- |
| project\_path | Location of the Nagios script root directory |

~~~~
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
                        <classpath>NagiosMonitor.jar;lib/dom4j-2.0.0-ALPHA-2.jar</classpath>
                        <impl-class>com.appdynamics.monitors.nagios.NagiosMonitor</impl-class>
                </java-task>
                <task-arguments>
                        <argument name="project_path" is-required="true" default-value="/path/to/nagios/project"/>
                </task-arguments>
        </monitor-run-task>
</monitor>
~~~~

###scripts.xml

| Param | Description |
| ---- | ---- |
| \<name\> | Name of the metric being returned by the shell |
| \<path\>  | Path to the shell file |
| \<period\>  | (seconds) - Delay between consecutive  calls. Collect the metric every certain period |
| \<task-arguments\> | Arguments that will be passed to the script |


~~~~
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
~~~~

##Metric Browser


![](http://appsphere.appdynamics.com/t5/image/serverpage/image-id/59i17B26F44069944E8/image-size/original?v=mpbl-1&px=-1)



##Contributing

Always feel free to fork and contribute any changes directly via GitHub.


##Support

For any support questions, please contact ace@appdynamics.com.
