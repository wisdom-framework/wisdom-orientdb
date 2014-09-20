Wisdom-OrientDB
===============

[![Build Status](https://travis-ci.org/wisdom-framework/wisdom-ractivejs.png)](https://travis-ci.org/wisdom-framework/wisdom-orientdb)

[OrientDB](https://github.com/orientechnologies/orientdb) is an open source NoSQL DBMS. It is fully written in Java. While it's a Document Database, it also has Graph and Object database features and API.

Wisdom-OrientDB brings OrientDB to your [wisdom framework](http://wisdom-framework.org) projects.

The wisdom-orientdb-object project provides an implementation of the Crud service; wrapping the object database features of OrientDB.

## Set Up

In order to use wisdom-orientdb-object within your wisdom project, you must follow two steps.

The first is to add it's dependency in your `pom.xml`.

```xml
<plugin>
  <groupId>org.wisdom-framework</groupId>
  <artifactId>wisdom-orientdb-object</artifactId>
  <version>${release.version}</version>
</plugin>
```

The second step is to configure your OrientDB database in wisdom `application.conf`

```
orientdb.test.url = memory:todolist
orientdb.test.user = test
orientdb.test.pass = test
orientdb.test.package = todolist.model
```

The example above shows how to set up an orientdb database with an alias test. The `orientdb.test.package` is the name of the package that contains your entities.

The optional `orientdb.<alias>.autolazyloading` boolean property is a convenient way to configure the default orientDB lazy loading behavior.

## Usage

Once wisdom-orientDB has been properly set up you can now requires either wisdom [Crud](http://wisdom-framework.org/documentation/apidocs/0.6.4/org/wisdom/api/model/Crud.html) services or the specialized [OrientDbCrud](https://github.com/wisdom-framework/wisdom-orientdb/blob/master/wisdom-orientdb-object/src/main/java/org/wisdom/orientdb/object/OrientDbCrud.java) for each of the entity available in your model package.

The following example illustrates how to inject an `OrientDBCrud` service for the entity `Todo.class`. An entity is a java class annotated with `javax.persistence.Entity`.

```java
@Model(value = Todo.class)
private OrientDbCrud<Todo,String> todoCrud;
```

More information about the behavior of the OrientDB database is available on their [wiki](https://github.com/orientechnologies/orientdb/wiki/Object-Database).

## Troubleshooting

OrientDB uses javassist in order to create proxy for the entity object. This can cause your project to experience some problems when being dynamicaly updated if it does not have _javassist.util.proxy.Proxy_ in its classpath. A work around is to load this class in one of your component.

```java
    import javassist.util.proxy.Proxy;
    //...
    static {Class workaround = Proxy.class;}
    //...
```
