package org.wisdom.orientdb.object;

import com.orientechnologies.orient.core.db.OPartitionedDatabasePool;
import com.orientechnologies.orient.object.db.OObjectDatabaseTx;
import org.wisdom.api.model.Repository;
import org.wisdom.orientdb.conf.WOrientConf;

/**
 * The OrientDbRepository is a standard wisdom db Repository providing OrientDbCrud services.
 *
 * {@link org.wisdom.api.model.Repository}
 */
public interface OrientDbRepository extends Repository<OPartitionedDatabasePool> {

    /**
     * <p>
     * Acquire {@link OObjectDatabaseTx} instance linked to this repository.
     * The return instance is attached to the local thread.
     * </p>
     * <p>
     * It must be closed to te release to the pool, it support try with resource
     * since {@link OObjectDatabaseTx} implements {@link AutoCloseable}.
     * </p>
     *
     * @return An active {@link OObjectDatabaseTx} instance from this repository pool.
     */
    OObjectDatabaseTx acquireDb();

    /**
     * @return This repository {@link WOrientConf} configuration.
     */
    WOrientConf getConf();
}
