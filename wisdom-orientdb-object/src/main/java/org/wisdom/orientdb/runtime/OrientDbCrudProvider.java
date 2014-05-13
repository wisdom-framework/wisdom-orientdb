/*
 * Copyright 2014, Technologic Arts Vietnam.
 * All right reserved.
 */

package org.wisdom.orientdb.runtime;

import org.apache.felix.ipojo.annotations.*;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.wisdom.api.configuration.ApplicationConfiguration;
import org.wisdom.orientdb.conf.WOrientConf;

import java.util.*;

/**
 * created: 5/13/14.
 *
 * @author <a href="mailto:jbardin@tech-arts.com">Jonathan M. Bardin</a>
 */
@Component
@Instantiate
public class OrientDbCrudProvider implements BundleTrackerCustomizer<Collection<OrientDbRepository>> {

    @Requires
    private ApplicationConfiguration conf;

    private final BundleContext context;

    private final Map<Bundle,OrientDbRepository> repositories = new HashMap<Bundle, OrientDbRepository>(2);

    private BundleTracker<Collection<OrientDbRepository>> bundleTracker;

    private Collection<WOrientConf> confs;

    public OrientDbCrudProvider(BundleContext bundleContext) {
        context = bundleContext;
    }

    @Validate
    private void start(){
        confs = WOrientConf.createFromApplicationConf(conf);

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

        for(WOrientConf conf: confs){

            if(!wiring.getCapabilities(conf.getNameSpace()).isEmpty()){
                repos.add(new OrientDbRepository(conf,wiring.getClassLoader(),context));
            }
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
            repo.destroy(loader);
        }
    }
}
