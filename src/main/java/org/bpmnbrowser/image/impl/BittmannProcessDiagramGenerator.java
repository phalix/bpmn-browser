package org.bpmnbrowser.image.impl;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.activiti.bpmn.model.Activity;
import org.activiti.bpmn.model.Artifact;
import org.activiti.bpmn.model.Association;
import org.activiti.bpmn.model.AssociationDirection;
import org.activiti.bpmn.model.BaseElement;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.CallActivity;
import org.activiti.bpmn.model.DataObject;
import org.activiti.bpmn.model.DataStore;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.FlowElementsContainer;
import org.activiti.bpmn.model.FlowNode;
import org.activiti.bpmn.model.Gateway;
import org.activiti.bpmn.model.GraphicInfo;
import org.activiti.bpmn.model.Import;
import org.activiti.bpmn.model.Interface;
import org.activiti.bpmn.model.Lane;
import org.activiti.bpmn.model.Message;
import org.activiti.bpmn.model.MessageFlow;
import org.activiti.bpmn.model.MultiInstanceLoopCharacteristics;
import org.activiti.bpmn.model.Pool;
import org.activiti.bpmn.model.Process;
import org.activiti.bpmn.model.Resource;
import org.activiti.bpmn.model.SequenceFlow;
import org.activiti.bpmn.model.Signal;
import org.activiti.bpmn.model.SubProcess;
import org.activiti.image.ProcessDiagramGenerator;
import org.activiti.image.impl.DefaultProcessDiagramCanvas;
import org.activiti.image.impl.DefaultProcessDiagramGenerator;
/**
 * @author Sebastian Bittmann
 */
public class BittmannProcessDiagramGenerator extends DefaultProcessDiagramGenerator implements ProcessDiagramGenerator {

	public BittmannProcessDiagramGenerator(final double scaleFactor) {
		super(scaleFactor);
		artifactDrawInstructions.put(Association.class, new ArtifactDrawInstruction() {

			public void draw(DefaultProcessDiagramCanvas processDiagramCanvas, BpmnModel bpmnModel, Artifact artifact) {
				Association association = (Association) artifact;
				String sourceRef = association.getSourceRef();
				String targetRef = association.getTargetRef();
				
				// source and target can be instance of FlowElement or Artifact
				BaseElement sourceElement = bpmnModel.getFlowElement(sourceRef);
				BaseElement targetElement = bpmnModel.getFlowElement(targetRef);
				if (sourceElement == null) {
					sourceElement = bpmnModel.getArtifact(sourceRef);
				}
				if (targetElement == null) {
					targetElement = bpmnModel.getArtifact(targetRef);
				}
				
				if(targetElement != null && sourceElement != null){
					List<GraphicInfo> graphicInfoList = bpmnModel.getFlowLocationGraphicInfo(artifact.getId());
					graphicInfoList = connectionPerfectionizer(processDiagramCanvas, bpmnModel, sourceElement, targetElement, graphicInfoList);
					int xPoints[]= new int[graphicInfoList.size()];
					int yPoints[]= new int[graphicInfoList.size()];
					for (int i=1; i<graphicInfoList.size(); i++) {
						GraphicInfo graphicInfo = graphicInfoList.get(i);
						GraphicInfo previousGraphicInfo = graphicInfoList.get(i-1);

						if (i == 1) {
							xPoints[0] = (int) previousGraphicInfo.getX();
							yPoints[0] = (int) previousGraphicInfo.getY();
						}
						xPoints[i] = (int) graphicInfo.getX();
						yPoints[i] = (int) graphicInfo.getY();
					}

					AssociationDirection associationDirection = association.getAssociationDirection();
					processDiagramCanvas.drawAssociation(xPoints, yPoints, associationDirection, false, scaleFactor);
				}else{
					System.err.println("Source or Target Element not found.");
				}
				
			}
		});


	}

	protected BittmannProcessDiagramCanvas generateProcessDiagram(BpmnModel bpmnModel, String imageType, 
			List<String> highLightedActivities, List<String> highLightedFlows,
			String activityFontName, String labelFontName, ClassLoader customClassLoader, double scaleFactor) {

		prepareBpmnModel(bpmnModel);

		BittmannProcessDiagramCanvas processDiagramCanvas = (BittmannProcessDiagramCanvas) initProcessDiagramCanvas(bpmnModel, imageType, activityFontName, labelFontName, customClassLoader);

		// Draw pool shape, if process is participant in collaboration
		for (Pool pool : bpmnModel.getPools()) {
			GraphicInfo graphicInfo = bpmnModel.getGraphicInfo(pool.getId());
			//TODO: Pools that have no activities should have a horizontal label!
			if(graphicInfo!=null){
				processDiagramCanvas.drawPoolOrLane(pool.getName(), graphicInfo);	
			}
			
		}
		
		for(DataStore dataStore : bpmnModel.getDataStores().values()){
			
			GraphicInfo graphicInfo = bpmnModel.getGraphicInfo(dataStore.getId());
			if(graphicInfo != null){
				//TODO: as soon as I have testfiles available needs to be completed.
				drawDataStore(processDiagramCanvas,dataStore.getName(),graphicInfo);
			}
		}
		
		
		// Draw lanes
		List<Process> processes = bpmnModel.getProcesses();
		for (int i = 0; i < processes.size(); i++) {
			Process process = processes.get(i);
			for (Lane lane : process.getLanes()) {
				GraphicInfo graphicInfo = bpmnModel.getGraphicInfo(lane.getId());
				if(graphicInfo!=null)
					processDiagramCanvas.drawPoolOrLane(lane.getName(), graphicInfo);
			}
			// Draw Data Objects
			for (DataObject dataObject : process.getDataObjects()) {
				GraphicInfo graphicInfo = bpmnModel.getGraphicInfo(dataObject.getId());
				if(graphicInfo!=null)
					//TODO: as soon as I have testfiles available needs to be completed.
					drawDataObject(processDiagramCanvas,dataObject.getName(),graphicInfo);
				
			}
			
			
			// Draw activities and their sequence-flows
			for (FlowNode flowNode : process.findFlowElementsOfType(FlowNode.class)) {
				drawActivity(processDiagramCanvas, bpmnModel, flowNode, highLightedActivities, highLightedFlows, scaleFactor);
			}
			for (FlowNode flowNode : process.findFlowElementsOfType(FlowNode.class)) {	
				drawSequenceFlows(processDiagramCanvas, bpmnModel, flowNode, highLightedActivities, highLightedFlows, scaleFactor);
			}
			// Draw artifacts: Text Annoation and Corresponding Associations
			for (Artifact artifact : process.getArtifacts()) { 
				drawArtifact(processDiagramCanvas, bpmnModel, artifact);
			}
			
			
			
			
		}
		
		Collection<Message> messages = bpmnModel.getMessages();
		for(Message message:messages){
			System.out.println("Message:"+message.getName());
		}
		
		
		Collection<Resource> resources = bpmnModel.getResources();
		for(Resource resource : resources){
			System.out.println("Resource: "+resource.getName());
		}
		
		List<Interface> interfaces = bpmnModel.getInterfaces();
		for(Interface interface_:interfaces){
			System.out.println("Interface: "+interface_.getName());
			
		}
		List<Import> imports = bpmnModel.getImports();
		for(Import import_:imports){
			System.out.println("Imports: "+import_.getId());
			//drawArtifact(processDiagramCanvas, bpmnModel, artifact);
		}
		List<Artifact> globalArtifacts = bpmnModel.getGlobalArtifacts();
		for(Artifact artifact:globalArtifacts){
			System.out.println("GlobalArtifact: "+artifact.getId());
			drawArtifact(processDiagramCanvas, bpmnModel, artifact);
		}
		
		Collection<Signal> signals = bpmnModel.getSignals();
		for(Signal signal: signals){
			System.out.println("Signal: "+signal.getName());
		}
		
		Map<String, MessageFlow> messageFlows = bpmnModel.getMessageFlows();
		for(MessageFlow mf : messageFlows.values()){
			drawMessageFlow(processDiagramCanvas,bpmnModel,mf,scaleFactor);
		}
		
		
		return processDiagramCanvas;
	}
	private void drawDataObject(BittmannProcessDiagramCanvas processDiagramCanvas,String name, GraphicInfo graphicInfo) {
		processDiagramCanvas.drawDataObject(name,graphicInfo,1.0f);
		
	}

	private void drawDataStore(BittmannProcessDiagramCanvas processDiagramCanvas,String name, GraphicInfo graphicInfo) {
		processDiagramCanvas.drawDataStore(name,graphicInfo,1.0f);
	}

	private void drawMessageFlow(BittmannProcessDiagramCanvas processDiagramCanvas, BpmnModel bpmnModel, MessageFlow messageFlow,
			double scaleFactor) {
		// Outgoing transitions of activity
	    
	      
	      boolean isDefault = false;
	      
	      String sourceRef = messageFlow.getSourceRef();
	      String targetRef = messageFlow.getTargetRef();
	      FlowElement sourceElement = bpmnModel.getFlowElement(sourceRef);
	      FlowElement targetElement = bpmnModel.getFlowElement(targetRef);
	      List<GraphicInfo> graphicInfoList = bpmnModel.getFlowLocationGraphicInfo(messageFlow.getId());
	      if (graphicInfoList != null && graphicInfoList.size() > 0) {
	        if(sourceElement != null && targetElement != null)
	    	  graphicInfoList = connectionPerfectionizer(processDiagramCanvas, bpmnModel, sourceElement, targetElement, graphicInfoList);
	        
	    	  int xPoints[]= new int[graphicInfoList.size()];
	        int yPoints[]= new int[graphicInfoList.size()];
	        
	        for (int i=1; i<graphicInfoList.size(); i++) {
	          GraphicInfo graphicInfo = graphicInfoList.get(i);
	          GraphicInfo previousGraphicInfo = graphicInfoList.get(i-1);
	          
	          if (i == 1) {
	            xPoints[0] = (int) previousGraphicInfo.getX();
	            yPoints[0] = (int) previousGraphicInfo.getY();
	          }
	          xPoints[i] = (int) graphicInfo.getX();
	          yPoints[i] = (int) graphicInfo.getY();
	          
	        }
	  
	        processDiagramCanvas.drawMessageflow(xPoints, yPoints, isDefault, scaleFactor);
	        
	  
	        // Draw messageflow label
	        GraphicInfo labelGraphicInfo = bpmnModel.getLabelGraphicInfo(messageFlow.getId());
	        if (labelGraphicInfo != null) {
	          processDiagramCanvas.drawLabel(messageFlow.getName(), labelGraphicInfo, false);
	        }
	      
	    }
		
	}

	protected static BittmannProcessDiagramCanvas initProcessDiagramCanvas(BpmnModel bpmnModel, String imageType,
			String activityFontName, String labelFontName, ClassLoader customClassLoader) {

		// We need to calculate maximum values to know how big the image will be in its entirety
		double minX = Double.MAX_VALUE;
		double maxX = 0;
		double minY = Double.MAX_VALUE;
		double maxY = 0;

		for (Pool pool : bpmnModel.getPools()) {
			GraphicInfo graphicInfo = bpmnModel.getGraphicInfo(pool.getId());
			minX = graphicInfo.getX();
			maxX = graphicInfo.getX() + graphicInfo.getWidth();
			minY = graphicInfo.getY();
			maxY = graphicInfo.getY() + graphicInfo.getHeight();
		}

		List<FlowNode> flowNodes = gatherAllFlowNodes(bpmnModel);
		for (FlowNode flowNode : flowNodes) {

			GraphicInfo flowNodeGraphicInfo = bpmnModel.getGraphicInfo(flowNode.getId());

			// width
			if (flowNodeGraphicInfo.getX() + flowNodeGraphicInfo.getWidth() > maxX) {
				maxX = flowNodeGraphicInfo.getX() + flowNodeGraphicInfo.getWidth();
			}
			if (flowNodeGraphicInfo.getX() < minX) {
				minX = flowNodeGraphicInfo.getX();
			}
			// height
			if (flowNodeGraphicInfo.getY() + flowNodeGraphicInfo.getHeight() > maxY) {
				maxY = flowNodeGraphicInfo.getY() + flowNodeGraphicInfo.getHeight();
			}
			if (flowNodeGraphicInfo.getY() < minY) {
				minY = flowNodeGraphicInfo.getY();
			}

			for (SequenceFlow sequenceFlow : flowNode.getOutgoingFlows()) {
				List<GraphicInfo> graphicInfoList = bpmnModel.getFlowLocationGraphicInfo(sequenceFlow.getId());
				if (graphicInfoList != null) {
					for (GraphicInfo graphicInfo : graphicInfoList) {
						// width
						if (graphicInfo.getX() > maxX) {
							maxX = graphicInfo.getX();
						}
						if (graphicInfo.getX() < minX) {
							minX = graphicInfo.getX();
						}
						// height
						if (graphicInfo.getY() > maxY) {
							maxY = graphicInfo.getY();
						}
						if (graphicInfo.getY()< minY) {
							minY = graphicInfo.getY();
						}
					}
				}
			}
		}

		List<Artifact> artifacts = gatherAllArtifacts(bpmnModel);
		for (Artifact artifact : artifacts) {

			GraphicInfo artifactGraphicInfo = bpmnModel.getGraphicInfo(artifact.getId());

			if (artifactGraphicInfo != null) {
				// width
				if (artifactGraphicInfo.getX() + artifactGraphicInfo.getWidth() > maxX) {
					maxX = artifactGraphicInfo.getX() + artifactGraphicInfo.getWidth();
				}
				if (artifactGraphicInfo.getX() < minX) {
					minX = artifactGraphicInfo.getX();
				}
				// height
				if (artifactGraphicInfo.getY() + artifactGraphicInfo.getHeight() > maxY) {
					maxY = artifactGraphicInfo.getY() + artifactGraphicInfo.getHeight();
				}
				if (artifactGraphicInfo.getY() < minY) {
					minY = artifactGraphicInfo.getY();
				}
			}

			List<GraphicInfo> graphicInfoList = bpmnModel.getFlowLocationGraphicInfo(artifact.getId());
			if (graphicInfoList != null) {
				for (GraphicInfo graphicInfo : graphicInfoList) {
					// width
					if (graphicInfo.getX() > maxX) {
						maxX = graphicInfo.getX();
					}
					if (graphicInfo.getX() < minX) {
						minX = graphicInfo.getX();
					}
					// height
					if (graphicInfo.getY() > maxY) {
						maxY = graphicInfo.getY();
					}
					if (graphicInfo.getY()< minY) {
						minY = graphicInfo.getY();
					}
				}
			}
		}

		int nrOfLanes = 0;
		for (Process process : bpmnModel.getProcesses()) {
			for (Lane l : process.getLanes()) {

				nrOfLanes++;

				GraphicInfo graphicInfo = bpmnModel.getGraphicInfo(l.getId());
				// // width
				if (graphicInfo.getX() + graphicInfo.getWidth() > maxX) {
					maxX = graphicInfo.getX() + graphicInfo.getWidth();
				}
				if (graphicInfo.getX() < minX) {
					minX = graphicInfo.getX();
				}
				// height
				if (graphicInfo.getY() + graphicInfo.getHeight() > maxY) {
					maxY = graphicInfo.getY() + graphicInfo.getHeight();
				}
				if (graphicInfo.getY() < minY) {
					minY = graphicInfo.getY();
				}
			}
		}

		// Special case, see https://activiti.atlassian.net/browse/ACT-1431
		if (flowNodes.isEmpty() && bpmnModel.getPools().isEmpty() && nrOfLanes == 0) {
			// Nothing to show
			minX = 0;
			minY = 0;
		}

		return new BittmannProcessDiagramCanvas((int) maxX + 10,(int) maxY + 10, (int) minX, (int) minY, 
				imageType, activityFontName, labelFontName, customClassLoader);
	}

	protected void drawActivity(DefaultProcessDiagramCanvas processDiagramCanvas, BpmnModel bpmnModel, 
			FlowNode flowNode, List<String> highLightedActivities, List<String> highLightedFlows, double scaleFactor) {

		ActivityDrawInstruction drawInstruction = activityDrawInstructions.get(flowNode.getClass());
		if (drawInstruction != null) {
			// Gather info on the collapsed marker
			GraphicInfo graphicInfo = bpmnModel.getGraphicInfo(flowNode.getId()); 
			graphicInfo.setElement(flowNode);
						
			drawInstruction.draw(processDiagramCanvas, bpmnModel, flowNode);

			// Gather info on the multi instance marker
			boolean multiInstanceSequential = false, multiInstanceParallel = false, collapsed = false;
			if (flowNode instanceof Activity) {
				Activity activity = (Activity) flowNode;
				MultiInstanceLoopCharacteristics multiInstanceLoopCharacteristics = activity.getLoopCharacteristics();
				if (multiInstanceLoopCharacteristics != null) {
					multiInstanceSequential = multiInstanceLoopCharacteristics.isSequential();
					multiInstanceParallel = !multiInstanceSequential;
				}
			}

			
			
			if (flowNode instanceof SubProcess) {
				collapsed = graphicInfo.getExpanded() != null && !graphicInfo.getExpanded();
			} else if (flowNode instanceof CallActivity) {
				collapsed = true;
			}

			if (scaleFactor == 1.0) {
				// Actually draw the markers
				processDiagramCanvas.drawActivityMarkers((int) graphicInfo.getX(), (int) graphicInfo.getY(),(int) graphicInfo.getWidth(), (int) graphicInfo.getHeight(), 
						multiInstanceSequential, multiInstanceParallel, collapsed);
			}

		}else{
			System.out.println("Not supported: "+flowNode.getClass()+" "+flowNode.getId());
		}

		// Nested elements
		if (flowNode instanceof FlowElementsContainer) {
			for (FlowElement nestedFlowElement : ((FlowElementsContainer) flowNode).getFlowElements()) {
				if (nestedFlowElement instanceof FlowNode) {
					drawActivity(processDiagramCanvas, bpmnModel, (FlowNode) nestedFlowElement, 
							highLightedActivities, highLightedFlows, scaleFactor);
				}
			}
		}
	}
	protected void drawSequenceFlows(DefaultProcessDiagramCanvas processDiagramCanvas, BpmnModel bpmnModel, 
			FlowNode flowNode, List<String> highLightedActivities, List<String> highLightedFlows, double scaleFactor) {
		
		
		
		// Outgoing transitions of activity
		for (SequenceFlow sequenceFlow : flowNode.getOutgoingFlows()) {
			boolean highLighted = (highLightedFlows.contains(sequenceFlow.getId()));
			String defaultFlow = null;
			if (flowNode instanceof Activity) {
				defaultFlow = ((Activity) flowNode).getDefaultFlow();
			} else if (flowNode instanceof Gateway) {
				defaultFlow = ((Gateway) flowNode).getDefaultFlow();
			}

			boolean isDefault = false;
			if (defaultFlow != null && defaultFlow.equalsIgnoreCase(sequenceFlow.getId())) {
				isDefault = true;
			}
			boolean drawConditionalIndicator = sequenceFlow.getConditionExpression() != null && !(flowNode instanceof Gateway);

			String sourceRef = sequenceFlow.getSourceRef();
			String targetRef = sequenceFlow.getTargetRef();
			FlowElement sourceElement = bpmnModel.getFlowElement(sourceRef);
			FlowElement targetElement = bpmnModel.getFlowElement(targetRef);
			List<GraphicInfo> graphicInfoList = bpmnModel.getFlowLocationGraphicInfo(sequenceFlow.getId());
			if (graphicInfoList != null && graphicInfoList.size() > 0) {
				graphicInfoList = connectionPerfectionizer(processDiagramCanvas, bpmnModel, sourceElement, targetElement, graphicInfoList);
				int xPoints[]= new int[graphicInfoList.size()];
				int yPoints[]= new int[graphicInfoList.size()];

				for (int i=1; i<graphicInfoList.size(); i++) {
					GraphicInfo graphicInfo = graphicInfoList.get(i);
					GraphicInfo previousGraphicInfo = graphicInfoList.get(i-1);

					if (i == 1) {
						xPoints[0] = (int) previousGraphicInfo.getX();
						yPoints[0] = (int) previousGraphicInfo.getY();
					}
					xPoints[i] = (int) graphicInfo.getX();
					yPoints[i] = (int) graphicInfo.getY();

				}

				processDiagramCanvas.drawSequenceflow(xPoints, yPoints, drawConditionalIndicator, isDefault, highLighted, scaleFactor);

				// Draw sequenceflow label
				GraphicInfo labelGraphicInfo = bpmnModel.getLabelGraphicInfo(sequenceFlow.getId());
				if (labelGraphicInfo != null) {
					processDiagramCanvas.drawLabel(sequenceFlow.getName(), labelGraphicInfo, false);
				}
			}
		}
	}


	protected void drawArtifact(BittmannProcessDiagramCanvas processDiagramCanvas, BpmnModel bpmnModel, Artifact artifact) {

		
		
		Class<? extends Artifact> artifactClass = artifact.getClass();
		ArtifactDrawInstruction drawInstruction = artifactDrawInstructions.get(artifactClass);

		if (drawInstruction != null) {
			drawInstruction.draw(processDiagramCanvas, bpmnModel, artifact);
		}else
			System.out.println("Not supported: "+artifactClass+" "+artifact.getId());
	}

	protected static List<GraphicInfo> connectionPerfectionizer(DefaultProcessDiagramCanvas processDiagramCanvas, BpmnModel bpmnModel, BaseElement sourceElement, BaseElement targetElement, List<GraphicInfo> graphicInfoList) {
		GraphicInfo sourceGraphicInfo = bpmnModel.getGraphicInfo(sourceElement.getId());
		GraphicInfo targetGraphicInfo = bpmnModel.getGraphicInfo(targetElement.getId());

		DefaultProcessDiagramCanvas.SHAPE_TYPE sourceShapeType = getShapeType(sourceElement);
		DefaultProcessDiagramCanvas.SHAPE_TYPE targetShapeType = getShapeType(targetElement);

		return processDiagramCanvas.connectionPerfectionizer(sourceShapeType, targetShapeType, sourceGraphicInfo, targetGraphicInfo, graphicInfoList);
	}
}
