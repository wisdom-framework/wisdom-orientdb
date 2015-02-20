/*
 * Copyright 2014, Technologic Arts Vietnam.
 * All right reserved.
 */

package org.wisdom.orientdb.runtime;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.architecture.Architecture;
import org.junit.Before;
import org.junit.Test;
import org.ow2.chameleon.testing.helpers.OSGiHelper;
import org.wisdom.orientdb.manager.OrientDbManager;
import org.wisdom.test.parents.WisdomTest;

import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * In container test for the {@link OrientDbManagerComp} component instance.
 *
 */
public class OrientDbManagerCompIT extends WisdomTest {
    private OSGiHelper helper;

    @Inject
    OrientDbManager manager;

    @Before
    public void before(){
        helper = new OSGiHelper(context);
    }

    @Test
    public void instanceShouldBeValidAndStarted(){
        Architecture architecture = helper.getServiceObject(Architecture.class, "(architecture.instance=" + OrientDbManagerComp.INSTANCE_NAME + ")");
        assertThat(architecture).isNotNull();
        assertThat(architecture.getInstanceDescription().getState()).isEqualTo(ComponentInstance.VALID);
    }

    @Test
    public void orientInstanceShouldBeActive(){
        assertThat(manager.isActive()).isTrue();
    }
}
