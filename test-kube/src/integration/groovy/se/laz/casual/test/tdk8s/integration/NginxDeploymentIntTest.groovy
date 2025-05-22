/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.integration

import io.fabric8.kubernetes.api.model.PodList
import io.fabric8.kubernetes.api.model.apps.Deployment
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.KubernetesClientBuilder
import se.laz.casual.test.tdk8s.TestKube
import se.laz.casual.test.tdk8s.connection.KubeConnection
import se.laz.casual.test.tdk8s.sample.NginxResources
import spock.lang.Shared
import spock.lang.Specification

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

import static se.laz.casual.test.tdk8s.TestKube.RESOURCE_LABEL_NAME

class NginxDeploymentIntTest extends Specification
{
    @Shared
    KubernetesClient client = new KubernetesClientBuilder().build()
    @Shared
    String id = NginxDeploymentIntTest.class.getSimpleName(  )
    @Shared
    TestKube instance
    @Shared
    String deploymentName = NginxResources.SIMPLE_NGINX_DEPLOYMENT_NAME
    @Shared
    String serviceName = NginxResources.SIMPLE_NGINX_SERVICE_NAME3

    def setupSpec()
    {
        assert client.pods(  ).withLabel( RESOURCE_LABEL_NAME, id ).list().getItems(  ).size(  ) == 0
        assert client.apps(  ).deployments(  ).withLabel( RESOURCE_LABEL_NAME, id ).list().getItems(  ).size(  ) == 0
        assert client.services(  ).withLabel( RESOURCE_LABEL_NAME, id ).list().getItems(  ).size(  ) == 0

        instance = TestKube.newBuilder()
                .label( id )
                .addDeployment( NginxResources.SIMPLE_NGINX_DEPLOYMENT_NAME, NginxResources.SIMPLE_NGINX_DEPLOYMENT )
                .addService( NginxResources.SIMPLE_NGINX_SERVICE_NAME3, NginxResources.SIMPLE_NGINX_SERVICE3 )
                .build()

        instance.init(  )

    }

    def cleanupSpec()
    {
        instance.destroy(  )

        assert client.pods(  ).withLabel( RESOURCE_LABEL_NAME, id ).list().getItems(  ).size(  ) == 0
        assert client.apps(  ).deployments(  ).withLabel( RESOURCE_LABEL_NAME, id ).list().getItems(  ).size(  ) == 0
        assert client.services(  ).withLabel( RESOURCE_LABEL_NAME, id ).list().getItems(  ).size(  ) == 0
    }

//    def "Connect to deployment, port 80, using port forward."()
//    {
//        given:
//        int status
//        String body
//
//        when:
//        try( KubeConnection connection = instance.getController().getPortForwardConnection( deploymentName, 80 ) )
//        {
//            HttpResponse<String> response = httpGet( connection )
//            status = response.statusCode(  )
//            body = response.body()
//        }
//
//        then:
//        status == 200
//        body != ""
//        body.containsIgnoreCase( "nginx" )
//    }

//    def "Test retrieving all pods."()
//    {
//        given:
//        Deployment d = instance.getDeployments(  ).get( deploymentName )
//
//        when:
//        PodList pods = instance.getClient(  ).pods(  ).withLabelSelector( d.getSpec(  ).getSelector(  ) ).list()
//
//        then:
//        pods.getItems().size(  ) == 2
//        noExceptionThrown(  )
//    }

    def scaleAsyncTest( String name, int replicas )
    {
        instance.getController(  ).scaleAsync( name, replicas ).join()
        scaleCheck( name, replicas )
    }

    def scaleTest( String name, int replicas )
    {
        instance.getController(  ).scale( name, replicas )
        scaleCheck( name, replicas )
    }

    def scaleCheck( String name, int replicas )
    {
        Deployment deployment = instance.getResourcesStore(  ).getDeployment( name )
        PodList pods = instance.getClient(  ).pods(  ).withLabelSelector( deployment.getSpec(  ).getSelector(  ) ).list()

        assert deployment.getSpec(  ).getReplicas(  ) == replicas
        assert pods.getItems(  ).size(  ) == replicas
    }

    def "Test scaling deployment synchronously."()
    {
        when:
        scaleTest( deploymentName, 2 )
        scaleTest( deploymentName, 2 )
        scaleTest( deploymentName, 0 )
        scaleTest( deploymentName, 1 )

        then:
        noExceptionThrown(  )
    }

    def "Test scaling deployment asynchronously."()
    {
        when:
        scaleAsyncTest( deploymentName, 2 )
        scaleAsyncTest( deploymentName, 2 )
        scaleAsyncTest( deploymentName, 0 )
        scaleAsyncTest( deploymentName, 1 )

        then:
        noExceptionThrown()
    }

    def "Connect to service, port 80."()
    {
        given:
        int status
        String body

        when:
        try( KubeConnection connection = instance.getConnection( serviceName, 80 ) )
        {
            HttpResponse<String> response = httpGet( connection )
            status = response.statusCode(  )
            body = response.body()
        }

        then:
        status == 200
        body != ""
        body.containsIgnoreCase( "nginx" )
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
