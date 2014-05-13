package org.wisdom.orientdb.runtime;

import com.orientechnologies.orient.object.db.OObjectDatabasePool;
import com.orientechnologies.orient.object.db.OObjectDatabaseTx;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.wisdom.api.model.Crud;
import org.wisdom.api.model.Repository;
import org.wisdom.orientdb.conf.WOrientConf;

import java.util.Collection;
import java.util.Dictionary;
import java.util.HashSet;

/**
 * Created by barjo on 5/10/14.
 */
public class OrientDbRepository implements Repository<OObjectDatabasePool>{
    private final OObjectDatabasePool server;
    private final WOrientConf conf;

    private Collection<ServiceRegistration<Crud>> registrations = new HashSet<ServiceRegistration<Crud>>();

    public OrientDbRepository(WOrientConf conf, ClassLoader loader,BundleContext context){
        this.server = new OObjectDatabasePool(conf.getUrl(),conf.getUser(),conf.getPass());
        Collection<Class<?>> entities;
        this.conf = conf;

        OObjectDatabaseTx db = server.acquire();

        db.getEntityManager().registerEntityClasses(conf.getNameSpace(),loader);

        entities = db.getEntityManager().getRegisteredEntities();

        db.close();

        //Register a Crud service for each entities
        Dictionary<String,String> properties = conf.toDico();

        //register a service for each entities matching the namespace
        for(Class<?> entity : entities){
            if(entity.getCanonicalName().startsWith(conf.getNameSpace())){
                registrations.add(context.registerService(Crud.class, new OrientDbCrudService(this, entity), properties));
            }
        }
    }

    public void destroy(ClassLoader loader){
        for(ServiceRegistration<Crud> reg : registrations){
            reg.unregister();
        }

        OObjectDatabaseTx db = server.acquire();
        db.getEntityManager().deregisterEntityClasses(conf.getNameSpace(),loader);
        db.close();
        server.close();
    }

    public WOrientConf getConf(){
        return conf;
    }

    @Override
    public Collection<Crud<?, ?>> getCrudServices() {
        return null;
    }

    @Override
    public String getName() {
        return server.getName();
    }


    @Override
    public String getType() {
        return "orientdb-object-pool";
    }

    @Override
    public Class<OObjectDatabasePool> getRepositoryClass() {
        return OObjectDatabasePool.class;
    }

    @Override
    public OObjectDatabasePool get() {
        return server;
    }
}