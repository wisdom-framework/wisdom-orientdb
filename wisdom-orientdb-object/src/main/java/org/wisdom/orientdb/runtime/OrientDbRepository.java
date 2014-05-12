package org.wisdom.orientdb.runtime;

import com.orientechnologies.orient.object.db.OObjectDatabasePool;
import org.wisdom.api.model.Crud;
import org.wisdom.api.model.Repository;

import java.util.Collection;

/**
 * Created by barjo on 5/10/14.
 */
public class OrientDbRepository implements Repository<OObjectDatabasePool>{
    private final OObjectDatabasePool server;

    public OrientDbRepository(OObjectDatabasePool server) {
        this.server = server;
    }

    @Override
    public Collection<Crud<?, ?>> getCrudServices() {
        return null;
    }

    @Override
    public String getName() {
        return server.getName();
    }

    @Override
    public String getType() {
        return "orientdb-object-pool";
    }

    @Override
    public Class<OObjectDatabasePool> getRepositoryClass() {
        return OObjectDatabasePool.class;
    }

    @Override
    public OObjectDatabasePool get() {
        return server;
    }
}
