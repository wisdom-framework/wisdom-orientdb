package org.wisdom.orientdb.manager;

import com.orientechnologies.common.profiler.OProfiler;
import com.orientechnologies.orient.core.command.script.OScriptManager;
import com.orientechnologies.orient.core.conflict.ORecordConflictStrategyFactory;
import com.orientechnologies.orient.core.db.OPartitionedDatabasePoolFactory;
import com.orientechnologies.orient.core.engine.OEngine;
import com.orientechnologies.orient.core.storage.OStorage;

import java.util.Collection;
import java.util.Date;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * A service that allows you controlling the {@link com.orientechnologies.orient.core.Orient} instance wrapper by the
 * {@link org.wisdom.orientdb.runtime.OrientDbManagerComp} component.
 *
 * It also allows to get the global {@link OPartitionedDatabasePoolFactory}.
 */
public interface OrientDbManager {
    /**
     * @return The partitioned database pool factory.
     */
    OPartitionedDatabasePoolFactory getDatabasePoolFactory();

    ORecordConflictStrategyFactory getRecordConflictStrategy();

    void scheduleTask(TimerTask task, long delay, long period);

    void scheduleTask(TimerTask task, Date firstTime, long period);

    Future<?> submit(Runnable runnable);

    <V> Future<V> submit(Callable<V> callable);

    OStorage loadStorage(String iURL);

    OStorage getStorage(String dbName);

    void registerEngine(OEngine iEngine);

    OEngine getEngine(String engineName);

    Set<String> getEngines();

    void unregisterStorageByName(String name);

    void unregisterStorage(OStorage storage);

    Collection<OStorage> getStorages();

    OProfiler getProfiler();

    void setProfiler(OProfiler iProfiler);

    OScriptManager getScriptManager();

    Boolean isActive();
}
