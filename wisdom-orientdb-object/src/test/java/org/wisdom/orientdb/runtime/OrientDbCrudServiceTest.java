/*
 * Copyright 2014, Technologic Arts Vietnam.
 * All right reserved.
 */

package org.wisdom.orientdb.runtime;

import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.command.OCommandPredicate;
import com.orientechnologies.orient.core.command.traverse.OTraverse;
import com.orientechnologies.orient.core.record.ORecord;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.object.db.OObjectDatabaseTx;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.wisdom.api.model.Crud;
import org.wisdom.api.model.EntityFilter;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * created: 5/9/14.
 *
 * @author <a href="mailto:jbardin@tech-arts.com">Jonathan M. Bardin</a>
 */
public class OrientDbCrudServiceTest {
    private static OObjectDatabaseTx db;
    private static Crud<Hello,String> crud;

    @Entity
    public class Hello {
        public String getId() {
            return id;
        }

        @Id
        private String id;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        private String name;


        //We override equals and hascode to test value injected in the proxy

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Hello hello = (Hello) o;

            if (getId() != null ? !getId().equals(hello.getId()) : hello.getId() != null) return false;
            if (getName() != null ? !getName().equals(hello.getName()) : hello.getName() != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = getId() != null ? getId().hashCode() : 0;
            result = 31 * result + (getName() != null ? getName().hashCode() : 0);
            return result;
        }
    }

    public static TemporaryFolder folder = new TemporaryFolder();

    @BeforeClass
    public static void setUp() throws IOException{
        folder.create();
        db = new OObjectDatabaseTx("plocal:"+folder.getRoot().getAbsolutePath()).create();
        crud = new OrientDbCrudService<Hello>(db,Hello.class);
    }

    @AfterClass
    public static void tearDown(){
        db.close();
        folder.delete();
    }

    @After
    public void afterEach(){
        //db.drop();
    }


    public void beforeEach(){
    }

    @Test
    public void shouldProperlyAddTheEntityToTheManager(){
        db.getEntityManager().deregisterEntityClass(Hello.class);
        assertThat(db.getEntityManager().getRegisteredEntities()).doesNotContain(Hello.class);
        new OrientDbCrudService<Hello>(db,Hello.class);
        assertThat(db.getEntityManager().getRegisteredEntities()).contains(Hello.class);
    }

    @Test
    public void saveShouldPersistTheInstance(){
        Hello hello = new Hello();
        hello.setName("John");
        Hello saved = crud.save(hello);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getId()).startsWith("#");

        db.delete(hello);
    }

    @Test
    public void findShouldReturnInstanceIfPresent(){
        Hello hello = new Hello();
        hello.setName("Bob");
        Hello saved = crud.save(hello);

        Hello bob = crud.findOne(saved.getId());
        assertThat(bob).isNotNull();
        assertThat(bob).isEqualTo(saved);
    }

    @Test
    public void findOneShouldReturnNullIfNotPresent(){
        Hello hello = new Hello();
        hello.setName("Bob");
        Hello saved = crud.save(hello);
        db.delete(saved);

        Hello bob = crud.findOne(saved.getId());
        assertThat(bob).isNull();
    }

    @Test
    public void findOneWithEntityFilterShouldReturnMatchingEntities(){
        Hello hello;

        for(int i = 0;i<5;i++){
            hello = new Hello();
            hello.setName("Bob"+i);
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
