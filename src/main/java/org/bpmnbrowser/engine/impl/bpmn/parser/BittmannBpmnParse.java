package org.bpmnbrowser.engine.impl.bpmn.parser;
import java.util.HashMap;

import org.activiti.engine.impl.bpmn.parser.BpmnParse;
import org.activiti.engine.impl.bpmn.parser.BpmnParser;
import org.activiti.engine.impl.pvm.process.TransitionImpl;


public class BittmannBpmnParse extends BpmnParse {

	public BittmannBpmnParse(BpmnParser parser) {
		super(parser);
		
	}
	
	protected void transformProcessDefinitions() {
	    sequenceFlows = new HashMap<String, TransitionImpl>();
	    /*
	     * for image generator, executability does not matter.
	    for (Process process : bpmnModel.getProcesses()) {
	      if (process.isExecutable()) {
	        bpmnParserHandlers.parseElement(this, process);
	      }
	    }*/

	    if (!processDefinitions.isEmpty()) {
	      processDI();
	    }
	  }

}
