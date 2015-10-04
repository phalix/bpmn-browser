package org.bpmnbrowser.bpmn20xmltoimg.service;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.PathMatcher;
import java.util.Iterator;
import java.util.function.Consumer;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.bpmnbrowser.bpmn20xmltoimg.BPMN20XMLParser;


@Path("/bpmn")
public class bpmn20xmltoimgservice {

	@Context
	private ServletContext context;

	
	@PUT
	@Path("/{group}/{name}")
	@Produces("application/xml")
	@Consumes(MediaType.APPLICATION_XML)
	public Response setBPMNDiagram(@PathParam(value = "group") String group, @PathParam(value = "name") String name,String requestbody){
		
		//
		
		try{
			BPMN20XMLParser parser = new BPMN20XMLParser();
			parser.parseXML(requestbody);
			
			//TODO: Create Folders
			//TODO: Save xml!
			
			return Response.ok().build();
		}catch(Exception e){
			return Response.status(401).build();
		}
	}

	@GET
	@Path("/{group}/{name}")
	@Produces("image/png")
	public Response getBPMNDiagram(@PathParam(value = "group") String group, @PathParam(value = "name") String name){
		String realPath = context.getRealPath("/resources/"+group+"/"+name);
		java.nio.file.Path subRoot = FileSystems.getDefault().getPath(realPath);
		java.nio.file.Path input = null;// = FileSystems.getDefault().getPath(realPath, ".xml");

		PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:"+realPath+"/*.xml");

		
		
		try {
			
			Iterator<java.nio.file.Path> iterator = Files.walk(subRoot).iterator();
			while(iterator.hasNext()){
				java.nio.file.Path next = iterator.next();
				if(pathMatcher.matches(next)){
					input = next;
				}
			};
			
			if(input == null ){
				return Response.status(404).build();
			}
			
			
			String np = input.toString().substring(0, input.toString().lastIndexOf("."));
			
			java.nio.file.Path output = FileSystems.getDefault().getPath(np+".png");

			BPMN20XMLParser parser = new BPMN20XMLParser();
			parser.parseXMLFileToPngFile(input, output);


			byte[] resp;

			resp = Files.readAllBytes(output);
			return Response.ok(resp).build();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return Response.status(404).build();
	}
}