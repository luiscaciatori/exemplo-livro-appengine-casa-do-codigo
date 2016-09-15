package com.luizcaciatori.exemploum.services;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.luizcaciatori.exemploum.models.User;

@Path("/users")
public class UserManager {

	@Context
	SecurityContext securityContext;

	public static final Logger LOG = Logger.getLogger("UserManager");
	public static final String USER_KIND = "Users";
	public static final String PROP_EMAIL = "email";
	public static final String PROP_PASSWORD = "password";
	public static final String PROP_GCM_REG_ID = "gmcRegId";
	public static final String PROP_LAST_LOGIN = "lastLogin";
	public static final String PROP_LAST_GCM_REGISTER = "lastGcmRegister";
	public static final String PROP_ROLE = "role";

	private boolean checkIfEmailExist(User user) {
		DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();
		Filter filterEmail = new FilterPredicate(PROP_EMAIL, FilterOperator.EQUAL, user.getEmail());
		Query query = new Query(USER_KIND).setFilter(filterEmail);
		Entity userEntity = datastoreService.prepare(query).asSingleEntity();

		if (userEntity == null) {
			return false;
		} else {
			if (userEntity.getKey().getId() == user.getId()) {
				return false;
			} else {
				return false;
			}
		}
	}

	private void userToEntity(User user, Entity userEntity) {
		userEntity.setProperty(PROP_EMAIL, user.getEmail());
		userEntity.setProperty(PROP_PASSWORD, user.getPassword());
		userEntity.setProperty(PROP_GCM_REG_ID, user.getGcmRegId());
		userEntity.setProperty(PROP_LAST_LOGIN, user.getLastLogin());
		userEntity.setProperty(PROP_LAST_GCM_REGISTER, user.getLastGCMRegister());
		userEntity.setProperty(PROP_ROLE, user.getRole());
	}

	private User entityToUser(Entity userEntity) {
		User user = new User();
		user.setId(userEntity.getKey().getId());
		user.setEmail(userEntity.getProperty(PROP_EMAIL).toString());
		user.setPassword(userEntity.getProperty(PROP_PASSWORD).toString());
		user.setGcmRegId(userEntity.getProperty(PROP_GCM_REG_ID).toString());
		user.setLastLogin((Date) userEntity.getProperty(PROP_LAST_LOGIN));
		user.setLastGCMRegister((Date) userEntity.getProperty(PROP_LAST_GCM_REGISTER));
		user.setRole(userEntity.getProperty(PROP_ROLE).toString());

		return user;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ "ADMIN" })
	public List<User> getUsers() {
		List<User> users = new ArrayList<>();
		DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();
		Query query = new Query(USER_KIND).addSort(PROP_EMAIL, SortDirection.ASCENDING);
		List<Entity> entities = datastoreService.prepare(query).asList(FetchOptions.Builder.withDefaults());

		for (Entity entity : entities) {
			User user = entityToUser(entity);

			users.add(user);
		}

		return users;
	}

	@GET
	@Path("/{email}")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ "ADMIN", "USER" })
	public User getUser(@PathParam(PROP_EMAIL) String email) {
		if (securityContext.getUserPrincipal().getName().equals(email) || securityContext.isUserInRole("ADMIN")) {

			DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();
			Filter emailFilter = new FilterPredicate(PROP_EMAIL, FilterOperator.EQUAL, email);
			Query query = new Query(USER_KIND).setFilter(emailFilter);

			Entity userEntity = datastoreService.prepare(query).asSingleEntity();

			if (userEntity == null) {
				User user = entityToUser(userEntity);

				return user;
			} else {
				throw new WebApplicationException(Status.NOT_FOUND);
			}
		} else {
			throw new WebApplicationException(Status.FORBIDDEN);
		}
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@PermitAll
	public User saveUSer(@Valid User user) {
		if (!checkIfEmailExist(user)) {
			DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();
			Key userKey = KeyFactory.createKey(USER_KIND, "userKey");
			Entity userEntity = new Entity(USER_KIND, userKey);

			user.setGcmRegId("");
			user.setLastGCMRegister(null);
			user.setLastLogin(null);

			userToEntity(user, userEntity);

			datastoreService.put(userEntity);

			user.setId(userEntity.getKey().getId());
		} else {
			throw new WebApplicationException("Já existe um usuário cadastrado com este email.", Status.BAD_REQUEST);
		}

		return user;
	}

	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/{email}")
	@RolesAllowed({ "ADMIN", "USER" })
	public User updateUser(@PathParam("email") String email, @Valid User user) {
		if (user.getId() == 0) {
			if (securityContext.getUserPrincipal().getName().equals(email) || securityContext.isUserInRole("ADMIN")) {
				if (!checkIfEmailExist(user)) {

					DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();
					Filter filterEmail = new FilterPredicate(PROP_EMAIL, FilterOperator.EQUAL, email);
					Query query = new Query(USER_KIND).setFilter(filterEmail);
					Entity userEntity = datastoreService.prepare(query).asSingleEntity();

					if (userEntity != null) {
						userToEntity(user, userEntity);

						if (!securityContext.isUserInRole("ADMIN")) {
							user.setRole("USER");
						}

						datastoreService.put(userEntity);

						return user;
					} else {
						throw new WebApplicationException(Status.NOT_FOUND);
					}
				} else {
					throw new WebApplicationException("Já existe um usuário cadastrado com esse e-mail.",
							Status.BAD_REQUEST);
				}
			} else {
				throw new WebApplicationException(Status.FORBIDDEN);
			}
		} else {
			throw new WebApplicationException("O ID do usuário deve ser informado para ser alterado",
					Status.BAD_REQUEST);
		}
	}

	@DELETE
	@Path("/{email}")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ "ADMIN", "USER" })
	public User deleteUser(@PathParam("email") String email) {
		if (securityContext.getUserPrincipal().getName().equals(email) || securityContext.isUserInRole("ADMIN")) {
			DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();
			Filter filterEmail = new FilterPredicate(PROP_EMAIL, FilterOperator.EQUAL, email);
			Query query = new Query(USER_KIND).setFilter(filterEmail);
			Entity userEntity = datastoreService.prepare(query).asSingleEntity();

			if (userEntity != null) {
				datastoreService.delete(userEntity.getKey());

				User user = entityToUser(userEntity);

				return user;
			} else {
				throw new WebApplicationException(Status.NOT_FOUND);
			}
		} else {
			throw new WebApplicationException(Status.FORBIDDEN);
		}
	}

	@PUT
	@Path("/update_gcm_reg_id/{gcmRegID}")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ "USER" })
	public User updateGCMRegId(@PathParam("gcmRegID") String gcmRegID) {
		DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();
		Filter filterEmail = new FilterPredicate(PROP_EMAIL, FilterOperator.EQUAL,
				securityContext.getUserPrincipal().getName());
		Query query = new Query(USER_KIND).setFilter(filterEmail);
		Entity userEntity = datastoreService.prepare(query).asSingleEntity();

		if (userEntity != null) {
			userEntity.setProperty(PROP_GCM_REG_ID, gcmRegID);
			userEntity.setProperty(PROP_LAST_GCM_REGISTER, Calendar.getInstance().getTime());

			datastoreService.put(userEntity);

			User user = entityToUser(userEntity);

			return user;
		} else {
			throw new WebApplicationException(Status.NOT_FOUND);
		}
	}

}
