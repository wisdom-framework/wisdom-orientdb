/*
 * Copyright 2014, Technologic Arts Vietnam.
 * All right reserved.
 */

package org.wisdom.orientdb.runtime;

import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.metadata.security.ORole;
import com.orientechnologies.orient.object.db.OObjectDatabaseTx;
import org.apache.felix.ipojo.annotations.*;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.wisdom.api.configuration.ApplicationConfiguration;
import org.wisdom.orientdb.conf.WOrientConf;

import java.net.URL;
import java.util.*;

import static java.io.File.separator;

/**
 * created: 5/13/14.
 *
 * @author <a href="mailto:jbardin@tech-arts.com">Jonathan M. Bardin</a>
 */
@Component(name = OrientDbCrudProvider.COMPONENT_NAME)
@Instantiate(name = OrientDbCrudProvider.INSTANCE_NAME)
public class OrientDbCrudProvider implements BundleTrackerCustomizer<Collection<OrientDbRepository>> {
    public static final String COMPONENT_NAME = "wisdom:orientdb:crudservice:factory";
    public static final String INSTANCE_NAME = "wisdom:orientdb:crudservice:provider";


    @Requires
    private ApplicationConfiguration appConf;

    private final BundleContext context;

    private BundleTracker<Collection<OrientDbRepository>> bundleTracker;

    private Collection<WOrientConf> confs;

    public OrientDbCrudProvider(BundleContext bundleContext) {
        context = bundleContext;
    }

    @Validate
    private void start(){
        Orient.instance().startup();
        Orient.instance().removeShutdownHook();
        confs = WOrientConf.createFromApplicationConf(appConf);

        if(!confs.isEmpty()){
            bundleTracker = new BundleTracker<>(context, Bundle.ACTIVE, this);
            bundleTracker.open();
        }
    }

    @Invalidate
    private void stop(){
        if(bundleTracker != null){
            bundleTracker.close();
        }

        Orient.instance().shutdown();
    }


    @Override
    public Collection<OrientDbRepository> addingBundle(Bundle bundle, BundleEvent bundleEvent) {
        if(confs.isEmpty()){
            return null;
        }

        Collection<OrientDbRepository> repos = new HashSet<>();
        BundleWiring wiring = bundle.adapt(BundleWiring.class);
        Iterator<WOrientConf> confIt = confs.iterator();
        List<Class> entities = new ArrayList<>();

        for(WOrientConf conf: confs){

            Enumeration<URL> enums = bundle.findEntries(packageNameToPath(conf.getNameSpace()), "*.class", true);

            if(enums== null || !enums.hasMoreElements()){
                break; //next configuration
            }

            //Create a pull for this configuration
            OrientDbRepository repo  = new OrientDbRepository(conf);
            OObjectDatabaseTx db;

            //Load the entities from the bundle
            do{
                URL entry = enums.nextElement();
                try {
                    entities.add(bundle.loadClass(urlToClassName(entry)));
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }while (enums.hasMoreElements());

            //Get the connection from the pool
            try{
                db = repo.get().acquire();
            }catch(Exception e){
                if(appConf.isProd()){
                    throw e;
                }

                //Create the database if in test or dev. mode
                db = new OObjectDatabaseTx(conf.getUrl()).create();

                //Add the user as admin to the newly created db.
                db.getMetadata().getSecurity().createUser(conf.getUser(), conf.getPass(), new String[]{ORole.ADMIN});
            }

            //Register a crud service for each entity
            for(Class entity: entities){
                repo.registerCrudService(entity,context);
            }

            //close the db
            db.close();

            //clear the entity list
            entities.clear();

            //add this configuration repo
            repos.add(repo);
        }

        return repos;
    }

    @Override
    public void modifiedBundle(Bundle bundle, BundleEvent bundleEvent, Collection<OrientDbRepository> repositories) {
        //TODO very dummy fix that
        removedBundle(bundle,bundleEvent,repositories);
        addingBundle(bundle,bundleEvent);
    }

    @Override
    public void removedBundle(Bundle bundle, BundleEvent bundleEvent, Collection<OrientDbRepository> repositories) {
        ClassLoader loader = bundle.adapt(BundleWiring.class).getClassLoader();
        for(OrientDbRepository repo: repositories){
            repo.destroy();
        }
    }

    private static String urlToClassName(URL url){
        String path = url.getPath();
        return path.replace(separator,".").substring(1,path.lastIndexOf("."));
    }

    private static String packageNameToPath(String packageName){
        return separator + packageName.replace(".",separator);
    }
}
