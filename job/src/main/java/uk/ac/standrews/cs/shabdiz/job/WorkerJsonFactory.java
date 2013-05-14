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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;

final class WorkerJsonFactory extends JsonFactory {

    private static final long serialVersionUID = 15401382709188608L;
    private static final WorkerJsonFactory WORKER_JSON_FACTORY_INSTANCE = new WorkerJsonFactory();
    private static final ObjectMapper WORKER_OBJECT_MAPPER;

    static {
        WORKER_OBJECT_MAPPER = new ObjectMapper();
        final Module worker_module = new WorkerModule();
        WORKER_OBJECT_MAPPER.registerModule(worker_module);
    }

    private WorkerJsonFactory() {

        super(WORKER_OBJECT_MAPPER);
    }

    static WorkerJsonFactory getInstance() {

        return WORKER_JSON_FACTORY_INSTANCE;
    }

}
