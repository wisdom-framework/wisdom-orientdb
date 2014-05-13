/*
 * Copyright 2014, Technologic Arts Vietnam.
 * All right reserved.
 */

package org.wisdom.orientdb.runtime;

import com.orientechnologies.orient.core.metadata.security.OSecurity;
import com.orientechnologies.orient.core.metadata.security.OUser;
import com.orientechnologies.orient.object.db.OObjectDatabaseTx;
import org.apache.felix.ipojo.annotations.*;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.resource.Capability;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.wisdom.api.configuration.ApplicationConfiguration;
import org.wisdom.orientdb.conf.WOrientConf;

import java.io.File;
import java.util.*;

import java.net.URL;

import static java.io.File.pathSeparator;

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

            Enumeration<URL> enums = bundle.findEntries(conf.getNameSpace().replace(".", pathSeparator), "*.class", true);

            if(!enums.hasMoreElements()){
                break; //next configuration
            }

            OrientDbRepository repo  = new OrientDbRepository(conf);
            OObjectDatabaseTx db;

            do{
                URL entry = enums.nextElement();
                try {
                    entities.add(bundle.loadClass(entry.getPath().replace(pathSeparator, ".")));
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }while (enums.hasMoreElements());

            try{
                db = repo.get().acquire();
            }catch(Exception e){
                if(appConf.isProd()){
                    throw e;
                }

                //Create the database if in test or dev. mode
                db = new OObjectDatabaseTx(conf.getUrl()).create();
                OSecurity sm = db.getMetadata().getSecurity();
                OUser user = sm.createUser(conf.getUser(), conf.getPass(), new String[]{"admin"});
            }

            for(Class entity: entities){
                repo.registerCrudService(entity,context);
            }

            db.close();

            entities.clear();
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
}
