package org.wisdom.orientdb.runtime;

import com.orientechnologies.common.profiler.OProfiler;
import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.command.script.OScriptManager;
import com.orientechnologies.orient.core.conflict.ORecordConflictStrategyFactory;
import com.orientechnologies.orient.core.db.OPartitionedDatabasePoolFactory;
import com.orientechnologies.orient.core.engine.OEngine;
import com.orientechnologies.orient.core.storage.OStorage;
import org.apache.felix.ipojo.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wisdom.api.configuration.ApplicationConfiguration;
import org.wisdom.orientdb.manager.OrientDbManager;

import java.util.Collection;
import java.util.Date;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * This component start an embedded {@link Orient} instance when starting and shut it down when it stops. <br/>
 *
 * It also provides an {@link OrientDbManager} service that allows for controlling the instance.
 *
 * Delegate to the {@link Orient} default instance.
 *
 */
@Component(name = OrientDbManagerComp.COMPONENT_NAME)
@Instantiate(name = OrientDbManagerComp.INSTANCE_NAME)
@Provides(specifications = OrientDbManager.class)
class OrientDbManagerComp implements OrientDbManager {
    static final String COMPONENT_NAME = "wisdom:orientdb:manager:factory";
    static final String INSTANCE_NAME = "wisdom:orientdb:manager";


    @Requires
    private ApplicationConfiguration appConf;

    private OPartitionedDatabasePoolFactory poolFactory;

    private final Logger logger = LoggerFactory.getLogger(OrientDbManagerComp.class);

    @Validate
    private void start(){

        //Start the orientdb instance
        logger.info("Starting Orient instance.");
        getInstance().startup(); //no need to check if active (it's done in startup)

        //Create the db pool factory
        poolFactory = new OPartitionedDatabasePoolFactory();

        //Set default poolmax
        poolFactory.setMaxPoolSize(appConf.getIntegerWithDefault("orientdb.poolmax",64));

        //remove the hook since we handle shutdown in the stop callback
        getInstance().removeShutdownHook();


    }

    @Invalidate
    private void stop() {
        logger.info("Shutting down Orient instance.");

        poolFactory.close();
        getInstance().shutdown(); //no need to check if active (it's done in shutdown)
    }

    /**
     * @return The default Orient instance.
     */
    private static Orient getInstance(){
        return Orient.instance();
    }



    /**
     * @return The partitioned database pool factory.
     */
    @Override
    public OPartitionedDatabasePoolFactory getDatabasePoolFactory(){
        return poolFactory;
    }

    //
    // Implement the OrientDbManager service, delegate to getInstance().
    //

    @Override
    public ORecordConflictStrategyFactory getRecordConflictStrategy() {
        return getInstance().getRecordConflictStrategy();
    }

    @Override
    public void scheduleTask(TimerTask task, long delay, long period) {
        getInstance().scheduleTask(task, delay, period);
    }

    @Override
    public void scheduleTask(TimerTask task, Date firstTime, long period) {
        getInstance().scheduleTask(task, firstTime, period);
    }

    @Override
    public Future<?> submit(Runnable runnable) {
        return getInstance().submit(runnable);
    }

    @Override
    public <V> Future<V> submit(Callable<V> callable) {
        return getInstance().submit(callable);
    }

    @Override
    public OStorage loadStorage(String iURL) {
        return getInstance().loadStorage(iURL);
    }

    @Override
    public OStorage getStorage(String dbName) {
        return getInstance().getStorage(dbName);
    }

    @Override
    public void registerEngine(OEngine iEngine) {
        getInstance().registerEngine(iEngine);
    }

    @Override
    public OEngine getEngine(String engineName) {
        return getInstance().getEngine(engineName);
    }

    @Override
    public Set<String> getEngines() {
        return getInstance().getEngines();
    }

    @Override
    public void unregisterStorageByName(String name) {
        getInstance().unregisterStorageByName(name);
    }

    @Override
    public void unregisterStorage(OStorage storage) {
        getInstance().unregisterStorage(storage);
    }

    @Override
    public Collection<OStorage> getStorages() {
        return getInstance().getStorages();
    }

    @Override
    public OProfiler getProfiler() {
        return getInstance().getProfiler();
    }

    @Override
    public void setProfiler(OProfiler iProfiler) {
        getInstance().setProfiler(iProfiler);
    }

    @Override
    public OScriptManager getScriptManager() {
        return getInstance().getScriptManager();
    }

    @Override
    public Boolean isActive() {
        return getInstance().isActive();
    }
}
