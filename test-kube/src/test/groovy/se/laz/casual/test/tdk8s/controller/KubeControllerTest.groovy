/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.controller

import spock.lang.Specification

import java.nio.file.Path
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class KubeControllerTest extends Specification
{
    KubeController instance

    def setup()
    {
        instance = KubeController.newBuilder(  ).build(  )
    }

    def "Provisioning delegates correctly."()
    {
        given:
        ProvisioningController pc = Mock()
        instance = KubeController.newBuilder(  ).provisioningController( pc ).build(  )

        when:
        instance.init(  )
        then:
        1* pc.init(  )

        when:
        instance.destroy(  )
        then:
        1* pc.destroy(  )

        when:
        instance.initAsync(  )
        then:
        1* pc.initAsync(  )

        when:
        instance.waitUntilReady(  )
        then:
        1* pc.waitUntilReady(  )

        when:
        instance.destroyAsync(  )
        then:
        1* pc.destroyAsync(  )

        when:
        instance.waitUntilDestroyed(  )
        then:
        1* pc.waitUntilDestroyed(  )

        when:
        instance.scale( "name", 1 )
        then:
        1* pc.scale( "name", 1 )

        when:
        instance.scaleAsync( "name", 2 )
        then:
        1* pc.scaleAsync( "name", 2 )
    }

    def "Connection controller delegates correctly."()
    {
        given:
        ConnectionController cc = Mock()
        instance = KubeController.newBuilder(  ).connectionController( cc ).build(  )
        String name = "my-service"
        int port = 8080

        when:
        instance.getConnection( name, port )

        then:
        1* cc.getConnection( name, port )

        when:
        instance.getPortForwardConnection( name, port )

        then:
        1* cc.getPortForwardConnection( name, port )
    }

    def "Execution controller delegates correctly."()
    {
        given:
        ExecController ec = Mock()
        instance = KubeController.newBuilder(  ).execController( ec ).build(  )
        String name = "my-pod"
        String[] command = ["sh", "-c", "echo 'hi'"]

        when:
        instance.executeCommand( name, command )

        then:
        1* ec.executeCommand( name, command )

        when:
        instance.executeCommandAsync( name, command )

        then:
        1* ec.executeCommandAsync( name, command )
    }

    def "Log controller delegates correctly."()
    {
        given:
        LogController lc = Mock()
        instance = KubeController.newBuilder(  ).logController( lc ).build(  )
        String name = "my-pod"
        int lines = 10
        String since = ZonedDateTime.now().format( DateTimeFormatter.ISO_OFFSET_DATE_TIME )

        when:
        instance.getLog( name )

        then:
        1* lc.getLog( name )

        when:
        instance.getLogTail( name, lines )

        then:
        1* lc.getLogTail( name, lines )

        when:
        instance.getLogSince( name, since )

        then:
        1* lc.getLogSince( name, since )
    }

    def "File transfer controller delegates correctly."()
    {
        given:
        FileTransferController fc = Mock()
        instance = KubeController.newBuilder(  ).fileTransferController( fc ).build(  )
        String name = "my-pod"
        String source = "./file.txt"
        Path dest = Path.of( "dest.txt" )

        when:
        instance.download( name, source, dest )

        then:
        1* fc.download( name, source, dest )

        when:
        instance.upload( name, source, dest )

        then:
        1* fc.upload( name, source, dest )
    }
}
