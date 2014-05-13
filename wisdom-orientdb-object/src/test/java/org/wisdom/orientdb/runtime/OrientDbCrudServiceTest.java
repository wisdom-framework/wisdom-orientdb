/*
 * Copyright 2014, Technologic Arts Vietnam.
 * All right reserved.
 */

package org.wisdom.orientdb.runtime;

import com.orientechnologies.orient.core.metadata.security.OSecurity;
import com.orientechnologies.orient.core.metadata.security.OUser;
import com.orientechnologies.orient.object.db.OObjectDatabaseTx;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.wisdom.api.model.Crud;
import org.wisdom.api.model.EntityFilter;
import org.wisdom.orientdb.conf.WOrientConf;
import org.wisdom.orientdb.model.Hello;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * created: 5/9/14.
 *
 * @author <a href="mailto:jbardin@tech-arts.com">Jonathan M. Bardin</a>
 */
public class OrientDbCrudServiceTest {
    private static OObjectDatabaseTx db;
    private static Crud<Hello, String> crud;

    public static TemporaryFolder folder = new TemporaryFolder();

    @BeforeClass
    public static void setUp() throws IOException {
        folder.create();
        String url = "plocal:" + folder.getRoot().getAbsolutePath();

        db = new OObjectDatabaseTx(url).create();
        OSecurity sm = db.getMetadata().getSecurity();
        OUser user = sm.createUser("test", "test", new String[]{"admin"});
        WOrientConf conf = new WOrientConf("test",url,"test","test","org.wisdom.orientdb.model");

        BundleContext context = mock(BundleContext.class);
        when(context.registerService(eq(Crud.class), any(Crud.class), eq(conf.toDico()))).thenReturn(mock(ServiceRegistration.class));

        crud = new OrientDbCrudService<Hello>(new OrientDbRepository(conf,OrientDbCrudServiceTest.class.getClassLoader(), mock(BundleContext.class)),Hello.class);
    }

    @AfterClass
    public static void tearDown() {
        try {
            db.drop();
        } finally {
            if (!db.isClosed()) {
                db.close();
            }
            folder.delete();
        }
    }

    @After
    public void afterEach() {
        //db.drop();
    }

    @Before
    public void beforeEach() {
    }

    @Test
    public void entityShouldBeRegister(){
        assertThat(db.getEntityManager().getRegisteredEntities()).contains(Hello.class);
    }

    @Test
    public void saveShouldPersistTheInstance() {
        Hello hello = new Hello();
        hello.setName("John");
        Hello saved = crud.save(hello);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getId()).startsWith("#");
    }

    @Test
    public void findShouldReturnInstanceIfPresent() {
        Hello hello = new Hello();
        hello.setName("Bob");
        Hello saved = crud.save(hello);

        Hello bob = crud.findOne(saved.getId());
        assertThat(bob).isNotNull();
        assertThat(bob).isEqualTo(saved);
    }

    @Test
    public void findOneShouldReturnNullIfNotPresent() {
        Hello hello = new Hello();
        hello.setName("Bob");
        Hello saved = crud.save(hello);
        db.delete(saved);

        Hello bob = crud.findOne(saved.getId());
        assertThat(bob).isNull();
    }

    @Test
    public void findOneWithEntityFilterShouldReturnMatchingEntities() {
        Hello hello;

        for (int i = 0; i < 5; i++) {
            hello = new Hello();
            hello.setName("Bob" + i);
            crud.save(hello);
        }

        Hello h = crud.findOne(new EntityFilter<Hello>() {
            @Override
            public boolean accept(Hello hello) {
                return hello.getName().matches("Bob[0-9]");
            }
        });

        assertThat(h).isNotNull();
        assertThat(h.getName()).matches("Bob[0-9]");
    }
}
