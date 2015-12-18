package org.wisdom.orientdb.runtime;

import com.orientechnologies.orient.core.command.OCommandRequest;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.query.OQuery;
import com.orientechnologies.orient.object.db.OObjectDatabaseTx;
import org.wisdom.api.model.EntityFilter;
import org.wisdom.api.model.FluentTransaction;
import org.wisdom.api.model.HasBeenRollBackException;
import org.wisdom.api.model.TransactionManager;
import org.wisdom.orientdb.object.OrientDbCrud;
import org.wisdom.orientdb.object.OrientDbRepository;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.StreamSupport.stream;

/**
 * CRUD Service Implementation which delegates to Orientdb {@link OObjectDatabaseTx}.
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
     *
     * @return An  OObjectDatabaseTx db
     */
    private OObjectDatabaseTx acquireDb() {
        return txManager.acquireDb();
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
        callAndCloseDb(db -> db.delete(t));
        return t;
    }

    @Override
    public void delete(String id) {
        callAndCloseDb(db -> db.delete(new ORecordId(id)));
    }

    @Override
    public Iterable<T> delete(Iterable<T> ts) {
        return callAndCloseDb(db -> {
            ts.forEach((elem) -> db.delete(elem));
            return ts;
        });
    }

    @Override
    public T save(T t) {
        return callAndCloseDb(db -> db.save(t));
    }

    @Override
    public Iterable<T> save(Iterable<T> ts) {
        return callAndCloseDb(db ->
                stream(ts.spliterator(), false).map(elem -> (T) db.save(elem))
                        .collect(Collectors.toList())
        );
    }

    @Override
    public T findOne(String id) {
        return callAndCloseDb(db -> (T) db.load(new ORecordId(id)));
    }

    @Override
    public T findOne(final EntityFilter<T> entityFilter) throws NoSuchElementException {
        return callAndCloseDb(db ->
                stream(db.browseClass(entityClass).spliterator(), false)
                        .filter((entity) -> entityFilter.accept(entity)).findFirst().orElse(null)
        );
    }

    @Override
    public boolean exists(String id) {
        return callAndCloseDb(db -> db.existsUserObjectByRID(new ORecordId(id)));
    }

    @Override
    public Iterable<T> findAll() {
        return callAndCloseDb(db -> db.browseClass(entityClass));
    }

    @Override
    public Iterable<T> findAll(Iterable<String> ids) {
        return callAndCloseDb((db) ->
                stream(ids.spliterator(), false).map(id -> (T) db.load(new ORecordId(id)))
                        .collect(Collectors.toList())
        );
    }

    @Override
    public Iterable<T> findAll(EntityFilter<T> entityFilter) {
        return callAndCloseDb(db ->
                stream(db.browseClass(entityClass).spliterator(), false)
                        .filter((entity) -> entityFilter.accept(entity)).collect(Collectors.toList())
        );
    }

    @Override
    public void release() {
        txManager.releaseDb();
    }

    @Override
    public List<T> query(OQuery<T> command, Object... args) {
        return callAndCloseDb(db -> db.query(command, args));
    }

    @Override
    public <RET> RET execute(OCommandRequest command, Object... args) {
        return callAndCloseDb(db -> db.command(command).execute(args));
    }

    @Override
    public T load(T entity) {
        return callAndCloseDb(db -> db.load(entity));
    }

    @Override
    public T load(T entity, String fetchPlan) {
        return callAndCloseDb(db -> db.load(entity, fetchPlan));
    }

    @Override
    public void attach(T entity) {
        acquireDb().attach(entity);
    }

    @Override
    public T detach(T attachedEntity) {
        return callAndCloseDb(db -> db.detach(attachedEntity));
    }

    @Override
    public T detach(T entity, Boolean returnNonProxyInstance) {
        return callAndCloseDb(db -> db.detach(entity, returnNonProxyInstance));
    }


    @Override
    public long count() {
        return callAndCloseDb(db -> db.countClass(entityClass.getSimpleName()));
    }

    @Override
    public OrientDbRepository getRepository() {
        return repo;
    }

    @Override
    public void executeTransactionalBlock(final Runnable runnable) throws HasBeenRollBackException {
        txManager.begin();

        try {
            runnable.run();
            txManager.commit();
        } catch (Exception e) {
            txManager.rollback();
            throw new HasBeenRollBackException(e);
        } finally {
            txManager.close();
        }
    }

    @Override
    public <A> A executeTransactionalBlock(Callable<A> aCallable) throws HasBeenRollBackException {
        txManager.begin();

        try {
            A ret = aCallable.call();
            txManager.commit();
            return ret;
        } catch (Exception e) {
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

    /**
     * Aquire the enclosed database, apply the given function and release the db to the pool.
     *
     * @param dbfunc The function to apply
     * @param <R>    the return type of {@code dbfunc}
     * @return the return value of calling {@code dbfunc}
     */
    private <R> R callAndCloseDb(Function<OObjectDatabaseTx, R> dbfunc) {
        try {
            return dbfunc.apply(acquireDb());
        } finally {
            txManager.releaseDb();
        }
    }
}
