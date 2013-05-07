/*
 * Copyright 2013 University of St Andrews School of Computer Science
 * 
 * This file is part of Shabdiz.
 * 
 * Shabdiz is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Shabdiz is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Shabdiz.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.standrews.cs.shabdiz.job;

import java.io.Serializable;
import java.util.UUID;

import com.staticiser.jetson.exception.JsonRpcException;

/**
 * Receives notifications from workers about the outcome of a submitted job.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public interface WorkerCallback {

    /**
     * Notifies the launcher about the result of a completed job.
     * 
     * @param job_id the globally unique id of the submitted job
     * @param result the result of the completed job
     * @throws JsonRpcException if unable to contact the correspondence
     */

    void notifyCompletion(UUID job_id, Serializable result) throws JsonRpcException;

    /**
     * Notifies the launcher about the exception resulted by executing a job.
     * 
     * @param job_id the globally unique id of the submitted job
     * @param exception the exception which occurred when trying to execute a job
     * @throws JsonRpcException if unable to contact the correspondence
     */
    void notifyException(UUID job_id, Exception exception) throws JsonRpcException;
}
