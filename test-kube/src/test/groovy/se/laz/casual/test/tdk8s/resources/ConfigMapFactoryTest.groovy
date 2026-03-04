/*
 * Copyright (c) 2026, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.resources

import io.fabric8.kubernetes.api.model.ConfigMap
import io.fabric8.kubernetes.api.model.ConfigMapBuilder
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class ConfigMapFactoryTest extends Specification
{
    def "Create configmap from file provided."()
    {
        given:
        String mapName = "mymap"
        Path file = Paths.get( "src/test/resources/test.txt" )

        ConfigMap expected = new ConfigMapBuilder(  )
                .withNewMetadata(  )
                    .withName( mapName )
                .endMetadata(  )
                .addToData( "test.txt", Files.readString( file ) )
                .build(  )

        when:
        ConfigMap actual = ConfigMapFactory.fromFile( mapName, file )

        then:
        actual == expected
    }

    def "Create with nulls, throws NullPointerException."()
    {
        when:
        ConfigMapFactory.fromFile( name, file )

        then:
        thrown NullPointerException

        where:
        name  | file
        "map" | null
        null  | Paths.get( "src/test/resources/test.txt" )
        null  | null
    }

    def "Create with non existing file."()
    {
        when:
        ConfigMapFactory.fromFile( "map", file )

        then:
        thrown IllegalArgumentException

        where:
        file << [
            Paths.get("src/test/res.txt" ),
            Paths.get( "src/test/resources/invalid.txt")
        ]
    }
}
