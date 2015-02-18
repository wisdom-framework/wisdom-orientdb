package org.wisdom.orientdb.document;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

/**
 *
 */
public interface OrientDbDocumentService {
    String getUrl();
    String getAlias();
    ODatabaseDocumentTx acquire();
}
