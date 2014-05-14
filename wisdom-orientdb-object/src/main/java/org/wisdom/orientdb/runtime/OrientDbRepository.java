package org.wisdom.orientdb.runtime;

import com.orientechnologies.orient.core.entity.OEntityManager;
import com.orientechnologies.orient.object.db.OObjectDatabasePool;
import com.orientechnologies.orient.object.db.OObjectDatabaseTx;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.wisdom.api.model.Crud;
import org.wisdom.api.model.Repository;
import org.wisdom.orientdb.conf.WOrientConf;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by barjo on 5/10/14.
 */
public class OrientDbRepository implements Repository<OObjectDatabasePool>{
    private final OObjectDatabasePool server;
    private final WOrientConf conf;

    private Map<Class,ServiceRegistration<Crud>> registrations = new HashMap<>();

    public OrientDbRepository(WOrientConf conf){
        this.server = new OObjectDatabasePool(conf.getUrl(),conf.getUser(),conf.getPass());
        this.conf = conf;
    }

    public WOrientConf getConf(){
        return conf;
    }

    protected void registerCrudService(Class entity,BundleContext context){
        //register the entity if not present
        OObjectDatabaseTx db = server.acquire();
        if(!db.getEntityManager().getRegisteredEntities().contains(entity)){
            db.getEntityManager().registerEntityClass(entity);
        }
        db.close();

        registrations.put(entity, context.registerService(Crud.class, new OrientDbCrudService(this, entity), conf.toDico()));
    }

    protected void destroy(){
        OEntityManager em = server.acquire().getEntityManager();

        for(Map.Entry<Class,ServiceRegistration<Crud>> entry: registrations.entrySet()){
            entry.getValue().unregister();
            em.deregisterEntityClass(entry.getKey());
        }

        server.close();
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