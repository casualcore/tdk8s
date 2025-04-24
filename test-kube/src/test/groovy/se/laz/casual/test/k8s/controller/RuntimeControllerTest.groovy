/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.k8s.controller

import com.github.stefanbirkner.systemlambda.SystemLambda
import se.laz.casual.test.k8s.runtime.ContainerAwareness
import spock.lang.Specification

class RuntimeControllerTest extends Specification
{

    def "Is this running in a container or not."()
    {
        given:
        RuntimeControllerImpl instance = new RuntimeControllerImpl()

        when:
        SystemLambda.withEnvironmentVariable( ContainerAwareness.CONTAINER_ENV, value ).execute {
            ContainerAwareness.init()
        }

        then:
        instance.isInsideContainer() == expected

        where:
        value    || expected
        "oci"    || true
        "docker" || true
        "lXc"    || true
        "blah"   || true
        ""       || false
        null     || false
    }

}
