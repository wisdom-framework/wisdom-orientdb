/*
 * Copyright 2014, Technologic Arts Vietnam.
 * All right reserved.
 */

package org.wisdom.orientdb.runtime;

import com.orientechnologies.orient.object.db.OObjectDatabaseTx;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.wisdom.api.model.Crud;

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
    }

    public static TemporaryFolder folder = new TemporaryFolder();

    @BeforeClass
    public static void setUp() throws IOException{
        folder.create();
        db = new OObjectDatabaseTx("plocal:"+folder.getRoot().getAbsolutePath()).create();
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
    public void ShouldProperlyAddTheEntityToTheManager(){
        assertThat(db.getEntityManager().getRegisteredEntities()).doesNotContain(Hello.class);
        Crud<Hello,String> crud = new OrientDbCrudService<Hello>(db,Hello.class);
        assertThat(db.getEntityManager().getRegisteredEntities()).contains(Hello.class);
    }

    @Test
    public void saveShouldPersistTheInstance(){
        Crud<Hello,String> crud = new OrientDbCrudService<Hello>(db,Hello.class);
        Hello hello = new Hello();
        hello.setName("John");
        Hello saved = crud.save(hello);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getId()).startsWith("#");
    }

}
