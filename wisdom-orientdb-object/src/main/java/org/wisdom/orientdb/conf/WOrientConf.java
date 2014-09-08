package org.wisdom.orientdb.conf;

import org.wisdom.api.configuration.Configuration;

import java.util.*;

public final class WOrientConf {
    public static final String ORIENTDB_PREFIX = "orientdb";
    public static final String ORIENTDB_URL = "url";
    public static final String ORIENTDB_USER = "user";
    public static final String ORIENTDB_PASS = "pass";
    public static final String ORIENTDB_PACKAGE = "package";
    public static final String ORIENTDB_AUTOLAZYLOADGING = "autolazyloading";

    private final String alias;
    private final String url;
    private final String user;
    private final String pass;
    private final String nameSpace;
    private Boolean autolazyloading;


    public WOrientConf(String alias, String url, String user, String pass, String nameSpace, Boolean autolazyloading) {
        this.alias = alias;
        this.url = url;
        this.user = user;
        this.pass = pass;
        this.nameSpace = nameSpace;
        this.autolazyloading = autolazyloading;
    }

    private WOrientConf(String alias, Configuration config) {
        this(
            alias,
            config.getOrDie(ORIENTDB_URL),
            config.getOrDie(ORIENTDB_USER),
            config.getOrDie(ORIENTDB_PASS),
            config.getOrDie(ORIENTDB_PACKAGE),

            config.getBooleanWithDefault(ORIENTDB_AUTOLAZYLOADGING,true)
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

    public Boolean getAutolazyloading() {
        return autolazyloading;
    }

    public void setAutoLazyLoading(Boolean lazyloading){
        if(lazyloading == null){
            throw new NullPointerException(ORIENTDB_AUTOLAZYLOADGING+" cannot be null.");
        }

        this.autolazyloading = lazyloading;
    }

    public Dictionary<String,Object> toDico(){
        Dictionary<String,Object> dico = new Hashtable<>(5);
        dico.put("name",alias);
        dico.put(ORIENTDB_URL,url);
        dico.put(ORIENTDB_USER,user);
        dico.put(ORIENTDB_PACKAGE,nameSpace);
        dico.put(ORIENTDB_AUTOLAZYLOADGING,autolazyloading);
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
    public static Collection<WOrientConf> createFromApplicationConf(Configuration config) {
        Configuration orient = config.getConfiguration(ORIENTDB_PREFIX);

        if(orient == null){
            return Collections.EMPTY_SET;
        }

        Set<String> subkeys = new HashSet<String>();

        for (String key : orient.asProperties().stringPropertyNames()) {
            subkeys.add(key.split("\\.", 2)[0]);
        }

        Collection<WOrientConf> subconfs = new ArrayList<WOrientConf>(subkeys.size());

        for (String subkey : subkeys) {
            subconfs.add(new WOrientConf(subkey, orient.getConfiguration(subkey)));
        }

        return subconfs;
    }

    @Override
    public String toString() {
        return "WOrientConf{" +
                "alias='" + alias + '\'' +
                ", url='" + url + '\'' +
                ", user='" + user + '\'' +
                ", nameSpace='" + nameSpace + '\'' +
                ", autolazyloading=" + autolazyloading +
                '}';
    }
}