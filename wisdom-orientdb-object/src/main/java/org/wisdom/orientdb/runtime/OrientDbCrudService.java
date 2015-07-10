package org.wisdom.orientdb.runtime;

import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.query.OQuery;
import com.orientechnologies.orient.object.db.OObjectDatabaseTx;
import org.wisdom.api.model.EntityFilter;
import org.wisdom.api.model.FluentTransaction;
import org.wisdom.api.model.HasBeenRollBackException;
import org.wisdom.api.model.TransactionManager;
import org.wisdom.orientdb.object.OrientDbCrud;
import org.wisdom.orientdb.object.OrientDbRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * CRUD Service Implementation using Orientdb ODatabaseObject.
 *
 * created: 5/9/14.
 *
 * @author barjo
 */
class OrientDbCrudService<T> implements OrientDbCrud<T, String> {
    private final OrientDbTransactionManager txManager;

    private final OrientDbRepository repo;

    private final Class<T> entityClass;


    OrientDbCrudService(OrientDbRepositoryImpl repo, Class<T> entityClass) {
        this.repo = repo;
        this.txManager = new OrientDbTransactionManager(repo);
        this.entityClass = entityClass;
    }

    /**
     * Ask the Transaction manager to give us a db, if we are in a transaction running on the local thread,
     * the existing db is returned, otherwise an db is retrieved from the pool.
     * @return An  OObjectDatabaseTx db
     */
    private OObjectDatabaseTx acquireDb(){
        return txManager.acquireDb();
    }

    /**
     * Release the database connection to the pool
     */
    private void releaseDb() {
        txManager.releaseDb();
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
        try {
            acquireDb().delete(t);
        } finally {
            releaseDb();
        }
        return t;
    }

    @Override
    public void delete(String id) {
        ORecordId rid = new ORecordId(id);
        try {
            acquireDb().delete(rid);
        } finally {
            releaseDb();
        }
    }

    @Override
    public Iterable<T> delete(Iterable<T> ts) {
        OObjectDatabaseTx db = acquireDb();
        List<T> deleted = new ArrayList<>();

        try {
            for (T todel : ts) {
                deleted.add((T) db.delete(todel));
            }
        } finally {
            releaseDb();
        }

        return deleted;
    }

    @Override
    public T save(T t) {
        try {
            return acquireDb().save(t);
        } finally {
            releaseDb();
        }
    }

    @Override
    public Iterable<T> save(Iterable<T> ts) {
        List<T> saved = new ArrayList<>();
        OObjectDatabaseTx db = acquireDb();

        try {
            for (T tosave : ts) {
                saved.add((T) db.save(tosave));
            }
        } finally {
            releaseDb();
        }

        return saved;
    }

    @Override
    public T findOne(String id) {
        try {
            return acquireDb().load(new ORecordId(id));
        } finally {
            releaseDb();
        }
    }

    @Override
    public T findOne(final EntityFilter<T> tEntityFilter) {
        OObjectDatabaseTx db = acquireDb();

        try {
            for (T entity : db.browseClass(entityClass)) {
                if (tEntityFilter.accept(entity)) {
                    return entity;
                }
            }
        } finally {
            releaseDb();
        }

        return null;
    }

    @Override
    public boolean exists(String id) {
        try {
            return acquireDb().existsUserObjectByRID(new ORecordId(id));
        } finally {
            releaseDb();
        }
    }

    @Override
    public Iterable<T> findAll() {
        try {
            return acquireDb().browseClass(entityClass);
        } finally {
            releaseDb();
        }
    }

    @Override
    public Iterable<T> findAll(Iterable<String> ids) {
        OObjectDatabaseTx db = acquireDb();
        List<T> entities = new ArrayList<>();

        try {
            for (String id : ids) {
                entities.add((T) db.load(new ORecordId(id)));
            }
        } finally {
            releaseDb();
        }

        return entities;
    }

    @Override
    public Iterable<T> findAll(EntityFilter<T> tEntityFilter) {
        OObjectDatabaseTx db = acquireDb();
        List<T> entities = new ArrayList<>();

        try {

            for (T entity : db.browseClass(entityClass)) {
                if (tEntityFilter.accept(entity)) {
                    entities.add(entity);
                }
            }
        } finally {
            releaseDb();
        }
        return entities;
    }

    @Override
    public List<T> query(OQuery<T> command, Object ... args){
        try {
            return acquireDb().query(command, args);
        }finally {
            releaseDb();
        }
    }

    @Override
    public T load(T entity) {
        try {
            return acquireDb().load(entity);
        }finally {
            releaseDb();
        }
    }

    @Override
    public T load(T entity, String fetchPlan) {
        try {
            return acquireDb().load(entity, fetchPlan);
        }finally {
            releaseDb();
        }
    }

    @Override
    public void attach(T entity) {
        try {
            acquireDb().attach(entity);
        }finally {
            releaseDb();
        }
    }

    @Override
    public T detach(T attachedEntity) {
        try {
            return acquireDb().detach(attachedEntity);
        }finally {
            releaseDb();
        }
    }

    @Override
    public T detach(T entity, Boolean returnNonProxyInstance) {
        try {
            return acquireDb().detach(entity, returnNonProxyInstance);
        }finally {
            releaseDb();
        }
    }


    @Override
    public long count() {
        return acquireDb().countClass(entityClass.getSimpleName());
    }

    @Override
    public OrientDbRepository getRepository() {
        return repo;
    }

    @Override
    public void executeTransactionalBlock(final Runnable runnable) throws HasBeenRollBackException{
        txManager.begin();

        try{
            runnable.run();
            txManager.commit();
        }catch (Exception e){
            txManager.rollback();
            throw new HasBeenRollBackException(e);
        } finally {
            txManager.close();
        }
    }

    @Override
    public <A> A executeTransactionalBlock(Callable<A> aCallable) throws HasBeenRollBackException{
        txManager.begin();

        try{
            A ret = aCallable.call();
            txManager.commit();
            return ret;
        }catch (Exception e){
            txManager.rollback();
            throw new HasBeenRollBackException(e);
        } finally {
            txManager.close();
        }
    }

    @Override
    public TransactionManager getTransactionManager() {
        return txManager;
    }

    @Override
    public <R> FluentTransaction<R> transaction() {
        return FluentTransaction.transaction(txManager);
    }

    @Override
    public <R> FluentTransaction.Intermediate transaction(Callable<R> callable) {
        return FluentTransaction.transaction(txManager).with(callable);
    }

}
