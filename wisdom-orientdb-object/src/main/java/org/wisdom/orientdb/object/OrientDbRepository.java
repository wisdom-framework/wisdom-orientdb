package org.wisdom.orientdb.object;

import com.orientechnologies.orient.object.db.OObjectDatabasePool;
import org.wisdom.api.model.Crud;
import org.wisdom.api.model.Repository;

import java.util.Collection;

/**
 * The OrientDbRepository is a standard wisdom db Repository providing OrientDbCrud services.
 *
 * @See org.wisdom.api.model.Repository
 */
public interface OrientDbRepository extends Repository<OObjectDatabasePool> {

}
