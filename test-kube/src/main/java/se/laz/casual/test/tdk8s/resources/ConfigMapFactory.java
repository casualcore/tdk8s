/*
 * Copyright (c) 2026, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.resources;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Factory to assist with creation of ConfigMaps from
 * for example local files.
 */
public final class ConfigMapFactory
{
    private ConfigMapFactory()
    {
    }

    /**
     * Create a ConfigMap called name, with data from the file provided.
     * The data key for the file will be the filename of the file path provided.
     *
     * @param name of the ConfigMap.
     * @param file to read as data for the ConfigMap.
     * @return ConfigMap containing the file.
     * @throws IOException if there is an issue whilst reading the file.
     */
    public static ConfigMap fromFile( String name, Path file ) throws IOException
    {
        validate( name, file );

        return new ConfigMapBuilder()
                .withNewMetadata()
                    .withName( name )
                .endMetadata()
                .addToData( file.getFileName().toString(), Files.readString( file ) )
                .build();
    }

    private static void validate( String name, Path file )
    {
        Objects.requireNonNull( name, "Name is null." );
        Objects.requireNonNull( file, "File is null." );

        if( !Files.exists( file ) )
        {
            throw new IllegalArgumentException( "File does not exist." );
        }
    }
}
