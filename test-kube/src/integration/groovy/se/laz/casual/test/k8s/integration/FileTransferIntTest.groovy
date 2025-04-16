/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.k8s.integration

import io.fabric8.kubernetes.api.model.Pod
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.KubernetesClientBuilder
import se.laz.casual.test.k8s.TestKube
import se.laz.casual.test.k8s.sample.NginxResources
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path

class FileTransferIntTest extends Specification
{
    @Shared
    KubernetesClient client = new KubernetesClientBuilder().build()
    @Shared
    String id = FileTransferIntTest.class.getSimpleName(  )
    @Shared
    TestKube instance
    @Shared
    String podName = NginxResources.SIMPLE_NGINX_POD_NAME

    def setupSpec()
    {

        List<Pod> pods = client.pods(  ).withLabel( "TestKube", id ).list().getItems(  )

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

        List<Pod> pods = client.pods(  ).withLabel( "TestKube", id ).list().getItems(  )

        assert pods.size(  ) == 0
    }

    def "Retrieve file from the pod."()
    {
        given:
        Path p = Files.createTempFile( "FileTransferIntTest", "txt" )

        when:
        boolean successful = client.pods( ).withName( podName ).file( "/docker-entrypoint.sh" ).copy( p )

        String actual = new String( Files.readAllBytes( p ) )

        then:
        successful
        actual != ""
        actual.startsWith( "#!/bin/sh" )

        cleanup:
        p.toFile(  ).delete(  )
    }

    def "Upload file to the pod."()
    {
        given:
        File f = new File( "./src/integration/resources/test.txt")

        assert f.exists(  )

        when:
        boolean successful = client.pods().withName( podName ).file( "/tmp/test.txt").upload( f.toPath(  ) )

        then:
        successful
    }
}
