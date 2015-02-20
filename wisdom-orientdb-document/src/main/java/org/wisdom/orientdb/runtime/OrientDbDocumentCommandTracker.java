package org.wisdom.orientdb.runtime;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.security.ORole;
import org.apache.felix.ipojo.annotations.*;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wisdom.api.configuration.ApplicationConfiguration;
import org.wisdom.orientdb.document.OrientDbDocumentCommand;

/**
 * @author barjo
 */
@Component
@Instantiate
class OrientDbDocumentCommandTracker implements ServiceTrackerCustomizer<OrientDbDocumentCommand,OrientDbDocServiceImpl> {
    @Requires
    private ApplicationConfiguration appConf;

    private final BundleContext context;

    private ServiceTracker<OrientDbDocumentCommand,OrientDbDocServiceImpl> tracker;

    private final Logger logger = LoggerFactory.getLogger(OrientDbDocumentCommandTracker.class);

    private OrientDbDocumentCommandTracker(BundleContext context) {
        this.context = context;
    }

    @Validate
    private void start(){
        tracker = new ServiceTracker<>(context,OrientDbDocumentCommand.class,this);
        tracker.open(true);
    }

    @Invalidate
    private void stop(){
        tracker.close();
    }

    @Override
    public OrientDbDocServiceImpl addingService(ServiceReference<OrientDbDocumentCommand> sref) {
        OrientDbDocumentCommand creator = context.getService(sref);
        OrientDbDocServiceImpl repo = new OrientDbDocServiceImpl(creator,context);

        try {
            tryAcquireOrCreateIfNotProd(repo);
        } catch (Exception e) {
            logger.error("Cannot create OrientDbDocumentService for OrientDb alias <{}>",creator.getConf().getAlias(),e);
        }

        //Register all Crud service
        logger.info("Adding OrientDbDocumentService for OrientDB alias <{}>",repo.getConf().getAlias());
        repo.register(context);
        return repo;
    }


    @Override
    public void modifiedService(ServiceReference<OrientDbDocumentCommand> sref, OrientDbDocServiceImpl repo) {

    }

    @Override
    public void removedService(ServiceReference<OrientDbDocumentCommand> sref, OrientDbDocServiceImpl repo) {
        repo.destroy(); //Destroy the repository linked to this OrientDbDocumentCommand
        logger.info("Removing OrientDbDocumentService for OrientDB alias <{}>",repo.getConf().getAlias());
        context.ungetService(sref);
    }


    private void tryAcquireOrCreateIfNotProd(OrientDbDocServiceImpl repo) {
        //Get the connection from the pool
        try{
            repo.acquire();
        }catch(Exception e){
            if(appConf.isProd()){
                throw e;
            }

            logger.debug("Cannot access to orientdb database alias <{}>; creating new database at url: {}",
                    repo.getConf().getAlias(),repo.getConf().getUrl(),e);

            //Create the database if in test or dev. mode
            ODatabaseDocumentTx db = new ODatabaseDocumentTx(repo.getConf().getUrl()).create();
            //Add the user as admin to the newly created db.
            db.getMetadata().getSecurity().createUser(repo.getConf().getUser(), repo.getConf().getPass(), ORole.ADMIN);
        }
    }
}
