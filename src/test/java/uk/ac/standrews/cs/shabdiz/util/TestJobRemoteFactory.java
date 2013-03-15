/*
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
package uk.ac.standrews.cs.shabdiz.util;

import uk.ac.standrews.cs.shabdiz.api.JobRemote;

/**
 * A factory for creating test {@link JobRemote} objects.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public final class TestJobRemoteFactory {

    /**
     * Instantiates a new test job remote factory.
     */
    private TestJobRemoteFactory() {

    }

    /**
     * Makes a remote job which echos the given message.
     * 
     * @param message_to_echo the message to echo
     * @return the job which echos the given message
     */
    public static JobRemote<String> makeEchoJob(final String message_to_echo) {

        return new EchoRemoteJob(message_to_echo);
    }

    /**
     * Makes a remote job which throws the given exception.
     * 
     * @param exception_to_throw the exception to throw
     * @return the job which throws the given exception
     */
    public static JobRemote<String> makeThrowExceptionJob(final Exception exception_to_throw) {

        return new ThrowExceptionRemoteJob(exception_to_throw);
    }

    public static final class EchoRemoteJob implements JobRemote<String> {

        private final String message_to_echo;

        public EchoRemoteJob(final String message_to_echo) {

            this.message_to_echo = message_to_echo;
        }

        private static final transient long serialVersionUID = -8715065957655698996L;

        @Override
        public String call() throws Exception {

            return message_to_echo;
        }
    }
}

final class ThrowExceptionRemoteJob implements JobRemote<String> {

    public Exception exception_to_throw;


    public ThrowExceptionRemoteJob() {

    }

    ThrowExceptionRemoteJob(final Exception exception_to_throw) {

        this.exception_to_throw = exception_to_throw;
    }

    private static final long serialVersionUID = 9089082845434872396L;

    @Override
    public String call() throws Exception {

        throw exception_to_throw;
    }
}
