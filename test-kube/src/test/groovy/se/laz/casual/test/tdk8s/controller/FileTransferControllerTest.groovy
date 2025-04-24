/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.controller

import io.fabric8.kubernetes.client.dsl.CopyOrReadable
import io.fabric8.kubernetes.client.dsl.PodResource
import se.laz.casual.test.tdk8s.store.ResourceNotFoundException
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Path

class FileTransferControllerTest extends Specification
{
    ResourceLookupController rlc = Mock()

    FileTransferController instance

    @Shared String name = "my-pod"
    @Shared String src = "myfile"
    @Shared Path dest = Path.of( "dest.txt" )

    def setup()
    {
        instance = new FileTransferControllerImpl( rlc )
    }

    def "Download file."()
    {
        given:
        PodResource resource = Mock()
        1* rlc.getPodResource( name ) >> Optional.of( resource )
        CopyOrReadable cor = Mock( )
        1* resource.file( src ) >> cor
        1* cor.copy( dest ) >> success

        when:
        boolean actual = instance.download( name, src, dest )

        then:
        actual == success

        where:
        success << [
                true, false
        ]
    }

    def "Download file, pod doesn't exist."()
    {
        given:

        1* rlc.getPodResource( name ) >> Optional.empty(  )

        when:
        instance.download( name, src, dest )

        then:
        thrown ResourceNotFoundException
    }

    def "Upload file."()
    {
        given:
        PodResource resource = Mock()
        1* rlc.getPodResource( name ) >> Optional.of( resource )
        CopyOrReadable cor = Mock( )
        1* resource.file( src ) >> cor
        1* cor.upload( dest ) >> success

        when:
        boolean actual = instance.upload( name, src, dest )

        then:
        actual == success

        where:
        success << [
                true, false
        ]
    }

    def "Upload file, pod doesn't exist."()
    {
        given:

        1* rlc.getPodResource( name ) >> Optional.empty(  )

        when:
        instance.upload( name, src, dest )

        then:
        thrown ResourceNotFoundException
    }

}
