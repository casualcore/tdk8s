/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.k8s.controller

import se.laz.casual.test.k8s.connection.NetworkChecker
import spock.lang.Specification

class NetworkControllerTest extends Specification
{
    NetworkController instance

    def setup()
    {
        instance = new NetworkController()
    }

    def "Check connection."()
    {
        given:
        int availablePort = NetworkChecker.getAvailablePort(  )

        when:
        boolean canConnect = instance.canConnect( "localhost", availablePort )

        then:
        !canConnect

        when:
        ServerSocket socket = new ServerSocket( availablePort )
        canConnect = instance.canConnect( "localhost", availablePort )

        then:
        canConnect

        when:
        socket.close(  )
        canConnect = instance.canConnect( "localhost", availablePort )

        then:
        !canConnect

        cleanup:
        socket.close()
    }

}
