package org.wisdom.orientdb.object;

import com.orientechnologies.orient.object.db.OObjectDatabasePool;
import org.wisdom.api.model.Repository;

/**
 * The OrientDbRepository is a standard wisdom db Repository providing OrientDbCrud services.
 *
 * {@link org.wisdom.api.model.Repository}
 */
public interface OrientDbRepository extends Repository<OObjectDatabasePool> {

}
