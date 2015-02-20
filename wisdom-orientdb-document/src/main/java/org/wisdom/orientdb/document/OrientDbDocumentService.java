package org.wisdom.orientdb.document;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

/**
 * Simple service that allows to acquire an {@link ODatabaseDocumentTx} from the pool.
 */
public interface OrientDbDocumentService {
    String getUrl();
    String getAlias();
    ODatabaseDocumentTx acquire();
}
