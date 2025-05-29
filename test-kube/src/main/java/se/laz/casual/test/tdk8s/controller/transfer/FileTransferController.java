/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.controller.transfer;

import se.laz.casual.test.tdk8s.store.ResourceNotFoundException;

import java.nio.file.Path;

public interface FileTransferController
{
    /**
     * Downloads from the resource the source file specified on the pod, to
     * the destination specified on the local filesystem.
     * <p>
     *     The resource name can be either the alias for the managed resource
     *     or the actual underlying name of the resource inside the cluster.
     * </p>
     * <p>
     *     Note: If the resource has multiple pods associated the first is used.
     * </p>
     *
     * @param resource from which to download.
     * @param source file on the resource to download.
     * @param destination file on the local filesystem to which to save the download.
     * @return if the operation was successful.
     * @throws ResourceNotFoundException if the resource was not found.
     */
    boolean download( String resource, String source, Path destination );

    /**
     * Uploads to the resource the source file specific on the local filesystem, to
     * the destination specified in the pod.
     * <p>
     *     The resource name can be either the alias for the managed resource
     *     or the actual underlying name of the resource inside the cluster.
     * </p>
     * <p>
     *     Note: If the resource has multiple pods associated the first is used.
     * </p>
     *
     * @param pod to which to upload.
     * @param source file on the local filesystem to upload.
     * @param destination file on the resource to which to save the upload.
     * @return if the operation was successful.
     * @throws ResourceNotFoundException if the resource was not found.
     */
    boolean upload( String pod, String source, Path destination );
}
