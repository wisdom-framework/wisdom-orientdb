/*
 * Copyright 2014, Technologic Arts Vietnam.
 * All right reserved.
 */

package org.wisdom.orientdb.runtime;

import com.google.common.collect.Lists;
import com.orientechnologies.orient.core.metadata.security.OSecurity;
import com.orientechnologies.orient.object.db.OObjectDatabaseTx;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.wisdom.api.model.EntityFilter;
import org.wisdom.api.model.HasBeenRollBackException;
import org.wisdom.orientdb.conf.WOrientConf;
import org.wisdom.orientdb.model.Hello;
import org.wisdom.orientdb.object.OrientDbCrud;
import org.wisdom.orientdb.object.OrientDbRepoCommand;
import org.wisdom.orientdb.othermodel.Olleh;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import static com.orientechnologies.orient.core.tx.OTransaction.TXTYPE.NOTX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * created: 5/9/14.
 *
 * @author <a href="mailto:jbardin@tech-arts.com">Jonathan M. Bardin</a>
 */
public class OrientDbCrudServiceTest {
    private static OObjectDatabaseTx db;
    private static OrientDbCrud<Hello, String> crud;
    private static OrientDbCrud<Olleh, String> crudOther;

    private final static TemporaryFolder folder = new TemporaryFolder();

    @BeforeClass
    public static void setUp() throws IOException {
        folder.create();
        String url = "plocal:" + folder.getRoot().getAbsolutePath();

        db = new OObjectDatabaseTx(url).create();
        OSecurity sm = db.getMetadata().getSecurity();
        sm.createUser("test", "test", "admin");
        List<String> namespaces = Arrays.asList("org.wisdom.orientdb.model","org.wisdom.orientdb.othermodel");
        final WOrientConf conf = new WOrientConf("test",url,"test","test",namespaces );
        conf.setTxType(NOTX);
        final List<Class<?>> entities = new ArrayList<>(2);
        entities.add(Hello.class);
        entities.add(Olleh.class);

        db.getEntityManager().registerEntityClass(Hello.class);
        db.getEntityManager().registerEntityClass(Olleh.class);


        OrientDbRepoCommand repoCommand = new OrientDbRepoCommand() {
            public WOrientConf getConf() {
                return conf;
            }
            public List<Class<?>> getEntityClass() {
                return entities;
            }
            public void init(OObjectDatabaseTx db) {
            }
            public void destroy(OObjectDatabaseTx db) {
            }
        };



        crud = new OrientDbCrudService<>(new OrientDbRepositoryImpl(repoCommand),Hello.class);
        crudOther = new OrientDbCrudService<>(new OrientDbRepositoryImpl(repoCommand),Olleh.class);
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

    @Test
    public void entityShouldBeRegister(){
        assertThat(db.getEntityManager().getRegisteredEntities()).contains(Hello.class);
        assertThat(db.getEntityManager().getRegisteredEntities()).contains(Olleh.class);
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
    public void saveShouldPersistTheInstanceForSecondNS() {
        Olleh olleh = new Olleh();
        olleh.setName("Doe");
        Olleh saved = crudOther.save(olleh);
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
        OObjectDatabaseTx current = crud.getRepository().get().acquire();

        //trigger OrientDbCrudService#acquire
        Hello hello = new Hello();
        hello.setName("Lazy");
        crud.save(hello);

        assertThat(current.isLazyLoading()).isFalse();

        //reset lazy loading to true
        ((OrientDbRepositoryImpl) crud.getRepository()).getConf().setAutoLazyLoading(true);
    }

    @Test
    public void crudShouldUseLazyLoadingIfSetToTrue(){
        ((OrientDbRepositoryImpl) crud.getRepository()).getConf().setAutoLazyLoading(true);
        OObjectDatabaseTx current = crud.getRepository().get().acquire();

        //trigger OrientDbCrudService#acquire
        Hello hello = new Hello();
        hello.setName("Bob");
        crud.save(hello);

        assertThat(current.isLazyLoading()).isEqualTo(true);
    }

    @Test
    public void transactionBlockOfTypeNOTXShouldRunProperly(){
        ((OrientDbRepositoryImpl) crud.getRepository()).getConf().setTxType(NOTX);

        Boolean result = null;
        try {
            result = crud.executeTransactionalBlock(new Callable<Boolean>() {
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
        } catch (HasBeenRollBackException e) {
            fail("Exception should not be throw",e);
        }

        assertThat(result).isTrue();
        assertThat(crud.findAll(new EntityFilter<Hello>() {
            @Override
            public boolean accept(Hello hello) {
                return hello.getName().equals("Haha!");
            }
        })).hasSize(1);
    }
}
