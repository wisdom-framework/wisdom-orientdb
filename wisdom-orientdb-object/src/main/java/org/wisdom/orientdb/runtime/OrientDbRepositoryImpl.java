package org.wisdom.orientdb.runtime;

import com.orientechnologies.orient.core.entity.OEntityManager;
import com.orientechnologies.orient.object.db.OObjectDatabasePool;
import com.orientechnologies.orient.object.db.OObjectDatabaseTx;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.wisdom.api.model.Crud;
import org.wisdom.orientdb.conf.WOrientConf;
import org.wisdom.orientdb.object.OrientDbCrud;
import org.wisdom.orientdb.object.OrientDbRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;

/**
 * Implementation of the OrientDbRepository.
 */
public class OrientDbRepositoryImpl implements OrientDbRepository {
    private final OObjectDatabasePool server;
    private final WOrientConf conf;

    private Collection<ServiceRegistration> registrations = new ArrayList<>();

    private Collection<OrientDbCrud<?,?>> crudServices = new ArrayList<>();

    public OrientDbRepositoryImpl(WOrientConf conf){
        this.server = new OObjectDatabasePool(conf.getUrl(),conf.getUser(),conf.getPass());
        this.conf = conf;
    }

    public WOrientConf getConf(){
        return conf;
    }

    protected void addCrubService(Class entity){
        OObjectDatabaseTx db = server.acquire();
        db.getEntityManager().registerEntityClass(entity);
        db.close();

        crudServices.add(new OrientDbCrudService<Object>(this,entity));
    }

    protected void registerAllCrud(BundleContext context){
        for(OrientDbCrud crud: crudServices){
            Dictionary prop = conf.toDico();
            prop.put(Crud.ENTITY_CLASS_PROPERTY,crud.getEntityClass());
            prop.put(Crud.ENTITY_CLASSNAME_PROPERTY,crud.getEntityClass().getName());
            registrations.add(context.registerService(new String[]{Crud.class.getName(),OrientDbCrud.class.getName()},crud,prop));
        }
    }

    protected void destroy(){
        OEntityManager entityManager = server.acquire().getEntityManager();

        for(ServiceRegistration reg: registrations){
            reg.unregister();
        }

        for(Crud crud: crudServices){
            entityManager.deregisterEntityClass(crud.getEntityClass());
        }

        registrations.clear();
        crudServices.clear();

        server.close();
    }

    @Override
    public Collection<Crud<?, ?>> getCrudServices() {
        return (Collection) crudServices;
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