package org.bpmnbrowser.bpmn20xmltoimg.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;

import java.nio.file.StandardOpenOption;
import java.util.Iterator;


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

	static String RESOURCES_DIR = "/resources";
	
	@Context
	private ServletContext context;

	private String getResourcePath(){
		//TODO: is it necessary to create the resources directory here, if not available?
		return context.getRealPath(RESOURCES_DIR);
	}
	
	
	@PUT
	@Path("/{group}/{name}")
	@Produces("application/xml")
	@Consumes(MediaType.APPLICATION_XML)
	public Response setBPMNDiagram(@PathParam(value = "group") String group, @PathParam(value = "name") String name,String requestbody){
		
		try{
			BPMN20XMLParser parser = new BPMN20XMLParser();
			InputStream png = parser.parseXML(requestbody);
			
			
			//Create Folders
			String resourcePath = getResourcePath();
			//java.nio.file.Path path = FileSystems.getDefault().getPath(resourcePath);
			java.nio.file.Path newFolders = Paths.get(resourcePath, group,name);
			Files.createDirectories(newFolders);
			
			//Save xml!
			String finalPath = context.getRealPath(RESOURCES_DIR+"/"+group+"/"+name+"/standard.bpmn20.xml");
			java.nio.file.Path savePath = FileSystems.getDefault().getPath(finalPath);
			
			Files.deleteIfExists(savePath);
			
			Files.write(savePath, requestbody.getBytes(), StandardOpenOption.CREATE);
			//Files.copy(requestbody, savePath, StandardCopyOption.REPLACE_EXISTING);
			
			
			return Response.ok().build();
		}catch(NoSuchFileException e){
			return Response.status(404).build();
		}catch(IOException e){
			return Response.status(500).build();
		}
	}

	@GET
	@Path("/{group}/{name}")
	@Produces("image/png")
	public Response getBPMNDiagram(@PathParam(value = "group") String group, @PathParam(value = "name") String name){
		getResourcePath();
		String realPath = context.getRealPath(RESOURCES_DIR+"/"+group+"/"+name);
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
		}  catch(NoSuchFileException e){
			return Response.status(404).build();
		} catch (IOException e) {
			return Response.status(500).build();
		}
	}
}