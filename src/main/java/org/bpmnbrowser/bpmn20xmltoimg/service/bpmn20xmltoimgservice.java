package org.bpmnbrowser.bpmn20xmltoimg.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;

import java.nio.file.StandardOpenOption;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.stream.Stream;

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


	@GET
	@Produces("application/json")
	public Response getBPMN(){
		String resourcePathString = getResourcePath();
		java.nio.file.Path resourcePath = FileSystems.getDefault().getPath(resourcePathString);
		StringBuilder result = new StringBuilder();
		try {
			String contextPath = context.getContextPath();

			result.append("{");
			String wd = walkDirectory(resourcePath,1,contextPath+"/bpmn/");
			result.append(wd.substring(1));
			result.append("}");
			return Response.ok(result.toString()).build();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			return Response.status(404).build();
		}finally{

		}
	}


	private String walkDirectory(java.nio.file.Path resourcePath, int i,String prefix) throws IOException {
		StringBuilder result = new StringBuilder();
		Stream<java.nio.file.Path> folders = Files.walk(resourcePath, 1);
		Iterator<java.nio.file.Path> iter = folders.iterator();
		
		while(iter.hasNext()){
			java.nio.file.Path next = iter.next();

			if(!Files.isSameFile(next, resourcePath) &&  Files.isDirectory(next)){
				if(i>0){

					String walkedDirectory = walkDirectory(next, i-1,prefix+next.getFileName().toString());	
					result.append(walkedDirectory);

				}else{
					result.append(",");
					result.append("\"");
					result.append(resourcePath.getFileName().toString());
					result.append("/");
					result.append(next.getFileName().toString());
					result.append("\"");
					result.append(":");
					result.append("\"");
					result.append(prefix);
					result.append("/");
					result.append(next.getFileName().toString());
					result.append("\"");
				}

			};
		}
		folders.close();
		return result.toString();
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