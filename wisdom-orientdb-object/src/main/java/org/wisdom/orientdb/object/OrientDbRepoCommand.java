package org.wisdom.orientdb.object;

import com.orientechnologies.orient.object.db.OObjectDatabaseTx;
import org.wisdom.orientdb.conf.WOrientConf;

import java.util.List;

/**
 * The OrientDbRepositorPub allows to run command through the OObjectDatabaseTx before the OrientDbRepositoryHook
 * is registered.
 */
public interface OrientDbRepoCommand {

    WOrientConf getConf();

    List<Class<?>> getEntityClass();

    void init(OObjectDatabaseTx db);

    void destroy(OObjectDatabaseTx db);
}
