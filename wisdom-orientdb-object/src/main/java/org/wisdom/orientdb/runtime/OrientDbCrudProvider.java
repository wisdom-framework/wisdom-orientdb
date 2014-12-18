package org.wisdom.orientdb.runtime;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.object.db.OObjectDatabaseTx;
import com.orientechnologies.orient.object.enhancement.OObjectProxyMethodHandler;
import org.apache.felix.ipojo.annotations.*;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wisdom.api.configuration.ApplicationConfiguration;
import org.wisdom.api.content.JacksonModuleRepository;
import org.wisdom.orientdb.conf.WOrientConf;
import org.wisdom.orientdb.object.OrientDbRepoCommand;
import sun.misc.CompoundEnumeration;

import java.io.IOException;
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
class OrientDbCrudProvider implements BundleTrackerCustomizer<Collection<ServiceRegistration>> {
    static final String COMPONENT_NAME = "wisdom:orientdb:crudservice:factory";
    static final String INSTANCE_NAME = "wisdom:orientdb:crudservice:provider";


    @Requires
    private ApplicationConfiguration appConf;

    @Requires
    private JacksonModuleRepository moduleRepository;

    private final Logger logger = LoggerFactory.getLogger(OrientDbCrudProvider.class);

    private static final SimpleModule module = new SimpleModule("Orientdb Ignore Proxy");

    static {
        module.addSerializer(OObjectProxyMethodHandler.class, new JsonSerializer<OObjectProxyMethodHandler>() {
            @Override
            public void serialize(OObjectProxyMethodHandler oObjectProxyMethodHandler, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
                jsonGenerator.writeStartObject();
            }
        });
    }

    private final BundleContext context;

    private BundleTracker<Collection<ServiceRegistration>> bundleTracker;

    private Collection<WOrientConf> confs;

    public OrientDbCrudProvider(BundleContext bundleContext) {
        context = bundleContext;
    }

    @Validate
    private void start(){
        //Ignore javaassit injected handler created by Orientdb for json serialization
        moduleRepository.register(module);

        //Not sure if orient has already been startup?
        Orient.instance().startup();

        //remove the hook since we handle shutdown in the stop callback
        Orient.instance().removeShutdownHook();

        //Parse the configuration
        confs = WOrientConf.createFromApplicationConf(appConf);

        //OrientDb database has been set up from the configuration file.
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

        moduleRepository.unregister(module);
    }


    @Override
    public Collection<ServiceRegistration> addingBundle(Bundle bundle, BundleEvent bundleEvent) {
        Collection<ServiceRegistration> registrations = new HashSet<>();

        for(WOrientConf conf: confs){

            List<Class<?>> entities = new ArrayList<>();

            for(String ns : conf.getNameSpace()) {
                Enumeration<URL> enums = bundle.findEntries(packageNameToPath(ns), "*.class", true);
                if(enums == null || !enums.hasMoreElements()){
                    continue;
                }
                logger.info("OrientDB Database configuration found for {} : {}",
                        packageNameToPath(ns), conf.toString());

                //Load the entities from the bundle
                do{
                    URL entry = enums.nextElement();
                    try {
                        entities.add(bundle.loadClass(urlToClassName(entry)));
                    } catch (ClassNotFoundException e) {
                        logger.error("Cannot load entity class {}",entry,e);
                    }
                }while (enums.hasMoreElements());
            }

            if(entities.isEmpty()){
                logger.debug("OrientDB configuration {} does not contains any Entity class, configuration is ignored.",
                        conf.getAlias());
                continue;
            }

            //Create and register a new OrientDbRepoCommand for the given entities and db configuration
            OrientDbRepoCommand repoWB = new OrientDbRepoCommandImpl(entities,conf);
            registrations.add(context.registerService(OrientDbRepoCommand.class, repoWB, new Hashtable<String, Object>()));
            logger.debug("The command for OrientDb database {} has been published.",conf.getAlias());
        }

        return registrations;
    }

    @Override
    public void modifiedBundle(Bundle bundle, BundleEvent bundleEvent, Collection<ServiceRegistration> registrations) {
        //TODO very dummy fix that
        //removedBundle(bundle,bundleEvent,repositories);
        //addingBundle(bundle,bundleEvent);
    }

    @Override
    public void removedBundle(Bundle bundle, BundleEvent bundleEvent, Collection<ServiceRegistration> registrations) {
        for(ServiceRegistration sreg: registrations){
            sreg.unregister();
        }
    }

    /**
     * Simple OrientDbRepoCommand which register the Entity class in the database entity manager upon initialisation and
     * remove them upon destroy.
     */
    class OrientDbRepoCommandImpl implements OrientDbRepoCommand {

        private final WOrientConf conf;

        private final List<Class<?>> entities;

        public OrientDbRepoCommandImpl(List<Class<?>> entities, WOrientConf conf) {
            this.entities =entities;
            this.conf = conf;
        }

        public WOrientConf getConf() {
            return conf;
        }

        public List<Class<?>> getEntityClass() {
            return entities;
        }

        public void init(OObjectDatabaseTx db) {
            for(Class entity: entities){
                db.getEntityManager().registerEntityClass(entity);
            }
        }

        public void destroy(OObjectDatabaseTx db) {
            for(Class entity: entities){
                db.getEntityManager().deregisterEntityClass(entity);
            }
        }
    }


    //
    // Helper methods
    //

    private static String urlToClassName(URL url){
        String path = url.getPath();
        return path.replace(separator,".").substring(1,path.lastIndexOf("."));
    }

    private static String packageNameToPath(String packageName){
        return separator + packageName.replace(".",separator);
    }
}
