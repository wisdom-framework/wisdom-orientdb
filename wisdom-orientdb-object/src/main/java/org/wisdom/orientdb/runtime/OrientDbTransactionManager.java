package org.wisdom.orientdb.runtime;

import com.orientechnologies.orient.object.db.OObjectDatabaseTx;
import org.wisdom.api.model.InitTransactionException;
import org.wisdom.api.model.RollBackHasCauseAnException;
import org.wisdom.api.model.TransactionManager;
import org.wisdom.orientdb.object.OrientDbRepository;

/**
 * Implementation of {@link TransactionManager} that delegates to the {@link OObjectDatabaseTx}.
 * Each transaction is associated with the current thread.
 */
class OrientDbTransactionManager implements TransactionManager{

    private final OrientDbRepository repo;

    private OObjectDatabaseTx db;

    /**
     * Flag used in order to know if the instance is used during a transaction in the current thread.
     */
    private static final ThreadLocal<Boolean> transaction = new ThreadLocal<Boolean>(){
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };


    OrientDbTransactionManager(OrientDbRepositoryImpl repo) {
        this.repo=repo;
        transaction.set(false);
    }

    OObjectDatabaseTx acquireDb(){
        if(db == null || db.isClosed()){
            db = repo.acquireDb();
        }
        db.activateOnCurrentThread();

        return db;
    }

    void releaseDb(){
        if(!transaction.get()){
            db.close();
        }
    }

    @Override
    public void begin() throws InitTransactionException {
        if (transaction.get()){
            throw new InitTransactionException("The transaction has already been started in this thread.");
        }

        try{
            acquireDb().begin(repo.getConf().getTxType());
            transaction.set(true);
        }catch (Throwable t){
            throw new InitTransactionException(t);
        }

    }

    @Override
    public void commit() throws Exception {
        if(!transaction.get()){
            throw new IllegalStateException("No transaction has been begin in this thread.");
        }
        db.commit();
    }

    @Override
    public void rollback() throws RollBackHasCauseAnException {
        if(!transaction.get()){
            throw new RollBackHasCauseAnException("No transaction has been started in this thread.");
        }

        try{
            db.rollback();
        }catch (Throwable t){
            throw new RollBackHasCauseAnException(t);
        }
    }

    @Override
    public void close() {
        if(db!=null){
            db.close();
        }
        transaction.set(false);
    }
}
