package org.bittmann.bpmn20xmltoimg;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.engine.impl.bpmn.parser.BpmnParser;
import org.activiti.engine.impl.cfg.BpmnParseFactory;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.interceptor.CommandInterceptor;


import org.bittmann.engine.impl.bpmn.parser.BittmannBpmnParse;
import org.bittmann.image.impl.BittmannProcessDiagramGenerator;


/**
 * @author Sebastian Bittmann
 */

public class BPMN20XMLParser {
	BpmnParser bpr;
	BpmnParseFactory bpf;
	ProcessEngineConfigurationImpl pec;
	
	public BPMN20XMLParser(){
		bpf = new BpmnParseFactory() {
			
			public BittmannBpmnParse createBpmnParse(BpmnParser bpmnParser) {
				return new BittmannBpmnParse(bpmnParser);
			}
		};
		bpr = new BpmnParser();
		bpr.setBpmnParseFactory(bpf);
		pec = new ProcessEngineConfigurationImpl() {
			
			@Override
			protected CommandInterceptor createTransactionInterceptor() {
				return null;
			}
		};	
		Context.setProcessEngineConfiguration(pec);
		pec.setEnableSafeBpmnXml(true);
		pec.setXmlEncoding("UTF-8");
	}

	private void writeFileFromStream(InputStream pngDiagram,Path toPath){
		try {
			Files.copy(pngDiagram, toPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	/**
	 * 
	 * @param xml - an xml string
	 * @param toFile - the target filename
	 */
	public void parseXMLToFile(String xml,String toFile) {
		InputStream pngDiagram = this.parseXML(xml);
		Path output = FileSystems.getDefault().getPath("resources",toFile);
		writeFileFromStream(pngDiagram,output);
	}
	
	private BittmannBpmnParse createParser(){
		return new BittmannBpmnParse(bpr);
	}
	
	private void execute(BittmannBpmnParse bp){
		bp.execute();
	}
	
	public void parseFile(String file){
		Path input = FileSystems.getDefault().getPath(file);
		Path output = FileSystems.getDefault().getPath(file+".png");
		
		parseXMLFileToPngFile(input, output);
	}

	public void parseXMLFileToPngFile(Path input, Path output) {
		InputStream br;
		try {
			br = Files.newInputStream(input,java.nio.file.StandardOpenOption.READ);
			
			//String content = new String(Files.readAllBytes(input));
			BittmannBpmnParse bp = createParser();
			//bp.sourceString(content);
			bp.sourceInputStream(br);
			this.execute(bp);
			this.writeFileFromStream(this.generatePngDiagramFromBPMNModel(bp.getBpmnModel()), output);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private InputStream generatePngDiagramFromBPMNModel(BpmnModel bpmnm){
		
		InputStream pngDiagram = 
		new BittmannProcessDiagramGenerator(1.0).generateDiagram(bpmnm, "png", 
				Collections.<String>emptyList(), Collections.<String>emptyList(),
				pec.getActivityFontName(), pec.getLabelFontName(), 
		        pec.getClassLoader(), 1.0);
		
		return pngDiagram;
	}
	
	public InputStream parseXML(String xml) {
		BittmannBpmnParse bp = createParser();
		bp.sourceString(xml);
		this.execute(bp);
		bp.getProcessDefinitions();
		return null;
		//return this.generatePngDiagramFromBPMNModel(bp.getBpmnModel());
	}
}
