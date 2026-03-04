/*
 * Copyright (c) 2025 - 2026, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.controller;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

/**
 * Validate incoming parameters to controller methods.
 */
public class ControllerParameterValidator
{

    private static final String RESOURCE = "Resource";
    private static final String REPLICAS = "Replicas";
    private static final String SERVICE = "Service";
    private static final String TARGET_PORT = "Target Port";
    private static final String COMMAND = "Command";
    private static final String LINES = "Lines";
    private static final String SINCE = "Since";
    private static final String SOURCE = "Source";
    private static final String DESTINATION = "Destination";
    private static final String IS_NULL = " is null.";
    private static final String IS_BLANK = " is blank: ";
    private static final String IS_EMPTY = " is empty: ";
    private static final String MUST_BE_GREATER_THAN_OR_EQUAL_TO_0_NOT = " must be greater than or equal to 0, not: ";

    private ControllerParameterValidator()
    {
    }

    public static void validateScale( String resource, int replicas )
    {
        validateString( resource, RESOURCE );
        validateInt( replicas, REPLICAS );
    }

    public static void validateScaleAsync( String resource, int replicas )
    {
        validateScale( resource, replicas );
    }

    public static void validateConnection( String service, int targetPort )
    {
        validateString( service, SERVICE );
        validateInt( targetPort, TARGET_PORT );
    }

    public static void validatePortForwardConnection( String resource, int targetPort )
    {
        validateString( resource, RESOURCE );
        validateInt( targetPort, TARGET_PORT );
    }

    public static void validateExecuteCommand( String resource, String... command )
    {
        validateString( resource, RESOURCE );
        validateStringArray( command, COMMAND );
    }

    public static void validateExecuteCommandAsync( String resource, String... command )
    {
        validateExecuteCommand( resource, command );
    }

    public static void validateGetLog( String resource )
    {
        validateString( resource, RESOURCE );
    }

    public static void validateGetLogTail( String resource, int lines )
    {
        validateGetLog( resource );
        validateInt( lines, LINES );
    }

    public static void validateGetLogSince( String resource, String sinceTime )
    {
        validateGetLog( resource );
        validateString( sinceTime, SINCE );
    }

    public static void validateDownload( String resource, String source, Path destination )
    {
        validateString( resource, RESOURCE );
        validateString( source, SOURCE );
        validatePath( destination, DESTINATION );
    }

    public static void validateUpload( String resource, Path source, String destination )
    {
        validateString( resource, RESOURCE );
        validatePath( source, SOURCE );
        validateString( destination, DESTINATION );
    }

    private static void validateString( String value, String name )
    {
        Objects.requireNonNull( value, name + IS_NULL );
        if( value.isBlank() )
        {
            throw new IllegalArgumentException( name + IS_BLANK + "'" + value + "'" );
        }
    }

    private static void validateStringArray( String[] value, String name )
    {
        Objects.requireNonNull( value, name + IS_NULL );
        if( value.length == 0 )
        {
            throw new IllegalArgumentException( name + IS_EMPTY + "'" + Arrays.toString( value ) + "'" );
        }
    }

    private static void validateInt( int value, String name )
    {
        if( value < 0 )
        {
            throw new IllegalArgumentException( name + MUST_BE_GREATER_THAN_OR_EQUAL_TO_0_NOT + value );
        }
    }

    private static void validatePath( Path path, String name )
    {
        Objects.requireNonNull( path, name + IS_NULL );
    }
}
