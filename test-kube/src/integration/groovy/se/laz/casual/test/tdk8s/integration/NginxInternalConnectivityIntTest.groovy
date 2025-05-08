/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.integration

import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.KubernetesClientBuilder
import se.laz.casual.test.tdk8s.TestKube
import se.laz.casual.test.tdk8s.exec.ExecResult
import se.laz.casual.test.tdk8s.sample.NginxResources
import spock.lang.Shared
import spock.lang.Specification

import java.util.concurrent.TimeUnit

import static se.laz.casual.test.tdk8s.TestKube.RESOURCE_LABEL_NAME

class NginxInternalConnectivityIntTest extends Specification
{
    @Shared
    KubernetesClient client = new KubernetesClientBuilder().build()
    @Shared
    String id = NginxInternalConnectivityIntTest.class.getSimpleName(  )
    @Shared
    TestKube instance

    def setupSpec()
    {

        assert client.pods(  ).withLabel( RESOURCE_LABEL_NAME, id ).list().getItems(  ).size(  ) == 0
        assert client.services(  ).withLabel( RESOURCE_LABEL_NAME, id ).list().getItems(  ).size(  ) == 0

        instance = TestKube.newBuilder()
                .label( id )
                .addPod( NginxResources.SIMPLE_NGINX_POD_NAME, NginxResources.SIMPLE_NGINX_POD )
                .addPod( NginxResources.SIMPLE_NGINX_POD_NAME2, NginxResources.SIMPLE_NGINX_POD2 )
                .addService( NginxResources.SIMPLE_NGINX_SERVICE_NAME, NginxResources.SIMPLE_NGINX_SERVICE )
                .build()

        instance.init(  )

    }

    def cleanupSpec()
    {
        instance.destroy(  )

        assert client.pods(  ).withLabel( RESOURCE_LABEL_NAME, id ).list().getItems(  ).size(  ) == 0
        assert client.services(  ).withLabel( RESOURCE_LABEL_NAME, id ).list().getItems(  ).size(  ) == 0
    }

    def "Execute curl to other pod via service from other pod."()
    {
        given:
        //sleep( 10000 )
        String[] command = ["sh", "-c", "curl -iv http://" + NginxResources.SIMPLE_NGINX_SERVICE_NAME +":"+80 ]

        when:
        ExecResult actual = instance.getController(  ).executeCommandAsync( NginxResources.SIMPLE_NGINX_POD_NAME2, command )
                .get( 5, TimeUnit.SECONDS)

        then:
        actual.getExitCode(  ) == 0
        actual.getOutput(  ).contains("HTTP/1.1 200 OK"  )
    }
}
