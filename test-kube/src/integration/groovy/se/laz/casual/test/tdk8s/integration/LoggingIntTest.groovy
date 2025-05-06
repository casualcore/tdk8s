/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.integration

import io.fabric8.kubernetes.api.model.Pod
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.KubernetesClientBuilder
import se.laz.casual.test.tdk8s.TestKube
import se.laz.casual.test.tdk8s.sample.NginxResources
import spock.lang.Shared
import spock.lang.Specification

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import static se.laz.casual.test.tdk8s.TestKube.RESOURCE_LABEL_NAME

class LoggingIntTest extends Specification
{
    @Shared
    KubernetesClient client = new KubernetesClientBuilder().build()
    @Shared
    String id = LoggingIntTest.class.getSimpleName(  )
    @Shared
    TestKube instance
    @Shared
    String podName = NginxResources.SIMPLE_NGINX_POD_NAME

    @Shared
    ZonedDateTime start = ZonedDateTime.now()

    def setupSpec()
    {

        List<Pod> pods = client.pods(  ).withLabel( RESOURCE_LABEL_NAME, id ).list().getItems(  )

        assert pods.size(  ) == 0

        instance = TestKube.newBuilder()
                .label( id )
                .addPod( podName, NginxResources.SIMPLE_NGINX_POD )
                .build()

        instance.init(  )
    }

    def cleanupSpec()
    {
        instance.destroy(  )

        List<Pod> pods = client.pods(  ).withLabel( RESOURCE_LABEL_NAME, id ).list().getItems(  )

        assert pods.size(  ) == 0
    }

    def "Retrieve full log from a pod."()
    {
        when:
        String actual = instance.getController().getLog( podName )

        then:
        actual != ""
        actual.containsIgnoreCase( "nginx" )
    }

    def "Retrieve last 10 lines of a pod."()
    {
        when:
        String fullLog = instance.getController().getLog( podName )
        String actual = instance.getController().getLogTail( podName, 10 )

        then:
        actual != ""
        actual.containsIgnoreCase( "start worker process" )
        actual.split( "\n" ).size(  ) == 10
        fullLog.endsWith( actual )
    }

    def "Retrieve logs since date."()
    {
        when:
        String fullLog = instance.getController().getLog( podName )
        String fromStartLog = instance.getController().getLogSince( podName, start.format( DateTimeFormatter.ISO_OFFSET_DATE_TIME ) )
        ZonedDateTime after = ZonedDateTime.now().plusSeconds( 1 ) // log time granularity is seconds.
        String afterLog = instance.getController().getLogSince( podName, after.format( DateTimeFormatter.ISO_OFFSET_DATE_TIME ))

        then:
        fullLog != ""
        fullLog.containsIgnoreCase( "nginx" )
        fullLog == fromStartLog
        afterLog != fromStartLog
    }
}
