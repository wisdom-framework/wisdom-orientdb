package org.wisdom.orientdb.runtime;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import org.apache.felix.ipojo.annotations.*;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wisdom.api.configuration.ApplicationConfiguration;
import org.wisdom.orientdb.conf.WOrientConf;
import org.wisdom.orientdb.document.OrientDbDocumentCommand;
import org.wisdom.orientdb.manager.OrientDbManager;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Parse the wisdom-orientdb configuration and publish the corresponding {@link org.wisdom.orientdb.document.OrientDbDocumentCommand}.
 *
 * @author barjo
 */
@Component(name = OrientDbDocumentProvider.COMPONENT_NAME)
@Instantiate(name = OrientDbDocumentProvider.INSTANCE_NAME)
class OrientDbDocumentProvider {
    static final String COMPONENT_NAME = "wisdom:orientdb:documentRepo:factory";
    static final String INSTANCE_NAME = "wisdom:orientdb:documentRepo:provider";


    @Requires
    private ApplicationConfiguration appConf;

    /**
     * Bind our lifecycle to the {@link OrientDbManager} manager.
     **/
    @Requires
    private OrientDbManager manager;

    private final Logger logger = LoggerFactory.getLogger(OrientDbDocumentProvider.class);

    private final BundleContext context;

    private SortedMap<String,ServiceRegistration<OrientDbDocumentCommand>> registrations = new TreeMap<>();

    public OrientDbDocumentProvider(BundleContext bundleContext) {
        context = bundleContext;
    }

    @Validate
    private void start(){
        //Parse the configuration
        for(WOrientConf conf : WOrientConf.createFromApplicationConf(appConf)){
            //Publish an OrientDbDocumentCommand for this configuration
            registrations.put(conf.getAlias(), context.registerService(OrientDbDocumentCommand.class, new OrientDbDocumentCommandImpl(conf), conf.toDico()));
            logger.debug("The document command for OrientDb database <{}> has been published.",conf.getAlias());
        }
    }

    @Invalidate
    private void stop(){
        for(Map.Entry<String,ServiceRegistration<OrientDbDocumentCommand>> entry: registrations.entrySet()){
            logger.debug("The document command for OrientDb database <{}> has been unpublished.",entry.getKey());
            entry.getValue().unregister();
        }
        registrations.clear();
    }


    /**
     * Dummy {@link OrientDbDocumentCommand} which do nothing.
     */
    class OrientDbDocumentCommandImpl implements OrientDbDocumentCommand {
        private final WOrientConf conf;

        public OrientDbDocumentCommandImpl(WOrientConf conf) {
            this.conf = conf;
        }

        public WOrientConf getConf() {
            return conf;
        }

        @Override
        public void init(ODatabaseDocumentTx db) {
        }

        @Override
        public void destroy(ODatabaseDocumentTx db) {
        }
    }
}
