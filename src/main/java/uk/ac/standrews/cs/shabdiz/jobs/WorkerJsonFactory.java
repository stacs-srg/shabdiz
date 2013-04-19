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
package uk.ac.standrews.cs.shabdiz.jobs;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class WorkerJsonFactory extends JsonFactory {

    private static final long serialVersionUID = 15401382709188608L;
    private static ObjectMapper worker_object_mapper;
    static {
        worker_object_mapper = new ObjectMapper();
        final Module worker_module = new WorkerModule();
        worker_object_mapper.registerModule(worker_module);
    }

    private static WorkerJsonFactory WORKER_JSON_FACTORY_INSTANCE = new WorkerJsonFactory();

    private WorkerJsonFactory() {

        super(worker_object_mapper);
    }

    public static WorkerJsonFactory getInstance() {

        return WORKER_JSON_FACTORY_INSTANCE;
    }

}
