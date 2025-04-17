/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.k8s.integration


import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.KubernetesClientBuilder
import se.laz.casual.test.k8s.TestKube
import se.laz.casual.test.k8s.connection.KubeConnection
import se.laz.casual.test.k8s.sample.NginxResources
import spock.lang.Shared
import spock.lang.Specification

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

import static se.laz.casual.test.k8s.TestKube.RESOURCE_LABEL_NAME

class MultiTestKubesIntTest extends Specification
{
    @Shared
    KubernetesClient client = new KubernetesClientBuilder().build()
    @Shared
    String id = MultiTestKubesIntTest.class.getSimpleName(  )
    @Shared
    TestKube instance
    @Shared
    String podName = NginxResources.SIMPLE_NGINX_POD_NAME
    @Shared
    String serviceName = NginxResources.SIMPLE_NGINX_SERVICE_NAME

    String id2 = MultiTestKubesIntTest.class.getSimpleName(  ) + "2"
    TestKube instance2
    String podName2 = NginxResources.SIMPLE_NGINX_POD_NAME2
    String serviceName2 = NginxResources.SIMPLE_NGINX_SERVICE_NAME2

    def setupSpec()
    {

        assert client.pods(  ).withLabel( RESOURCE_LABEL_NAME, id ).list().getItems(  ).size(  ) == 0
        assert client.services(  ).withLabel( RESOURCE_LABEL_NAME, id ).list().getItems(  ).size(  ) == 0

        instance = TestKube.newBuilder()
                .label( id )
                .addPod( podName, NginxResources.SIMPLE_NGINX_POD )
                .addService( serviceName, NginxResources.SIMPLE_NGINX_SERVICE )
                .build()

        instance.init(  )

    }

    def cleanupSpec()
    {
        instance.destroy(  )

        assert client.pods(  ).withLabel( RESOURCE_LABEL_NAME, id ).list().getItems(  ).size(  ) == 0
        assert client.services(  ).withLabel( RESOURCE_LABEL_NAME, id ).list().getItems(  ).size(  ) == 0
    }

    def setup()
    {
        assert client.pods(  ).withLabel( RESOURCE_LABEL_NAME, id2 ).list().getItems(  ).size(  ) == 0
        assert client.services(  ).withLabel( RESOURCE_LABEL_NAME, id2 ).list().getItems(  ).size(  ) == 0

        instance2 = TestKube.newBuilder()
                .label( id2 )
                .addPod( podName2, NginxResources.SIMPLE_NGINX_POD2 )
                .addService( serviceName2, NginxResources.SIMPLE_NGINX_SERVICE2 )
                .build()
        instance2.init(  )
    }

    def cleanup()
    {
        instance2.destroy(  )
        assert client.pods(  ).withLabel( RESOURCE_LABEL_NAME, id2 ).list().getItems(  ).size(  ) == 0
        assert client.services(  ).withLabel( RESOURCE_LABEL_NAME, id2 ).list().getItems(  ).size(  ) == 0
    }

    def "Connect to both."()
    {
        expect:
        assertConnection( instance, serviceName, 80 )
        assertConnection( instance2, serviceName2, 80 )
    }

    def assertConnection( TestKube tk, String serviceName, int port )
    {
        int status
        String body

        try( KubeConnection connection = tk.getController().getConnection( serviceName, port ) )
        {
            HttpResponse<String> response = httpGet( connection )
            status = response.statusCode(  )
            body = response.body()
        }

        assert status == 200
        assert body != ""
        assert body.containsIgnoreCase( "nginx" )
        return true
    }

    HttpResponse<String> httpGet( KubeConnection connection )
    {
        String host = connection.getHostName()
        int port = connection.getPort()

        HttpClient httpClient = HttpClient.newBuilder(  ).build(  )
        HttpRequest request = HttpRequest.newBuilder( )
                .uri( URI.create( "http://" + host + ":" + port +"/" ) )
                .GET( )
                .build(  )

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
