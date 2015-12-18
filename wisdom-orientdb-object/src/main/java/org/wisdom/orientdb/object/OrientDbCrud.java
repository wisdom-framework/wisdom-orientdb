package org.wisdom.orientdb.object;


import com.orientechnologies.orient.core.command.OCommandRequest;
import com.orientechnologies.orient.core.query.OQuery;
import org.wisdom.api.model.Crud;

import java.io.Serializable;
import java.util.List;

/**
 * The OrientDbCrud is a standard wisdom Crud service with some feature specific to Orientdb such as the possibility to
 * execute OQuery and load the entity from an instance.
 *
 * {@link org.wisdom.api.model.Crud}
 * @param <T> The type of the entity
 * @param <I> The type of the id. ( either Long, Object or String )
 */
public interface OrientDbCrud<T,I extends Serializable> extends Crud<T,I> {

    /**
     * Release the current database connection to the connection pool. Do nothing if the db connection has already been
     * released.
     */
    void release();

    /**
     * Run an SQl query.
     * {@link "http://github.com/orientechnologies/orientdb/wiki/SQL-Query"}
     *
     * example:
     *  <code>
     *  List<T> result = crud.query(
     *  new OSQLSynchQuery<T>("select * from "+T.simpleName()+" where ID = 10 and name like 'G%'"));
     *  </code>
     *
     * {@link com.orientechnologies.orient.object.db.OObjectDatabaseTx#query}
     * @param command The Sql command to run
     * @param args The arguments
     * @return The result of the request as a list of entities.
     */
    List<T> query(OQuery<T> command, Object ... args);

    /**
     * Execute an {@link OCommandRequest} passing the optional argument {@code args} and returns an Object.
     * {@link "http://orientdb.com/javadoc/latest/com/orientechnologies/orient/core/command/OCommandRequest.html"}
     * <br/>
     * example:
     *  <code>
     *      crud.execute(new OCommandSQL("CREATE CLASS MY_CLASS"));
     *  </code>
     *
     * @param command The OCommandRequest to run
     * @param args The optional arguments
     * @param <RET> The return object type
     * @return The result of the OCommandRequest execution.
     */
    <RET> RET execute(OCommandRequest command, Object ... args);

    /**
     * Load the given entity from the db and return it.
     *
     * {@link com.orientechnologies.orient.object.db.OObjectDatabaseTx#load(Object)}
     * @param entity The entity to load from the db.
     * @return The attached version of the entity.
     */
    T load(T entity);

    /**
     * Load the given entity from the db using the given fetchPlan and return it.
     *
     * {@link com.orientechnologies.orient.object.db.OObjectDatabaseTx#load(Object)}
     * @param entity The entity to load from the db.
     * @param fetchPlan The fetchPlan used to retrieve the entity.
     * @return The attached version of the entity
     */
    T load(T entity, String fetchPlan);

    /**
     * Attached the given entity to the db. In this way all changes done within the object without using setters
     * will be copied to the document.
     *
     * {@link com.orientechnologies.orient.object.db.OObjectDatabaseTx#attach(Object)}
     * @param entity The entity to be attached.
     */
    void attach(T entity);

    /**
     * Detached an entity from the database. All of the entity field can now be modified without impact on the db.
     *
     * {@link com.orientechnologies.orient.object.db.OObjectDatabaseTx#detach(Object)}
     * @param attachedEntity The entity that needs to be detached
     * @return A detached version of the entity (a proxy instance).
     */
    T detach(T attachedEntity);

    /**
     * Detached an entity from the database. All of the entity field can now be modified without impact on the db.
     *
     * {@link com.orientechnologies.orient.object.db.OObjectDatabaseTx#detach(Object, boolean)}
     * @param attachedEntity The entity that needs to be detached
     * @param returnNonProxyInstance <code>true</code> if you want the returned entity to be a plain object,
     *                               and not a proxy.
     * @return A detached version of the entity.
     */
    T detach(T attachedEntity,Boolean returnNonProxyInstance);


    /**
     * @return This OrientDbCrud service associated repository,
     */
    OrientDbRepository getRepository();
}
