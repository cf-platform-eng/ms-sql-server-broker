/*
 * Copyright (C) 2017-Present Pivotal Software, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the under the Apache License,
 * Version 2.0 (the "License”); you may not use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package io.pivotal.ecosystem.mssqlserver.broker;

import io.pivotal.ecosystem.mssqlserver.broker.connector.SqlServerServiceInfo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceBindingDoesNotExistException;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceDoesNotExistException;
import org.springframework.cloud.servicebroker.model.binding.*;
import org.springframework.cloud.servicebroker.model.instance.*;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import({SharedConfig.class, H2Config.class})
@TestPropertySource("classpath:h2.properties")
public class H2SqlServerBrokerTest {

    @Autowired
    private InstanceService instanceService;

    @Autowired
    private BindingService bindingService;

    @Autowired
    @Qualifier("custom")
    private CreateServiceInstanceRequest createServiceInstanceCustomRequest;

    @Autowired
    @Qualifier("custom")
    private CreateServiceInstanceBindingRequest createServiceInstanceBindingCustomRequest;

    @Autowired
    private GetLastServiceOperationRequest getLastServiceOperationRequest;

    @Autowired
    private GetServiceInstanceRequest getServiceInstanceRequest;

    @Autowired
    private UpdateServiceInstanceRequest updateServiceInstanceRequest;

    @Autowired
    private GetServiceInstanceBindingRequest getServiceInstanceBindingRequest;

    @Autowired
    private DeleteServiceInstanceBindingRequest deleteServiceInstanceBindingRequest;

    @Autowired
    private DeleteServiceInstanceRequest deleteServiceInstanceRequest;


    @Test
    public void testLifecycleCustom() {
        CreateServiceInstanceResponse csir = instanceService.createServiceInstance(createServiceInstanceCustomRequest);
        assertNotNull(csir);
        assertEquals(OperationState.SUCCEEDED.getValue(), csir.getOperation());
        assertFalse(csir.isInstanceExisted());

        GetServiceInstanceResponse gsir = instanceService.getServiceInstance(getServiceInstanceRequest);
        assertNotNull(gsir);
        assertEquals(1, gsir.getParameters().size());

        try {
            instanceService.getLastOperation(getLastServiceOperationRequest);
        } catch (UnsupportedOperationException e) {
            //expected
        }

        try {
            instanceService.updateServiceInstance(updateServiceInstanceRequest);
        } catch (UnsupportedOperationException e) {
            //expected
        }

        CreateServiceInstanceAppBindingResponse csiabr = (CreateServiceInstanceAppBindingResponse) bindingService.createServiceInstanceBinding(createServiceInstanceBindingCustomRequest);
        assertNotNull(csiabr);
        assertFalse(csiabr.isBindingExisted());
        Map<String, Object> m = csiabr.getCredentials();
        assertNotNull(m);
        assertEquals(4, m.size());

        GetServiceInstanceAppBindingResponse gsiabr = (GetServiceInstanceAppBindingResponse) bindingService.getServiceInstanceBinding(getServiceInstanceBindingRequest);
        assertNotNull(gsiabr);
        Map<String, Object> m2 = gsiabr.getCredentials();
        assertNotNull(m2);
        assertEquals(4, m.size());
        assertEquals("aUser", m2.get(SqlServerServiceInfo.USERNAME));
        assertNotNull(m2.get(SqlServerServiceInfo.URI));
        assertTrue("uri should start with 'jdbc:h2'", m2.get(SqlServerServiceInfo.URI).toString().startsWith("jdbc:h2:"));
        assertEquals("deleteme", m2.get(SqlServerServiceInfo.DATABASE));

        bindingService.deleteServiceInstanceBinding(deleteServiceInstanceBindingRequest);
        try {
            bindingService.getServiceInstanceBinding(getServiceInstanceBindingRequest);
        } catch (ServiceInstanceBindingDoesNotExistException e) {
            //expected
        }

        DeleteServiceInstanceResponse dsir = instanceService.deleteServiceInstance(deleteServiceInstanceRequest);
        assertNotNull(dsir);
        assertEquals(OperationState.SUCCEEDED.getValue(), dsir.getOperation());

        try {
            instanceService.getServiceInstance(getServiceInstanceRequest);
        } catch (ServiceInstanceDoesNotExistException e) {
            //expected
        }
    }
}