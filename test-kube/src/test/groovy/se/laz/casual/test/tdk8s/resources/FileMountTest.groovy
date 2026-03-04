/*
 * Copyright (c) 2026, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.resources

import io.fabric8.kubernetes.api.model.ConfigMap
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Paths

class FileMountTest extends Specification
{

    @Shared
    ConfigMap map = ConfigMapFactory.fromFile( "my-map", Paths.get( "src/test/resources/test.txt") )
    @Shared
    String mountPath = "/mnt/test.txt"
    @Shared
    String volume = "custom-vol-01"
    @Shared
    String container = "container-name-01"

    def "Create and then get."()
    {
        when:
        FileMount instance = FileMount.newBuilder()
                .configMap( map )
                .mountPath( mountPath )
                .volume( volume )
                .container( container )
                .build()

        then:
        instance.getConfigMap() == map
        instance.getMountPath() == mountPath
        instance.getVolume() == volume
        instance.getContainer() == container
    }

    def "Get minimal, with defaults."()
    {
        when:
        FileMount instance = FileMount.newBuilder()
                .configMap( map )
                .mountPath( mountPath )
        .build()

        then:
        instance.getConfigMap() == map
        instance.getMountPath() == mountPath
        instance.getVolume() == FileMount.DEFAULT_VOLUME_NAME
        instance.getContainer() == null
    }

}
