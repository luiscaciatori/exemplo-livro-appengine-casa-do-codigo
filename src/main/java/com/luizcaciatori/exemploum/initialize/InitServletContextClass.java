package com.luizcaciatori.exemploum.initialize;

import java.util.Calendar;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.mortbay.log.Log;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.luizcaciatori.exemploum.services.UserManager;

public class InitServletContextClass implements ServletContextListener {

	private static final Logger LOG = Logger.getLogger("InitContextClass");

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		LOG.info("Aplicação ExemploUm Iniciada");

		initializeUserEntities();
	}

	private void initializeUserEntities() {
		DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();
		Filter roleFilter = new FilterPredicate(UserManager.PROP_ROLE, FilterOperator.EQUAL, "ADMIN");
		Query query = new Query(UserManager.USER_KIND).setFilter(roleFilter);
		List<Entity> entities = datastoreService.prepare(query).asList(FetchOptions.Builder.withLimit(1));

		if (entities.size() == 0) {
			LOG.info("Nenhum usuário encontrado. Inicializando o tipo Users no Datastore");

			Key userKey = KeyFactory.createKey(UserManager.USER_KIND, "userKey");

			Entity entity = new Entity(UserManager.USER_KIND, userKey);
			entity.setProperty(UserManager.PROP_EMAIL, "admin@email.com");
			entity.setProperty(UserManager.PROP_PASSWORD, "admin");
			entity.setProperty(UserManager.PROP_GCM_REG_ID, "");
			entity.setProperty(UserManager.PROP_LAST_LOGIN, Calendar.getInstance().getTime());
			entity.setProperty(UserManager.PROP_LAST_GCM_REGISTER, Calendar.getInstance().getTime());
			entity.setProperty(UserManager.PROP_ROLE, "ADMIN");

			datastoreService.put(entity);
		}
	}

}
