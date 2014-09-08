package org.wisdom.orientdb.runtime;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.metadata.security.ORole;
import com.orientechnologies.orient.object.db.OObjectDatabaseTx;
import com.orientechnologies.orient.object.enhancement.OObjectProxyMethodHandler;
import org.apache.felix.ipojo.annotations.*;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wisdom.api.configuration.ApplicationConfiguration;
import org.wisdom.api.content.JacksonModuleRepository;
import org.wisdom.orientdb.conf.WOrientConf;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;

import static java.io.File.separator;

/**
 * created: 5/13/14.
 *
 * @author <a href="mailto:jbardin@tech-arts.com">Jonathan M. Bardin</a>
 */
@Component(name = OrientDbCrudProvider.COMPONENT_NAME)
@Instantiate(name = OrientDbCrudProvider.INSTANCE_NAME)
public class OrientDbCrudProvider implements BundleTrackerCustomizer<Collection<OrientDbRepositoryImpl>> {
    public static final String COMPONENT_NAME = "wisdom:orientdb:crudservice:factory";
    public static final String INSTANCE_NAME = "wisdom:orientdb:crudservice:provider";


    @Requires
    private ApplicationConfiguration appConf;

    @Requires
    private JacksonModuleRepository moduleRepository;

    private Logger logger = LoggerFactory.getLogger(OrientDbCrudProvider.class);

    private static final SimpleModule module = new SimpleModule("Orientdb Ignore Proxy");

    static {
        module.addSerializer(OObjectProxyMethodHandler.class, new JsonSerializer<OObjectProxyMethodHandler>() {
            @Override
            public void serialize(OObjectProxyMethodHandler oObjectProxyMethodHandler, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
                jsonGenerator.writeStartObject();
                jsonGenerator.writeStartObject();
            }
        });
    }


    private final BundleContext context;

    private BundleTracker<Collection<OrientDbRepositoryImpl>> bundleTracker;

    private Collection<WOrientConf> confs;

    public OrientDbCrudProvider(BundleContext bundleContext) {
        context = bundleContext;
    }

    @Validate
    private void start(){
        confs = WOrientConf.createFromApplicationConf(appConf);

        if(confs.isEmpty()){
            return;
        }

        //Ignore javaassit injected handler created by Orientdb for json serialization
        moduleRepository.register(module);

        //Not sure if orient has already been startup?
        Orient.instance().startup();

        //remove the hook since we handle shutdown in the stop callback
        Orient.instance().removeShutdownHook();

        bundleTracker = new BundleTracker<>(context, Bundle.ACTIVE, this);
        bundleTracker.open();
    }

    @Invalidate
    private void stop(){
        if(confs.isEmpty()){
            return;
        }

        if(bundleTracker != null){
            bundleTracker.close();
        }

        Orient.instance().shutdown();

        moduleRepository.unregister(module);
    }


    @Override
    public Collection<OrientDbRepositoryImpl> addingBundle(Bundle bundle, BundleEvent bundleEvent) {
        Collection<OrientDbRepositoryImpl> repos = new HashSet<>();

        for(WOrientConf conf: confs){

            Enumeration<URL> enums = bundle.findEntries(packageNameToPath(conf.getNameSpace()), "*.class", true);

            if(enums == null || !enums.hasMoreElements()){
                continue; //next configuration
            }

            logger.info("OrientDB Database configuration found for {} : {}",
                    packageNameToPath(conf.getNameSpace()), conf.toString());

            //Create a pull for this configuration
            OrientDbRepositoryImpl repo  = new OrientDbRepositoryImpl(conf);
            OObjectDatabaseTx db;

            //Get the connection from the pool
            try{
                db = repo.get().acquire();
            }catch(Exception e){
                if(appConf.isProd()){
                    throw e;
                }

                logger.debug("Cannot access to orientdb database {}; creating new database at url: {}",
                        conf.getAlias(),conf.getUrl(),e);

                //Create the database if in test or dev. mode
                db = new OObjectDatabaseTx(conf.getUrl()).create();
                //Add the user as admin to the newly created db.
                db.getMetadata().getSecurity().createUser(conf.getUser(), conf.getPass(), ORole.ADMIN);
            }


            //if(!db.exists()){
            //
            //}

            //Load the entities from the bundle
            do{
                URL entry = enums.nextElement();
                try {
                    repo.addCrubService(bundle.loadClass(urlToClassName(entry)));
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }while (enums.hasMoreElements());


            logger.debug("Crud service has been added for {} in {} OrientDb.",conf.getNameSpace(),conf.getAlias());

            //register all crud service available in this repo
            repo.registerAllCrud(context);

            //close the db
            db.close();

            //add this configuration repo
            repos.add(repo);
        }

        return repos;
    }

    @Override
    public void modifiedBundle(Bundle bundle, BundleEvent bundleEvent, Collection<OrientDbRepositoryImpl> repositories) {
        //TODO very dummy fix that
        //removedBundle(bundle,bundleEvent,repositories);
        //addingBundle(bundle,bundleEvent);
    }

    @Override
    public void removedBundle(Bundle bundle, BundleEvent bundleEvent, Collection<OrientDbRepositoryImpl> repositories) {
        for(OrientDbRepositoryImpl repo: repositories){
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
