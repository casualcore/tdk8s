/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.integration


import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.KubernetesClientBuilder
import se.laz.casual.test.tdk8s.TestKube
import se.laz.casual.test.tdk8s.connection.KubeConnection
import se.laz.casual.test.tdk8s.sample.WildflyResources
import spock.lang.Shared
import spock.lang.Specification

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

import static se.laz.casual.test.tdk8s.TestKube.RESOURCE_LABEL_NAME

class WildflyConnectivityIntTest extends Specification
{
    @Shared
    KubernetesClient client = new KubernetesClientBuilder().build()
    @Shared
    String id = WildflyConnectivityIntTest.class.getSimpleName(  )
    @Shared
    TestKube instance
    @Shared
    String podName = WildflyResources.SIMPLE_WILDFLY_POD_NAME
    @Shared
    String serviceName = WildflyResources.SIMPLE_WILDFLY_SERVICE_NAME

    def setupSpec()
    {

        assert client.pods(  ).withLabel( RESOURCE_LABEL_NAME, id ).list().getItems(  ).size(  ) == 0
        assert client.services(  ).withLabel( RESOURCE_LABEL_NAME, id ).list().getItems(  ).size(  ) == 0

        instance = TestKube.newBuilder()
                .label( id )
                .addPod( WildflyResources.SIMPLE_WILDFLY_POD_NAME, WildflyResources.SIMPLE_WILDFLY_POD )
                .addService( WildflyResources.SIMPLE_WILDFLY_SERVICE_NAME, WildflyResources.SIMPLE_WILDFLY_SERVICE )
                .addService( WildflyResources.EXTERNAL_WILDFLY_SERVICE_NAME, WildflyResources.EXTERNAL_WILDFLY_SERVICE )
                .build()

        instance.init(  )

    }

    def cleanupSpec()
    {
        instance.destroy(  )

        assert client.pods(  ).withLabel( RESOURCE_LABEL_NAME, id ).list().getItems(  ).size(  ) == 0
        assert client.services(  ).withLabel( RESOURCE_LABEL_NAME, id ).list().getItems(  ).size(  ) == 0
    }

    def "Connection to pod, port 9990 using port forward."()
    {
        given:
        int status
        String body

        when:
        try( KubeConnection connection = instance.getController().getPortForwardConnection( podName, 9990 ) )
        {
            HttpResponse<String> response = httpGet( connection )
            status = response.statusCode(  )
            body = response.body()
        }

        then:
        status == 302
        body == ""
    }

    def "Connect to pod, port 8080, using port forward."()
    {
        given:
        int status
        String body

        when:
        try( KubeConnection connection = instance.getController().getPortForwardConnection( podName, 8080 ) )
        {
            HttpResponse<String> response = httpGet( connection )
            status = response.statusCode(  )
            body = response.body()
        }

        then:
        status == 200
        body != ""
        body.containsIgnoreCase( "wildfly" )
    }

    def "Connect to pod, port 8080, using port forward, not using try with resources."()
    {
        when:
        KubeConnection connection = instance.getController().getPortForwardConnection( podName, 8080 )

        HttpResponse<String> response = httpGet( connection )
        int status = response.statusCode(  )
        String body = response.body()

        then:
        status == 200
        body != ""
        body.containsIgnoreCase( "wildfly" )
    }

    def "Connect to service, port 8080, using port forward."()
    {
        given:
        int status
        String body

        when:
        try( KubeConnection connection = instance.getController().getPortForwardConnection( serviceName, 8080 ) )
        {
            HttpResponse<String> response = httpGet( connection )
            status = response.statusCode(  )
            body = response.body()
        }

        then:
        status == 200
        body != ""
        body.containsIgnoreCase( "wildfly" )
    }

    def "Connect to service, port 8080."()
    {
        given:
        int status
        String body

        when:
        try( KubeConnection connection = instance.getConnection( serviceName, 8080 ) )
        {
            HttpResponse<String> response = httpGet( connection )
            status = response.statusCode(  )
            body = response.body()
        }

        then:
        status == 200
        body != ""
        body.containsIgnoreCase( "wildfly" )
    }

    def "Connect to external service, with internal port 8080."()
    {
        given:
        int status
        String body

        when:
        try( KubeConnection connection = instance.getConnection( WildflyResources.EXTERNAL_WILDFLY_SERVICE_NAME, 8080 ) )
        {
            HttpResponse<String> response = httpGet( connection )
            status = response.statusCode(  )
            body = response.body()
        }

        then:
        status == 200
        body != ""
        body.containsIgnoreCase( "wildfly" )
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
