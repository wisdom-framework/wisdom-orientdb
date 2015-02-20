/*
 * Copyright 2014, Technologic Arts Vietnam.
 * All right reserved.
 */

package org.wisdom.orientdb.runtime;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.architecture.Architecture;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.ow2.chameleon.testing.helpers.OSGiHelper;
import org.wisdom.orientdb.conf.WOrientConf;
import org.wisdom.orientdb.document.OrientDbDocumentCommand;
import org.wisdom.orientdb.document.OrientDbDocumentService;
import org.wisdom.test.parents.WisdomTest;

import java.util.Collections;
import java.util.Hashtable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * In container test for the {@link org.wisdom.orientdb.runtime.OrientDbDocumentProvider} component instance.
 *
 */
public class OrientDbDocumentProviderIT extends WisdomTest {
    private OSGiHelper helper;

    @Mock
    private OrientDbDocumentCommand dummyCommand;

    @Before
    public void before(){
        helper = new OSGiHelper(context);

        MockitoAnnotations.initMocks(this);
        when(dummyCommand.getConf()).thenReturn(new WOrientConf("testdb","memory:testdb","user","pass", Collections.EMPTY_LIST));
    }

    @Test
    public void instanceShouldBeValidAndStarted(){
        Architecture architecture = helper.getServiceObject(Architecture.class, "(architecture.instance=" + OrientDbDocumentProvider.INSTANCE_NAME + ")");
        assertThat(architecture).isNotNull();
        assertThat(architecture.getInstanceDescription().getState()).isEqualTo(ComponentInstance.VALID);
    }

    @Test
    public void docServiceShouldBeProvidedWhenCommandIsPublished(){
        //given

        //when
        ServiceRegistration<OrientDbDocumentCommand> sreg = context.registerService(OrientDbDocumentCommand.class,dummyCommand,new Hashtable<String, Object>());

        //then
        ServiceReference sref = helper.getServiceReference(OrientDbDocumentService.class);
        assertThat(sref).isNotNull();
    }
}
