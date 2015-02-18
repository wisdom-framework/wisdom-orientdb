package org.wisdom.orientdb.runtime;

import com.orientechnologies.common.profiler.OProfilerMBean;
import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.command.script.OScriptManager;
import com.orientechnologies.orient.core.conflict.ORecordConflictStrategyFactory;
import com.orientechnologies.orient.core.engine.OEngine;
import com.orientechnologies.orient.core.storage.OStorage;
import org.apache.felix.ipojo.annotations.*;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wisdom.api.configuration.ApplicationConfiguration;
import org.wisdom.orientdb.manager.OrientDbManager;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * Wrapper around {@link Orient}
 * @author barjo
 */
@Component(name = OrientDbManagerComp.COMPONENT_NAME)
@Instantiate(name = OrientDbManagerComp.INSTANCE_NAME)
@Provides(specifications = OrientDbManager.class)
class OrientDbManagerComp implements OrientDbManager {
    static final String COMPONENT_NAME = "wisdom:orientdb:manager:factory";
    static final String INSTANCE_NAME = "wisdom:orientdb:manager";


    @Requires
    private ApplicationConfiguration appConf;

    private final Logger logger = LoggerFactory.getLogger(OrientDbManagerComp.class);

    private final BundleContext context;

    private final Orient orient = Orient.instance();

    public OrientDbManagerComp(BundleContext bundleContext) {
        context = bundleContext;
    }

    @Validate
    private void start(){
        //Start the orientdb instance
        if(!orient.isActive()) {
            logger.info("Starting Orient instance.");
            orient.startup();
        }

        //remove the hook since we handle shutdown in the stop callback
        Orient.instance().removeShutdownHook();
    }

    @Invalidate
    private void stop() {
        if (orient.isActive()) {
            logger.info("Shutting down Orient instance.");
            orient.shutdown();
        }
    }

    @Override
    public ORecordConflictStrategyFactory getRecordConflictStrategy() {
        return orient.getRecordConflictStrategy();
    }

    @Override
    public void scheduleTask(TimerTask task, long delay, long period) {
        orient.scheduleTask(task, delay, period);
    }

    @Override
    public void scheduleTask(TimerTask task, Date firstTime, long period) {
        orient.scheduleTask(task, firstTime, period);
    }

    @Override
    public Future<?> submit(Runnable runnable) {
        return orient.submit(runnable);
    }

    @Override
    public <V> Future<V> submit(Callable<V> callable) {
        return orient.submit(callable);
    }

    @Override
    public OStorage loadStorage(String iURL) {
        return orient.loadStorage(iURL);
    }

    @Override
    public OStorage registerStorage(OStorage storage) throws IOException {
        return orient.registerStorage(storage);
    }

    @Override
    public OStorage getStorage(String dbName) {
        return orient.getStorage(dbName);
    }

    @Override
    public void registerEngine(OEngine iEngine) {
        orient.registerEngine(iEngine);
    }

    @Override
    public OEngine getEngine(String engineName) {
        return orient.getEngine(engineName);
    }

    @Override
    public Set<String> getEngines() {
        return orient.getEngines();
    }

    @Override
    public void unregisterStorageByName(String name) {
        orient.unregisterStorageByName(name);
    }

    @Override
    public void unregisterStorage(OStorage storage) {
        orient.unregisterStorage(storage);
    }

    @Override
    public Collection<OStorage> getStorages() {
        return orient.getStorages();
    }

    @Override
    public OProfilerMBean getProfiler() {
        return orient.getProfiler();
    }

    @Override
    public void setProfiler(OProfilerMBean iProfiler) {
        orient.setProfiler(iProfiler);
    }

    @Override
    public OScriptManager getScriptManager() {
        return orient.getScriptManager();
    }

    @Override
    public Boolean isActive() {
        return orient.isActive();
    }
}
