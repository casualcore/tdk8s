/*
 * Copyright (c) 2025 - 2026, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.controller

import io.fabric8.kubernetes.client.KubernetesClient
import se.laz.casual.test.tdk8s.TestKube
import se.laz.casual.test.tdk8s.controller.connection.ConnectionController
import se.laz.casual.test.tdk8s.controller.exec.ExecController
import se.laz.casual.test.tdk8s.controller.logging.LogController
import se.laz.casual.test.tdk8s.controller.provisioning.ProvisioningController
import se.laz.casual.test.tdk8s.controller.transfer.FileTransferController
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Path
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class KubeControllerTest extends Specification
{
    @Shared KubernetesClient client = Mock()
    @Shared TestKube testKube = Mock()
    @Shared String labelValue = UUID.randomUUID(  ).toString(  )

    KubeController instance

    def setup()
    {
        instance = newBuilder()
                .build(  )
    }

    KubeController.Builder newBuilder( )
    {
        KubeController.newBuilder(  )
                .testKube( testKube )
                .client( client )
                .label( labelValue )
    }

    def "Null throws NullPointerException"()
    {
        when:
        KubeController.newBuilder(  )
                .testKube( tk )
                .client( c )
                .label( l )
                .build(  )

        then:
        thrown NullPointerException

        where:
        tk       | c      | l
        testKube | client | null
        testKube | null   | labelValue
        testKube | null   | null
        null     | client | labelValue
        null     | client | null
        null     | null   | labelValue
        null     | null   | null
    }

    def "Provisioning delegates correctly."()
    {
        given:
        ProvisioningController pc = Mock()
        instance = newBuilder()
                .provisioningController( pc ).build(  )

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

    def "Provisioning controller checks for parameters before delegating scaling."()
    {
        given:
        ProvisioningController pc = Mock()
        instance = newBuilder()
                .provisioningController( pc ).build(  )

        when:
        instance.scale( deployment, replicas )

        then:
        thrown expected
        0* pc._

        when:
        instance.scaleAsync( deployment, replicas )

        then:
        thrown expected
        0* pc._

        where:
        deployment | replicas || expected
        null       | 1        || NullPointerException
        ""         | 1        || IllegalArgumentException
        " "        | 1        || IllegalArgumentException
        "mydep"    | -1       || IllegalArgumentException
    }

    def "Connection controller delegates correctly."()
    {
        given:
        ConnectionController cc = Mock()
        instance = newBuilder(  ).connectionController( cc ).build(  )
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

    def "Connection controller validates parameters before delegates correctly."()
    {
        given:
        ConnectionController cc = Mock()
        instance = newBuilder().connectionController( cc ).build()

        when:
        instance.getConnection( resource, port )

        then:
        thrown expected
        0* cc._

        when:
        instance.getPortForwardConnection( resource, port )

        then:
        thrown expected
        0* cc._

        where:
        resource | port || expected
        null     | 1    || NullPointerException
        ""       | 1    || IllegalArgumentException
        " "      | 1    || IllegalArgumentException
        "myres"  | -1   || IllegalArgumentException
    }

    def "Execution controller delegates correctly."()
    {
        given:
        ExecController ec = Mock()
        instance = newBuilder(  ).execController( ec ).build(  )
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

    def "Execution controller delegates correctly."()
    {
        given:
        ExecController ec = Mock()
        instance = newBuilder().execController( ec ).build()

        when:
        instance.executeCommand( name, (String[])command )

        then:
        thrown expected
        0* ec._

        when:
        instance.executeCommandAsync( name, (String[])command )

        then:
        thrown expected
        0* ec._

        where:
        name  | command                   || expected
        null  | ["sh", "-c", "echo 'hi'"] || NullPointerException
        ""    | ["sh", "-c", "echo 'hi'"] || IllegalArgumentException
        " "   | ["sh", "-c", "echo 'hi'"] || IllegalArgumentException
        "res" | null                      || NullPointerException
        "res" | []                        || IllegalArgumentException

    }

    def "Log controller delegates correctly."()
    {
        given:
        LogController lc = Mock()
        instance = newBuilder(  ).logController( lc ).build(  )
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

    def "Log controller validates parameters before delegates correctly, getLog."()
    {
        given:
        LogController lc = Mock()
        instance = newBuilder().logController( lc ).build()

        when:
        instance.getLog( name )

        then:
        thrown expected
        0* lc._

        where:
        name || expected
        null || NullPointerException
        ""   || IllegalArgumentException
        " "  || IllegalArgumentException
    }

    def "Log controller validates parameters before delegates correctly, getLogTail."()
    {
        given:
        LogController lc = Mock()
        instance = newBuilder().logController( lc ).build()

        when:
        instance.getLogTail( name, lines )

        then:
        thrown expected
        0* lc._

        where:
        name  | lines || expected
        null  | 10    || NullPointerException
        ""    | 10    || IllegalArgumentException
        " "   | 10    || IllegalArgumentException
        "res" | -1    || IllegalArgumentException

    }

    def "Log controller validates parameters before delegates correctly, getLogSince."()
    {
        given:
        LogController lc = Mock()
        instance = newBuilder().logController( lc ).build()

        when:
        instance.getLogSince( name, since )

        then:
        thrown expected
        0* lc._

        where:
        name  | since                                                                || expected
        null  | ZonedDateTime.now().format( DateTimeFormatter.ISO_OFFSET_DATE_TIME ) || NullPointerException
        ""    | ZonedDateTime.now().format( DateTimeFormatter.ISO_OFFSET_DATE_TIME ) || IllegalArgumentException
        " "   | ZonedDateTime.now().format( DateTimeFormatter.ISO_OFFSET_DATE_TIME ) || IllegalArgumentException
        "res" | null                                                                 || NullPointerException
        "res" | ""                                                                   || IllegalArgumentException
        "res" | " "                                                                  || IllegalArgumentException

    }

    def "File transfer controller delegates correctly."()
    {
        given:
        FileTransferController fc = Mock()
        instance = newBuilder(  ).fileTransferController( fc ).build(  )
        String name = "my-pod"
        String localFile = "./file.txt"
        Path containerFile = Path.of( "dest.txt" )

        when:
        instance.download( name, localFile, containerFile )

        then:
        1* fc.download( name, localFile, containerFile )

        when:
        instance.upload( name, containerFile, localFile )

        then:
        1* fc.upload( name, containerFile, localFile )
    }

    def "File transfer controller validates parameters before delegates correctly."()
    {
        given:
        FileTransferController fc = Mock()
        instance = newBuilder(  ).fileTransferController( fc ).build(  )

        when:
        instance.download( name, containerFile, localFile )

        then:
        thrown expected
        0* fc._

        when:
        instance.upload( name, localFile, containerFile )

        then:
        thrown expected
        0* fc._

        where:
        name     | containerFile | localFile             || expected
        null     | "./file.txt"  | Path.of( "dest.txt" ) || NullPointerException
        ""       | "./file.txt"  | Path.of( "dest.txt" ) || IllegalArgumentException
        " "      | "./file.txt"  | Path.of( "dest.txt" ) || IllegalArgumentException
        "my-pod" | null          | Path.of( "dest.txt" ) || NullPointerException
        "my-pod" | ""            | Path.of( "dest.txt" ) || IllegalArgumentException
        "my-pod" | " "           | Path.of( "dest.txt" ) || IllegalArgumentException
        "my-pod" | "./file.txt"  | null                  || NullPointerException
    }
}
