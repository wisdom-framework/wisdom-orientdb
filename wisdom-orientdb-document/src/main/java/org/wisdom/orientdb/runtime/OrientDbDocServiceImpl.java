package org.wisdom.orientdb.runtime;

import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal;
import com.orientechnologies.orient.core.db.OPartitionedDatabasePool;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.wisdom.orientdb.conf.WOrientConf;
import org.wisdom.orientdb.document.OrientDbDocumentCommand;
import org.wisdom.orientdb.document.OrientDbDocumentService;

/**
 * Implementation of the {@link org.wisdom.orientdb.document.OrientDbDocumentService}.
 */
class OrientDbDocServiceImpl implements OrientDbDocumentService {
    private final OPartitionedDatabasePool pool;
    private final OrientDbDocumentCommand repoCmd;

    private ServiceRegistration<OrientDbDocumentService> registration;

    OrientDbDocServiceImpl(OPartitionedDatabasePool pool, OrientDbDocumentCommand repoCmd){
        this.pool = pool;
        this.repoCmd = repoCmd;
    }

    public WOrientConf getConf(){
        return repoCmd.getConf();
    }


    void destroy(){
        if(registration == null){
            throw new IllegalStateException("The OrientDbDocRepoImpl service must be register before calling destroy.");
        }
        registration.unregister();

        try (ODatabaseDocumentTx db = acquire();) {
            repoCmd.destroy(db); //Call the OrientDbDocumentCommand destroy callback
        }
    }

    void register(BundleContext context) {
        try(ODatabaseDocumentTx db = pool.acquire();){
            repoCmd.init(db); //Call the OrientDbDocumentCommand init callback
        }

        registration = context.registerService(OrientDbDocumentService.class, this, repoCmd.getConf().toDico());
    }


    @Override
    public String getUrl() {
        return pool.getUrl();
    }

    @Override
    public String getAlias() {
        return repoCmd.getConf().getAlias();
    }

    @Override
    public ODatabaseDocumentTx acquire() {
        ODatabaseDocumentTx db = pool.acquire();
        ODatabaseRecordThreadLocal.INSTANCE.set(db);
        return db;
    }
}