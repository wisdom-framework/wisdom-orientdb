package org.wisdom.orientdb.runtime;

import com.orientechnologies.orient.object.db.OObjectDatabasePool;
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
 */
class OrientDbRepositoryImpl implements OrientDbRepository {
    private final OObjectDatabasePool server;
    private final OrientDbRepoCommand repoCmd;

    private final Collection<ServiceRegistration> registrations = new ArrayList<>();

    private final Collection<OrientDbCrud<?,?>> crudServices = new ArrayList<>();

    OrientDbRepositoryImpl(OrientDbRepoCommand repoCmd){
        this.server = new OObjectDatabasePool(repoCmd.getConf().getUrl(),
                repoCmd.getConf().getUser(),
                repoCmd.getConf().getPass());
        this.repoCmd = repoCmd;
    }

    public WOrientConf getConf(){
        return repoCmd.getConf();
    }

    void registerAllCrud(BundleContext context){
        OObjectDatabaseTx db = server.acquire();

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
        OObjectDatabaseTx db = server.acquire();

        for(ServiceRegistration reg: registrations){
            reg.unregister();
        }

        repoCmd.destroy(db); //Call the OrientDbRepoCommand destroy callback

        registrations.clear();

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