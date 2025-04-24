/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.connection

import spock.lang.Specification

class NetworkCheckerTest extends Specification
{

    def "Get available port multiple times."()
    {
        when:
        int actual = NetworkChecker.getAvailablePort()

        then:
        actual > 0

        and:
        int actual2 = NetworkChecker.getAvailablePort()

        then:
        actual2 > 0
        actual != actual2
    }

    def "Get available port then use, ask for port again."()
    {
        given:
        int availablePort = NetworkChecker.getAvailablePort()

        when:
        boolean available = NetworkChecker.isPortAvailable( availablePort )

        then:
        available

        when:
        ServerSocket socket = new ServerSocket( availablePort )

        then:
        socket.isBound()

        when:
        available = NetworkChecker.isPortAvailable( availablePort )

        then:
        !available

        cleanup:
        socket.close()
    }

    def "Check connection."()
    {
        given:
        int availablePort = NetworkChecker.getAvailablePort(  )

        when:
        boolean canConnect = NetworkChecker.canConnect( "localhost", availablePort )

        then:
        !canConnect

        when:
        ServerSocket socket = new ServerSocket( availablePort )
        canConnect = NetworkChecker.canConnect( "localhost", availablePort )

        then:
        canConnect

        when:
        socket.close(  )
        canConnect = NetworkChecker.canConnect( "localhost", availablePort )

        then:
        !canConnect

        cleanup:
        socket.close()
    }

}
