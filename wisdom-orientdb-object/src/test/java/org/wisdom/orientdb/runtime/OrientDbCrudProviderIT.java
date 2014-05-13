/*
 * Copyright 2014, Technologic Arts Vietnam.
 * All right reserved.
 */

package org.wisdom.orientdb.runtime;

import com.orientechnologies.orient.core.metadata.security.OSecurity;
import com.orientechnologies.orient.core.metadata.security.OUser;
import com.orientechnologies.orient.object.db.OObjectDatabaseTx;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.ow2.chameleon.testing.helpers.IPOJOHelper;
import org.ow2.chameleon.testing.helpers.OSGiAssert;
import org.ow2.chameleon.testing.helpers.OSGiHelper;
import org.wisdom.test.parents.WisdomTest;

import javax.inject.Inject;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * created: 5/13/14.
 *
 * @author <a href="mailto:jbardin@tech-arts.com">Jonathan M. Bardin</a>
 */
public class OrientDbCrudProviderIT extends WisdomTest {

    public static TemporaryFolder folder = new TemporaryFolder();

    private static OObjectDatabaseTx db;


    @Inject
    public BundleContext context;

    public IPOJOHelper ipojoHelper;
    public OSGiHelper osgiHelper;
    public OSGiAssert osgiAssert;

    @BeforeClass
    public static void setup() throws IOException {
        folder.create();
    }

    @AfterClass
    public static void tearDown(){
        if(db != null && !db.isClosed()){
            db.close();
        }
    }

    @Before
    public void beforeEach(){
        ipojoHelper = new IPOJOHelper(context);
        osgiHelper = new OSGiHelper(context);
        osgiAssert = new OSGiAssert(context);
    }

    @Test
    public void bundleShouldBeValid() throws InterruptedException {
        assertThat(osgiHelper.getBundle("org.wisdom.framework.wisdom.orientdb.object").getState()).isEqualTo(Bundle.ACTIVE);
    }


    public OObjectDatabaseTx getDb(){
        if(db == null){
            db = new OObjectDatabaseTx("plocal:" + folder.getRoot().getAbsolutePath()).create();
            OSecurity sm = db.getMetadata().getSecurity();
            OUser user = sm.createUser("test", "test", new String[]{"admin"});
            db.close();
        }

        return db;
    }
}
