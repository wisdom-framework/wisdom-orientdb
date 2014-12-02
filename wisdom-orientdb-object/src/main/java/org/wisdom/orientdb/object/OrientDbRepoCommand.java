package org.wisdom.orientdb.object;

import com.orientechnologies.orient.object.db.OObjectDatabaseTx;
import org.wisdom.orientdb.conf.WOrientConf;

import java.util.List;

/**
 * The OrientDbRepoCommand service will be used as incentive to provides a new {@link OrientDbRepository} and {@link org.wisdom.orientdb.object.OrientDbCrud} services
 * corresponding to its configuration {@link #getConf()}.
 *
 * If the OrientDbRepoCommand configuration is valid, a {@link org.wisdom.orientdb.object.OrientDbCrud} service will be published for each entity present in the
 * {@link #getEntityClass()} list.
 *
 * In addition to
 */
public interface OrientDbRepoCommand {

    /**
     * @return The {@link OrientDbRepository} configuration that we want to create.
     */
    WOrientConf getConf();

    /**
     * @return The entities class that we want to publish as {@link org.wisdom.orientdb.object.OrientDbCrud}
     */
    List<Class<?>> getEntityClass();

    /**
     * A hook that will be call before the creation of the OrientDbRepository and the OrientDbCrud services.
     * @param db The database instance that contains the entities.
     */
    void init(OObjectDatabaseTx db);

    /**
     * A hook that will be call after the destruction/de-registration of the OrientDbCrud services
     * and their OrientDbRepository.
     * @param db The database instance that contains the entities.
     */
    void destroy(OObjectDatabaseTx db);
}
