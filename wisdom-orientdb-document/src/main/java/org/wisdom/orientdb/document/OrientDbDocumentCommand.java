package org.wisdom.orientdb.document;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import org.wisdom.orientdb.conf.WOrientConf;

/**
 * The OrientDbDocumentCommand service will be used as incentive to provides a new {@link OrientDbDocumentService} corresponding to its configuration {@link #getConf()}.
 */
public interface OrientDbDocumentCommand {

    /**
     * @return The {@link OrientDbDocumentService} configuration that we want to create.
     */
    WOrientConf getConf();


    /**
     * A hook that will be call before the publication of the {@link OrientDbDocumentService}.
     * @param db The OrientDB document database instance.
     */
    void init(ODatabaseDocumentTx db);

    /**
     * A hook that will be call after the de-registration of the {@link OrientDbDocumentService}.
     * @param db The OrientDB document database instance.
     */
    void destroy(ODatabaseDocumentTx db);
}
