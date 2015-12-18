package org.wisdom.orientdb.runtime;

import com.orientechnologies.orient.core.db.OPartitionedDatabasePool;
import com.orientechnologies.orient.object.db.OObjectDatabaseTx;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.wisdom.api.model.Crud;
import org.wisdom.orientdb.conf.WOrientConf;
import org.wisdom.orientdb.object.OrientDbCrud;
import org.wisdom.orientdb.object.OrientDbRepoCommand;
import org.wisdom.orientdb.object.OrientDbRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;

/**
 * Implementation of the OrientDbRepository.
 *
 */
class OrientDbRepositoryImpl implements OrientDbRepository {
    private final OPartitionedDatabasePool server;
    private final OrientDbRepoCommand repoCmd;

    private final Collection<ServiceRegistration> registrations = new ArrayList<>();

    private final Collection<OrientDbCrud<?,?>> crudServices = new ArrayList<>();

    OrientDbRepositoryImpl(OPartitionedDatabasePool server, OrientDbRepoCommand repoCmd){
        this.server = server;
        this.repoCmd = repoCmd;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WOrientConf getConf(){
        return repoCmd.getConf();
    }

    /**
     * Acquire {@link OObjectDatabaseTx} instance linked to this repository.
     *
     * @return An active {@link OObjectDatabaseTx} instance from this repository pool.
     */
    @Override
    public OObjectDatabaseTx acquireDb() {
        OObjectDatabaseTx db = new OObjectDatabaseTx(server.acquire());
        db.setLazyLoading(getConf().getAutolazyloading());
        return db;
    }

    void registerAllCrud(BundleContext context){
        OObjectDatabaseTx db =  acquireDb();

        repoCmd.init(db); //Call the OrientDbRepoCommand init callback

        for(Class entity: repoCmd.getEntityClass()){
            //Service properties
            Dictionary prop = getConf().toDico();
            prop.put(Crud.ENTITY_CLASS_PROPERTY,entity);
            prop.put(Crud.ENTITY_CLASSNAME_PROPERTY,entity.getName());

            registrations.add(context.registerService(new String[]{Crud.class.getName(),OrientDbCrud.class.getName()},
                    new OrientDbCrudService(this,entity),
                    prop));
        }

        db.close();
    }

    void destroy(){
        for(ServiceRegistration reg: registrations){
            reg.unregister();
        }

        try(
            OObjectDatabaseTx db = new OObjectDatabaseTx(server.acquire());
        ) {
            repoCmd.destroy(db); //Call the OrientDbRepoCommand destroy callback
        }

        //we don't close the pool in case it will be reuse
        registrations.clear();
    }

    @Override
    public Collection<Crud<?, ?>> getCrudServices() {
        return (Collection) crudServices;
    }

    @Override
    public String getName() {
        return server.getUrl();
    }

    @Override
    public String getType() {
        return "orientdb-partitioned-pool";
    }

    @Override
    public Class<OPartitionedDatabasePool> getRepositoryClass() {
        return OPartitionedDatabasePool.class;
    }

    @Override
    public OPartitionedDatabasePool get() {
        return server;
    }
}