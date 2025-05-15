/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.connection;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;

/**
 * Checks if network connectivity conditions.
 */
public class NetworkChecker
{
    static Logger log = Logger.getLogger( NetworkChecker.class.getName());

    private NetworkChecker()
    {
    }

    /**
     * Check if it is possible to connect to the given host and port.
     *
     * @param host to connect to.
     * @param port to check.
     * @return if the connection was successful.
     */
    public static boolean canConnect( String host, int port )
    {
        try( Socket socket = new Socket() )
        {
            socket.connect( new InetSocketAddress( host, port ), 500 );
            return true;
        }
        catch( IOException e )
        {
            log.finest( ()-> "Unable to connect to: " + host + ":" + port );
            return false;
        }
    }

    /**
     * Get an available port.
     *
     * @return currently available port.
     */
    public static int getAvailablePort()
    {
        return createTestSocket( 0 );
    }

    /**
     * Check if a specific port is available.
     *
     * @param port to check.
     * @return if the port is currently available.
     */
    public static boolean isPortAvailable( int port )
    {
        try
        {
            createTestSocket( port );
            return true;
        }
        catch( IllegalStateException e )
        {
            return false;
        }
    }

    private static int createTestSocket( int port )
    {
        try( ServerSocket socket = new ServerSocket( port ) )
        {
            return socket.getLocalPort();
        }
        catch( IOException e )
        {
            throw new IllegalStateException( "Unexpected exception whilst trying to find an available port." );
        }
    }
}
