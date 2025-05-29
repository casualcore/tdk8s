/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.controller.exec;

import se.laz.casual.test.tdk8s.exec.ExecResult;
import se.laz.casual.test.tdk8s.store.ResourceNotFoundException;

import java.util.concurrent.CompletableFuture;

public interface ExecController
{
    /**
     * Executes the provided command on the resource and wait until completed.
     * <p>
     *     The resource name can be either the alias for the managed resource
     *     or the actual underlying name of the resource inside the cluster.
     * </p>
     * <p>
     *     Note: If the resource has multiple pods associated the first is used.
     * </p>
     *
     * @param resource on which to run the command.
     * @param command the command to run.
     * @return the result of running the command.
     * @throws ResourceNotFoundException if the resource was not found.
     */
    ExecResult executeCommand( String resource, String... command );

    /**
     * Executes the provided command on the resource without waiting for the result.
     * <p>
     *     The resource name can be either the alias for the managed resource
     *     or the actual underlying name of the resource inside the cluster.
     * </p>
     * <p>
     *     Note: If the resource has multiple pods associated the first is used.
     * </p>
     *
     * @param resource on which to run the command.
     * @param command the command to run.
     * @return the result of the running command as a future.
     */
    CompletableFuture<ExecResult> executeCommandAsync( String resource, String... command );
}
