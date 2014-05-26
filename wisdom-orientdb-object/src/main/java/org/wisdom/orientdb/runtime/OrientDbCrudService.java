/*
 * Copyright 2014, Technologic Arts Vietnam.
 * All right reserved.
 */

package org.wisdom.orientdb.runtime;

import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.query.OQuery;
import com.orientechnologies.orient.core.tx.OTransaction;
import com.orientechnologies.orient.object.db.OObjectDatabasePool;
import com.orientechnologies.orient.object.db.OObjectDatabaseTx;
import org.wisdom.api.model.EntityFilter;
import org.wisdom.api.model.Repository;
import org.wisdom.orientdb.object.OrientDbCrud;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * CRUD Service Implementation using Orientdb ODatabaseObject.
 *
 * created: 5/9/14.
 *
 * @author <a href="mailto:jbardin@tech-arts.com">Jonathan M. Bardin</a>
 */
public class OrientDbCrudService<T> implements OrientDbCrud<T, String> {
    private OObjectDatabaseTx db;

    private final OrientDbRepository repo;

    private final Class<T> entityClass;


    private final OObjectDatabasePool pool;

    /**
     * Transaction type.
     */
    private final OTransaction.TXTYPE txtype;

    /**
     * Flag used in order to know if the instance is used during a transaction in the current thread.
     */
    private static final ThreadLocal<Boolean> transaction = new ThreadLocal<Boolean>(){
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };

    protected OrientDbCrudService(OrientDbRepository repo, Class<T> entityClass) {
        txtype = OTransaction.TXTYPE.OPTIMISTIC;
        this.repo = repo;
        pool = repo.get();
        this.entityClass = entityClass;
        transaction.set(false);
    }

    /**
     * Acquire the database connection from the pool.
     */
    private void acquire() {
        if (db == null || db.isClosed()) {
            db = pool.acquire();
        }
    }

    /**
     * Release the database connection to the pool
     */
    private void release() {
        if (!transaction.get()) {
            db.close();
        }
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
        acquire();
        try {
            db.delete(t);
        } finally {
            release();
        }
        return t;
    }

    @Override
    public void delete(String id) {
        ORecordId rid = new ORecordId(id);
        acquire();
        try {
            db.delete(rid);
        } finally {
            release();
        }
    }

    @Override
    public Iterable<T> delete(Iterable<T> ts) {
        acquire();
        List<T> deleted = new ArrayList<T>();
        try {
            for (T todel : ts) {
                deleted.add((T) db.delete(todel));
            }
        } finally {
            release();
        }
        return deleted;
    }

    @Override
    public T save(T t) {
        acquire();
        try {
            return db.save(t);
        } finally {
            release();
        }
    }

    @Override
    public Iterable<T> save(Iterable<T> ts) {
        List<T> saved = new ArrayList<T>();
        acquire();
        try {
            for (T tosave : ts) {
                saved.add((T) db.save(tosave));
            }
        } finally {
            release();
        }

        return saved;
    }

    @Override
    public T findOne(String id) {
        acquire();
        try {
            return db.load(new ORecordId(id));
        } finally {
            release();
        }
    }

    @Override
    public T findOne(final EntityFilter<T> tEntityFilter) {
        acquire();
        try {
            for (T entity : db.browseClass(entityClass)) {
                if (tEntityFilter.accept(entity)) {
                    return entity;
                }
            }
        } finally {
            release();
        }

        return null;
    }

    @Override
    public boolean exists(String id) {
        acquire();
        try {
            return db.existsUserObjectByRID(new ORecordId(id));
        } finally {
            release();
        }
    }

    @Override
    public Iterable<T> findAll() {
        acquire();
        try {
            return db.browseClass(entityClass);
        } finally {
            release();
        }
    }

    @Override
    public Iterable<T> findAll(Iterable<String> ids) {
        acquire();
        List<T> entities = new ArrayList<T>();
        try {
            for (String id : ids) {
                entities.add((T) db.load(new ORecordId(id)));
            }
        } finally {
            release();
        }

        return entities;
    }

    @Override
    public Iterable<T> findAll(EntityFilter<T> tEntityFilter) {
        acquire();
        List<T> entities = new ArrayList<T>();
        try {

            for (T entity : db.browseClass(entityClass)) {
                if (tEntityFilter.accept(entity)) {
                    entities.add(entity);
                }
            }
        } finally {
            release();
        }
        return entities;
    }

    @Override
    public List<T> query(OQuery<T> command, Object ... args){
        acquire();
        try {
            return db.query(command,args);
        }finally {
            release();
        }
    }

    @Override
    public T load(T pojo) {
        acquire();
        try {
            return db.load(pojo);
        }finally {
            release();
        }
    }

    @Override
    public T load(T pojo, String fetchPlan) {
        acquire();
        try {
            return db.load(pojo,fetchPlan);
        }finally {
            release();
        }
    }

    @Override
    public T detach(T pojo, Boolean returnNonProxyInstance) {
        acquire();
        try {
            return db.detach(pojo,returnNonProxyInstance);
        }finally {
            release();
        }
    }


    @Override
    public long count() {
        return db.countClass(entityClass.getSimpleName());
    }

    @Override
    public Repository getRepository() {
        return repo;
    }

    @Override
    public void executeTransactionalBlock(Runnable runnable) {
        transaction.set(true);
        acquire();
        db.begin(txtype);

        try {
            runnable.run();
            db.commit();
        } catch (Exception e) {
            db.rollback();
        } finally {
            transaction.set(false);
            release();
        }
    }

    @Override
    public <A> A executeTransactionalBlock(Callable<A> aCallable) {
        transaction.set(true);
        acquire();
        db.begin(txtype);
        try {
            A result = aCallable.call();
            db.commit();
            return result;
        } catch (Exception e) {
            db.rollback();
            return null;
        } finally {
            transaction.set(false);
            release();
        }
    }
}
