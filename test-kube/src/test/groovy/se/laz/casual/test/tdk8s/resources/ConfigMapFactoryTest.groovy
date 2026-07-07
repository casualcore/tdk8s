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

    def "Create with null within Path resolution, throws IllegalArgumentException."()
    {
        given:
        Path path = Paths.get( "/" )

        when:
        ConfigMapFactory.fromFile(  "map", path )

        then:
        thrown IllegalArgumentException
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

    def "Create with multiple files."()
    {
        given:
        String mapName = "mymap"
        Path file = Paths.get( "src/test/resources/test.txt" )
        Path file2 = Paths.get( "src/test/resources/test2.txt" )

        ConfigMap expected = new ConfigMapBuilder(  )
                .withNewMetadata(  )
                .withName( mapName )
                .endMetadata(  )
                .addToData( "test.txt", Files.readString( file ) )
                .addToData( "test2.txt", Files.readString( file2 ) )
                .build(  )

        when:
        ConfigMap actual = ConfigMapFactory.fromFiles( mapName, file, file2 )

        then:
        actual == expected
    }

    def "Create with multiple files, with non existent file throws IllegalArgumentException."()
    {
        given:
        String mapName = "mymap"
        Path file = Paths.get( "src/test/resources/test.txt" )
        Path file2 = Paths.get( "src/test/resources/invalid.txt" )

        when:
        ConfigMapFactory.fromFiles( mapName, file, file2 )

        then:
        thrown IllegalArgumentException
    }

    def "Create with nulls, throws NullPointerException."()
    {
        when:
        ConfigMapFactory.fromFiles( name, file, file2 )

        then:
        thrown NullPointerException

        where:
        name  | file                                       | file2
        "map" | Paths.get( "src/test/resources/test.txt" ) | null
        "map" | null                                       | Paths.get( "src/test/resources/test2.txt" )
        null  | Paths.get( "src/test/resources/test.txt" ) | Paths.get( "src/test/resources/test2.txt" )
        null  | null                                       | null
    }
}
