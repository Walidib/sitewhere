package com.sitewhere.groovy.device.communication;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.groovy.control.CompilationFailedException;

import com.sitewhere.SiteWhere;
import com.sitewhere.server.lifecycle.TenantLifecycleComponent;
import com.sitewhere.spi.SiteWhereException;
import com.sitewhere.spi.device.IDeviceAssignment;
import com.sitewhere.spi.device.IDeviceNestingContext;
import com.sitewhere.spi.device.command.IDeviceCommandExecution;
import com.sitewhere.spi.device.command.ISystemCommand;
import com.sitewhere.spi.device.communication.ICommandDestination;
import com.sitewhere.spi.device.communication.IOutboundCommandRouter;
import com.sitewhere.spi.server.lifecycle.LifecycleComponentType;

import groovy.lang.Binding;
import groovy.util.ResourceException;
import groovy.util.ScriptException;

/**
 * Implementation of {@link IOutboundCommandRouter} that uses Groovy scripts to
 * perform routing logic.
 * 
 * @author Derek
 */
public class GroovyCommandRouter extends TenantLifecycleComponent implements IOutboundCommandRouter {

    /** Static logger instance */
    private static Logger LOGGER = LogManager.getLogger();

    /** List of available command destinations */
    private List<ICommandDestination<?, ?>> commandDestinations;

    /** Path to script used for routing custom commands */
    private String scriptPath;

    public GroovyCommandRouter() {
	super(LifecycleComponentType.CommandRouter);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.sitewhere.spi.device.communication.IOutboundCommandRouter#initialize(
     * java.util.List)
     */
    @Override
    public void initialize(List<ICommandDestination<?, ?>> destinations) throws SiteWhereException {
	this.commandDestinations = destinations;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.device.communication.IOutboundCommandRouter#
     * routeCommand(com.sitewhere.spi.device.command.IDeviceCommandExecution,
     * com.sitewhere.spi.device.IDeviceNestingContext,
     * com.sitewhere.spi.device.IDeviceAssignment)
     */
    @Override
    public void routeCommand(IDeviceCommandExecution execution, IDeviceNestingContext nesting,
	    IDeviceAssignment assignment) throws SiteWhereException {
	route(execution, null, nesting, assignment);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.device.communication.IOutboundCommandRouter#
     * routeSystemCommand(com.sitewhere.spi.device.command.ISystemCommand,
     * com.sitewhere.spi.device.IDeviceNestingContext,
     * com.sitewhere.spi.device.IDeviceAssignment)
     */
    @Override
    public void routeSystemCommand(ISystemCommand command, IDeviceNestingContext nesting, IDeviceAssignment assignment)
	    throws SiteWhereException {
	route(null, command, nesting, assignment);
    }

    /**
     * Route either a custom command or system command based on logic determined
     * in a Groovy script.
     * 
     * @param execution
     * @param system
     * @param nesting
     * @param assignment
     * @throws SiteWhereException
     */
    @SuppressWarnings("deprecation")
    protected void route(IDeviceCommandExecution execution, ISystemCommand system, IDeviceNestingContext nesting,
	    IDeviceAssignment assignment) throws SiteWhereException {
	try {
	    Binding binding = new Binding();
	    binding.setVariable(IGroovyVariables.VAR_COMMAND_EXCUTION, execution);
	    binding.setVariable(IGroovyVariables.VAR_COMMAND_EXECUTION, execution);
	    binding.setVariable(IGroovyVariables.VAR_SYSTEM_COMMAND, system);
	    binding.setVariable(IGroovyVariables.VAR_NESTING_CONTEXT, nesting);
	    binding.setVariable(IGroovyVariables.VAR_ASSIGNMENT, assignment);
	    binding.setVariable(IGroovyVariables.VAR_LOGGER, LOGGER);
	    LOGGER.debug("About to route command using script '" + getScriptPath() + "'");
	    String target = (String) SiteWhere.getServer().getTenantGroovyConfiguration(getTenant())
		    .getGroovyScriptEngine().run(getScriptPath(), binding);
	    if (target != null) {
		for (ICommandDestination<?, ?> destination : getCommandDestinations()) {
		    if (target.equals(destination.getDestinationId())) {
			if (execution != null) {
			    destination.deliverCommand(execution, nesting, assignment);
			} else if (system != null) {
			    destination.deliverSystemCommand(system, nesting, assignment);
			}
		    }
		}
	    } else {
		LOGGER.warn("Groovy command router did not return a command destination id.");
	    }
	} catch (ResourceException e) {
	    throw new SiteWhereException("Unable to access Groovy command router script.", e);
	} catch (ScriptException e) {
	    throw new SiteWhereException("Unable to run Groovy command router script.", e);
	} catch (CompilationFailedException e) {
	    throw new SiteWhereException("Error compiling Groovy script.", e);
	} catch (Throwable e) {
	    throw new SiteWhereException("Unhandled exception in Groovy command router script.", e);
	}
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.server.lifecycle.ILifecycleComponent#getLogger()
     */
    @Override
    public Logger getLogger() {
	return LOGGER;
    }

    public String getScriptPath() {
	return scriptPath;
    }

    public void setScriptPath(String scriptPath) {
	this.scriptPath = scriptPath;
    }

    public List<ICommandDestination<?, ?>> getCommandDestinations() {
	return commandDestinations;
    }

    public void setCommandDestinations(List<ICommandDestination<?, ?>> commandDestinations) {
	this.commandDestinations = commandDestinations;
    }
}