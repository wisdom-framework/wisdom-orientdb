package org.wisdom.orientdb.object;


import com.orientechnologies.orient.core.query.OQuery;
import org.wisdom.api.model.Crud;

import java.io.Serializable;
import java.util.List;

/**
 * The OrientDbCrud is a standard wisdom Crud service with some feature specific to Orientdb such as the possibility to
 * execute OQuery and load the entity from an instance.
 *
 * @See   org.wisdom.api.model.Crud
 * @param <T> The type of the entity
 * @param <I> The type of the id. ( either Long, Object or String )
 */
public interface OrientDbCrud<T,I extends Serializable> extends Crud<T,I>{

    /**
     * Run an SQl query.
     * {@link "http://github.com/orientechnologies/orientdb/wiki/SQL-Query"}
     *
     * example:
     *  List<T> result = crud.query(
     *  new OSQLSynchQuery<T>("select * from "+T.simpleName()+" where ID = 10 and name like 'G%'"));
     *
     * @See com.orientechnologies.orient.object.db.OObjectDatabaseTx#query
     * @param command The Sql command to run
     * @param args the arguments
     * @return The result of the request as a list of entities.
     */
    List<T> query(OQuery<T> command, Object ... args);

    T load(T object);

    T load(T object, String fetchPlan);

    T detach(T object,Boolean returnNonProxyInstance);
}
