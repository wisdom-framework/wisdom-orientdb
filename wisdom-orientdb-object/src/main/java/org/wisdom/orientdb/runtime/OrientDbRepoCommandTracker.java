package org.wisdom.orientdb.runtime;

import com.orientechnologies.orient.core.metadata.security.ORole;
import com.orientechnologies.orient.object.db.OObjectDatabaseTx;
import org.apache.felix.ipojo.annotations.*;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wisdom.api.configuration.ApplicationConfiguration;
import org.wisdom.orientdb.object.OrientDbRepoCommand;

/**
 * @author barjo
 */
@Component
@Instantiate
class OrientDbRepoCommandTracker implements ServiceTrackerCustomizer<OrientDbRepoCommand,OrientDbRepositoryImpl> {
    @Requires
    private ApplicationConfiguration appConf;

    private final BundleContext context;

    private ServiceTracker<OrientDbRepoCommand,OrientDbRepositoryImpl> tracker;

    private final Logger logger = LoggerFactory.getLogger(OrientDbRepoCommandTracker.class);


    private OrientDbRepoCommandTracker(BundleContext context) {
        this.context = context;
    }

    @Validate
    private void start(){
        tracker = new ServiceTracker<>(context,OrientDbRepoCommand.class,this);
        tracker.open(true);
    }

    @Invalidate
    private void stop(){
        tracker.close();
    }

    @Override
    public OrientDbRepositoryImpl addingService(ServiceReference<OrientDbRepoCommand> sref) {
        OrientDbRepoCommand creator = context.getService(sref);
        OrientDbRepositoryImpl  repo = new OrientDbRepositoryImpl(creator);

        try {
            tryAcquireOrCreateIfNotProd(repo);
        } catch (Exception e) {
            logger.error("Cannot create OrientDBRepository for db {}",creator.getConf().getAlias(),e);
        }

        //Register all Crud service
        logger.info("Adding Crud service for OrientDB db <{}> - {}",repo.getConf().getAlias(),creator.getEntityClass());
        repo.registerAllCrud(context);
        return repo;
    }


    @Override
    public void modifiedService(ServiceReference<OrientDbRepoCommand> sref, OrientDbRepositoryImpl repo) {

    }

    @Override
    public void removedService(ServiceReference<OrientDbRepoCommand> sref, OrientDbRepositoryImpl repo) {
        repo.destroy(); //Destroy the repository linked to this OrientDbRepoCommand
        logger.info("Removing Crud service for OrientDB db <{}> - {}",repo.getConf().getAlias());
        context.ungetService(sref);
    }


    private void tryAcquireOrCreateIfNotProd(OrientDbRepositoryImpl repo) {
        //Get the connection from the pool
        try{
            repo.get().acquire();
        }catch(Exception e){
            if(appConf.isProd()){
                throw e;
            }

            logger.debug("Cannot access to orientdb database {}; creating new database at url: {}",
                    repo.getConf().getAlias(),repo.getConf().getUrl(),e);

            //Create the database if in test or dev. mode
            OObjectDatabaseTx db = new OObjectDatabaseTx(repo.getConf().getUrl()).create();
            //Add the user as admin to the newly created db.
            db.getMetadata().getSecurity().createUser(repo.getConf().getUser(), repo.getConf().getPass(), ORole.ADMIN);
        }

    }
}
