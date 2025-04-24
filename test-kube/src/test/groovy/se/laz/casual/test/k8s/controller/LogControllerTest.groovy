/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.k8s.controller

import io.fabric8.kubernetes.client.dsl.PodResource
import io.fabric8.kubernetes.client.dsl.PrettyLoggable
import io.fabric8.kubernetes.client.dsl.TailPrettyLoggable
import se.laz.casual.test.k8s.store.ResourceNotFoundException
import spock.lang.Specification

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class LogControllerTest extends Specification
{

    ResourceLookupController lookup = Mock()

    LogController instance

    def setup()
    {
        instance = new LogControllerImpl( lookup )
    }

    def "Retrieve full log."()
    {
        given:
        String name = "podName"
        String expected = "this is the lovely log."
        PodResource pr = Mock(PodResource)
        1* lookup.getPodResource( name ) >> Optional.ofNullable( pr )
        1* pr.getLog(  ) >> expected


        when:
        String actual = instance.getLog( name )

        then:
        actual == expected
    }

    def "Retrieve tail log."()
    {
        given:
        String name = "podName"
        int lines = 10
        String expected = "this is the lovely log."
        PodResource pr = Mock(PodResource)
        1* lookup.getPodResource( name ) >> Optional.ofNullable( pr )
        PrettyLoggable pl = Mock( PrettyLoggable)
        1* pr.tailingLines( lines ) >> pl
        1* pl.getLog(  ) >> expected


        when:
        String actual = instance.getLogTail( name, lines )

        then:
        actual == expected
    }

    def "Retrieve since log."()
    {
        given:
        String name = "podName"
        String since = ZonedDateTime.now().format( DateTimeFormatter.ISO_OFFSET_DATE_TIME )
        String expected = "this is the lovely log."
        PodResource pr = Mock(PodResource)
        1* lookup.getPodResource( name ) >> Optional.ofNullable( pr )
        TailPrettyLoggable tpl = Mock( TailPrettyLoggable)
        1* pr.sinceTime( since ) >> tpl
        1* tpl.getLog(  ) >> expected

        when:
        String actual = instance.getLogSince( name, since )

        then:
        actual == expected
    }

    def "Retrieve full log for unknown, throws ResourceNotFoundException."()
    {
        given:
        String name = "podName"

        1* lookup.getPodResource( name ) >> Optional.empty(  )

        when:
        instance.getLog( name )

        then:
        thrown ResourceNotFoundException
    }

    def "Retrieve tail log for unknown, throws ResourceNotFoundException."()
    {
        given:
        String name = "podName"

        1* lookup.getPodResource( name ) >> Optional.empty(  )

        when:
        instance.getLogTail( name, 10 )

        then:
        thrown ResourceNotFoundException
    }

    def "Retrieve since log for unknown, throws ResourceNotFoundException."()
    {
        given:
        String name = "podName"

        1* lookup.getPodResource( name ) >> Optional.empty(  )

        when:
        instance.getLogSince( name, ZonedDateTime.now().format( DateTimeFormatter.ISO_OFFSET_DATE_TIME ) )

        then:
        thrown ResourceNotFoundException
    }
}
