/*
 * Copyright 2014, Technologic Arts Vietnam.
 * All right reserved.
 */

package org.wisdom.orientdb.runtime;

import com.orientechnologies.orient.core.metadata.security.OSecurity;
import com.orientechnologies.orient.core.metadata.security.OUser;
import com.orientechnologies.orient.core.tx.OTransaction;
import com.orientechnologies.orient.object.db.OObjectDatabaseTx;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.wisdom.api.model.EntityFilter;
import org.wisdom.orientdb.conf.WOrientConf;
import org.wisdom.orientdb.model.Hello;
import org.wisdom.orientdb.object.OrientDbCrud;

import java.io.IOException;
import java.util.concurrent.Callable;

import static com.orientechnologies.orient.core.tx.OTransaction.TXTYPE.NOTX;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * created: 5/9/14.
 *
 * @author <a href="mailto:jbardin@tech-arts.com">Jonathan M. Bardin</a>
 */
public class OrientDbCrudServiceTest {
    private static OObjectDatabaseTx db;
    private static OrientDbCrud<Hello, String> crud;

    public static TemporaryFolder folder = new TemporaryFolder();

    @BeforeClass
    public static void setUp() throws IOException {
        folder.create();
        String url = "plocal:" + folder.getRoot().getAbsolutePath();

        db = new OObjectDatabaseTx(url).create();
        OSecurity sm = db.getMetadata().getSecurity();
        OUser user = sm.createUser("test", "test", new String[]{"admin"});
        WOrientConf conf = new WOrientConf("test",url,"test","test","org.wisdom.orientdb.model",true, NOTX);
        db.getEntityManager().registerEntityClass(Hello.class);

        crud = new OrientDbCrudService<Hello>(new OrientDbRepositoryImpl(conf),Hello.class);
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

    @Test
    public void crudShouldNotUseLazyLoadingIfSetToFalse(){
        ((OrientDbRepositoryImpl) crud.getRepository()).getConf().setAutoLazyLoading(false);
        OObjectDatabaseTx current = ((OrientDbRepositoryImpl) crud.getRepository()).get().acquire();

        //trigger OrientDbCrudService#acquire
        Hello hello = new Hello();
        hello.setName("Lazy");
        Hello saved = crud.save(hello);

        assertThat(current.isLazyLoading()).isFalse();

        //reset lazy loading to true
        ((OrientDbRepositoryImpl) crud.getRepository()).getConf().setAutoLazyLoading(true);
    }

    @Test
    public void crudShouldUseLazyLoadingIfSetToTrue(){
        ((OrientDbRepositoryImpl) crud.getRepository()).getConf().setAutoLazyLoading(true);
        OObjectDatabaseTx current = ((OrientDbRepositoryImpl) crud.getRepository()).get().acquire();

        //trigger OrientDbCrudService#acquire
        Hello hello = new Hello();
        hello.setName("Bob");
        Hello saved = crud.save(hello);

        assertThat(current.isLazyLoading()).isEqualTo(true);
    }

    @Test
    public void transactionBlockOfTypeNOTXShouldRunProperly(){
        ((OrientDbRepositoryImpl) crud.getRepository()).getConf().setTxType(OTransaction.TXTYPE.NOTX);
        OObjectDatabaseTx current = ((OrientDbRepositoryImpl) crud.getRepository()).get().acquire();

        Boolean result = crud.executeTransactionalBlock(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                Hello hello = new Hello();
                hello.setName("Bob");
                Hello saved = crud.save(hello);
                saved.setName("Haha!");
                crud.save(saved);
                return true;
            }
        });

        assertThat(result).isTrue();
        assertThat(crud.findAll(new EntityFilter<Hello>() {
            @Override
            public boolean accept(Hello hello) {
                return hello.getName().equals("Haha!");
            }
        })).hasSize(1);
    }
}
