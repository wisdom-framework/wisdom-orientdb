/*
 * Copyright 2014, Technologic Arts Vietnam.
 * All right reserved.
 */

package org.wisdom.orientdb.runtime;

import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.object.db.OObjectDatabaseTx;
import org.wisdom.api.model.Crud;
import org.wisdom.api.model.EntityFilter;
import org.wisdom.api.model.Repository;

import java.util.concurrent.Callable;

/**
 * CRUD Service Implementation using Orientdb ODatabaseObject.
 *
 * created: 5/9/14.
 *
 * @author <a href="mailto:jbardin@tech-arts.com">Jonathan M. Bardin</a>
 */
public class OrientDbCrudService<T> implements Crud<T,String> {
    private final OObjectDatabaseTx db;

    private final Class<T> entityClass;

    public OrientDbCrudService(OObjectDatabaseTx objectDb, Class<T> entityClass) {
        db = objectDb;
        this.entityClass = entityClass;
        //Should we register the class if not present ?
        db.getEntityManager().registerEntityClass(entityClass);
    }

    @Override
    public Class<T> getEntityClass() {
        return entityClass;
    }

    @Override
    public Class<String> getIdClass() {
        return String.class;
    }

    @Override
    public T delete(T t) {
        db.delete(t);
        return t;
    }

    @Override
    public void delete(String id) {
        ORecordId rid = new ORecordId(id);
        db.delete(rid);
    }

    @Override
    public Iterable<T> delete(Iterable<T> ts) {
        for(T todel : ts){
            db.delete(todel);
        }
        return ts;
    }

    @Override
    public T save(T t) {
        return db.save(t);
    }

    @Override
    public Iterable<T> save(Iterable<T> ts) {
        for(T tosave : ts){
            db.save(tosave);
        }

        return ts;
    }

    @Override
    public T findOne(String id) {
        return db.load(new ORecordId(id));
    }

    @Override
    public T findOne(EntityFilter<T> tEntityFilter) {
        return null;
    }

    @Override
    public boolean exists(String id) {
        return false;
    }

    @Override
    public Iterable<T> findAll() {
        return db.browseClass(entityClass);
    }

    @Override
    public Iterable<T> findAll(Iterable<String> ids) {
        return null;
    }

    @Override
    public Iterable<T> findAll(EntityFilter<T> tEntityFilter) {
        return null;
    }

    @Override
    public long count() {
        return db.countClass(entityClass.getSimpleName());
    }

    @Override
    public Repository getRepository() {
        return null;
    }

    @Override
    public void executeTransactionalBlock(Runnable runnable) {
        db.begin();

        try {
            runnable.run();
            db.commit();
        } catch (Exception e) {
            db.rollback();
        } finally {
            //db.close();
        }
    }

    @Override
    public <A> A executeTransactionalBlock(Callable<A> aCallable) {
        db.begin();
        try {
            A result = aCallable.call();
            db.commit();
            return result;
        } catch (Exception e) {
            db.rollback();
            return null;
        } finally {
            //db.close();
        }
    }
}
