/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.controller

import io.fabric8.kubernetes.client.dsl.ExecWatch
import io.fabric8.kubernetes.client.dsl.PodResource
import io.fabric8.kubernetes.client.dsl.TtyExecErrorChannelable
import io.fabric8.kubernetes.client.dsl.TtyExecErrorable
import se.laz.casual.test.tdk8s.exec.ExecResult
import se.laz.casual.test.tdk8s.store.ResourceNotFoundException
import spock.lang.Shared
import spock.lang.Specification

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException

class ExecControllerTest extends Specification
{
    ResourceLookupController rlc = Mock()
    ExecController instance

    @Shared String name = "my-pod"
    @Shared String[] command = ["sh", "-c", "echo unittest" ]

    def setup()
    {
        instance = new ExecControllerImpl( rlc )
    }

    def "Execute command, sync."()
    {
        given:
        ExecWatch watch = Mock()
        initialiseMocks( output, exit, watch )

        ExecResult expected = ExecResult.newBuilder( ).exitCode( exit ).output( output ).build(  )

        when:
        ExecResult actual = instance.executeCommand( name, command )

        then:
        actual == expected
        1* watch.close(  )

        where:
        output     || exit
        "unittest" || 0
        ""         || 1
    }

    def "Execute command, sync, pod not found."()
    {
        given:
        1* rlc.getPodResource( name ) >> Optional.empty(  )

        when:
        instance.executeCommand( name, command )

        then:
        thrown ResourceNotFoundException
    }

    def "Execute command, async."()
    {
        given:
        ExecWatch watch = Mock()
        initialiseMocks( output, exit, watch )

        ExecResult expected = ExecResult.newBuilder( ).exitCode( exit ).output( output ).build(  )

        when:
        ExecResult actual = instance.executeCommandAsync( name, command ).join(  )

        then:
        actual == expected
        1* watch.close(  )

        where:
        output     || exit
        "unittest" || 0
        ""         || 1
    }

    def "Execute command, sync, pod not found."()
    {
        given:
        1* rlc.getPodResource( name ) >> Optional.empty(  )

        when:
        try
        {
            instance.executeCommandAsync( name, command ).join()
        }
        catch( CompletionException ce )
        {
            throw ce.getCause(  )
        }

        then:
        thrown ResourceNotFoundException
    }

    void initialiseMocks( String output, Integer exit, ExecWatch watch )
    {
        PodResource resource = Mock()
        1* rlc.getPodResource( name ) >> Optional.of( resource )
        TtyExecErrorable tee = Mock()
        1* resource.writingOutput( _ ) >> { OutputStream out ->
            out.write( output.getBytes() )
            return tee
        }
        TtyExecErrorChannelable teec = Mock()
        1* tee.writingError( _ ) >> { OutputStream out ->
            return teec
        }

        1* teec.exec( command ) >> watch
        CompletableFuture<Integer> exitCode = new CompletableFuture<>()
        exitCode.complete( exit )
        1* watch.exitCode(  ) >> exitCode
    }

}
