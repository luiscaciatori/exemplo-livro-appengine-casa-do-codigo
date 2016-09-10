package com.luizcaciatori.exemploum.services;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

@Path("/helloworld")
public class HelloWorld {
	
	@GET
	@Produces("application/json")
	@Path("teste/{name}")		
	public String helloWorld(@PathParam("name") String name) {
		return "Hello World, " + name;
	}
}
