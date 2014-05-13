package org.wisdom.orientdb.conf;

import org.wisdom.api.configuration.Configuration;

import java.util.*;

public class WOrientConf {
    public static final String ORIENTDB_PREFIX = "orientdb";
    public static final String ORIENTDB_URL = "url";
    public static final String ORIENTDB_USER = "user";
    public static final String ORIENTDB_PASS = "pass";
    public static final String ORIENTDB_PACKAGE = "package";

    private final String alias;
    private final String url;
    private final String user;
    private final String pass;
    private final String nameSpace;


    public WOrientConf(String alias, String url, String user, String pass, String nameSpace) {
        this.alias = alias;
        this.url = url;
        this.user = user;
        this.pass = pass;
        this.nameSpace = nameSpace;
    }

    private WOrientConf(String alias, Configuration config) {
        this(
            alias,
            config.getOrDie(ORIENTDB_URL),
            config.getOrDie(ORIENTDB_USER),
            config.getOrDie(ORIENTDB_PASS),
            config.getOrDie(ORIENTDB_PACKAGE)
        );
    }

    public String getAlias() {
        return alias;
    }

    public String getUrl() {
        return url;
    }

    public String getUser() {
        return user;
    }

    public String getPass() {
        return pass;
    }

    public String getNameSpace() {
        return nameSpace;
    }

    public Dictionary<String,String> toDico(){
        Dictionary<String,String> dico = new Hashtable<>(3);
        dico.put("name",alias);
        dico.put("url",url);
        dico.put("user",user);
        dico.put("package",nameSpace);
        return dico;
    }

    /**
     * Extract all WOrientConf configuration from the parent configuration.
     * If the configuration is
     * orientdb.default.url = "plocal:/home/wisdom/db"
     * orientdb.test.url = "plocal:/home/wisdom/test/db"
     * <p/>
     * the sub configuration will be:
     * <p/>
     * [alias:default]
     * url = "plocal:/home/wisdom/db"
     * [alias:test]
     * url = "plocal:/home/wisdom/test/db"
     *
     * @param config
     * @return
     */
    public static Collection<WOrientConf> extractFromParent(Configuration config) {
        Set<String> subkeys = new HashSet<String>();

        for (String key : config.getConfiguration(ORIENTDB_PREFIX).asProperties().stringPropertyNames()) {
            subkeys.add(key.split("\\.", 2)[0]);
        }

        Collection<WOrientConf> subconfs = new ArrayList<WOrientConf>(subkeys.size());

        for (String subkey : subkeys) {
            subconfs.add(new WOrientConf(subkey, config.getConfiguration(subkey)));
        }

        return subconfs;
    }

}