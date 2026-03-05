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
        return fromFiles( name, file );
    }

    /**
     * Create a ConfigMap called name, with data from each of the files provided.
     * The data key for the files will be the filename of the file path provided.
     *
     * @param name of the ConfigMap.
     * @param files 1 or more files to read as data for the ConfigMap.
     * @return ConfigMap containing the files.
     * @throws IOException if there is an issue whilst reading the files.
     */
    public static ConfigMap fromFiles( String name, Path... files ) throws IOException
    {
        validate( name, files );

        ConfigMapBuilder builder =  new ConfigMapBuilder()
                .withNewMetadata()
                .withName( name )
                .endMetadata();
        for( Path file: files )
        {
                builder.addToData( file.getFileName().toString(), Files.readString( file ) );
        }
        return builder.build();
    }

    private static void validate( String name, Path...files )
    {
        Objects.requireNonNull( name, "Name is null." );
        Objects.requireNonNull( files, "Files is null." );

        for( Path file: files )
        {
            Objects.requireNonNull( file, "Files is null." );
            if( !Files.exists( file ) )
            {
                throw new IllegalArgumentException( "File does not exist." );
            }
        }
    }
}
