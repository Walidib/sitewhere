package com.sitewhere.spi.server.lifecycle;

import java.util.ArrayDeque;
import java.util.Deque;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sitewhere.rest.model.monitoring.ProgressMessage;
import com.sitewhere.spi.SiteWhereException;

/**
 * Common logic used in lifecycle progress monitoring.
 * 
 * @author Derek
 */
public class LifecycleProgressUtils {

    /** Static logger instance */
    @SuppressWarnings("unused")
    private static Logger LOGGER = LogManager.getLogger();

    /**
     * Common logic for starting an operation on an
     * {@link ILifecycleProgressMonitor}.
     * 
     * @param monitor
     * @param operation
     * @throws SiteWhereException
     */
    public static void startProgressOperation(ILifecycleProgressMonitor monitor, String operation)
	    throws SiteWhereException {
	ILifecycleProgressContext context = monitor.getContextStack().peek();
	if (context == null) {
	    throw new SiteWhereException("Unable to start operation. No context available.");
	}
	int newIndex = context.getCurrentOperationIndex() + 1;
	if (newIndex > context.getOperationCount()) {
	    throw new SiteWhereException(
		    "Unable to start operation. Index will exceed expected operation count. Operation was: "
			    + operation);
	}
	context.setCurrentOperationIndex(newIndex);
	context.setCurrentOperationMessage(operation);
    }

    /**
     * Finish the currently executing progress operation.
     * 
     * @param monitor
     * @throws SiteWhereException
     */
    public static void finishProgressOperation(ILifecycleProgressMonitor monitor) throws SiteWhereException {
	ILifecycleProgressContext context = monitor.getContextStack().peek();
	if (context == null) {
	    throw new SiteWhereException("Unable to finish operation. No context available.");
	}

	// Report progress based on new operation.
	String task = monitor.getContextStack().getLast().getCurrentOperationMessage();
	String current = monitor.getContextStack().getFirst().getCurrentOperationMessage();
	Deque<ILifecycleProgressContext> queue = new ArrayDeque<>(monitor.getContextStack());
	double progress = computeSubprogressFor(queue, 100.0);
	monitor.reportProgress(new ProgressMessage(task, progress, current));
    }

    /**
     * Recursively computes progress based on nested contexts.
     * 
     * @param stack
     * @param parentChunk
     * @return
     */
    protected static double computeSubprogressFor(Deque<ILifecycleProgressContext> stack, double current) {
	if (stack.isEmpty()) {
	    return current;
	}
	ILifecycleProgressContext context = stack.removeLast();
	double opIndex = (double) context.getCurrentOperationIndex();
	double opCount = (double) context.getOperationCount();

	double finished = Math.floor((opIndex - 1.0) / opCount * current);
	double working = Math.floor(1.0 / opCount * current);

	return finished + computeSubprogressFor(stack, working);
    }
}