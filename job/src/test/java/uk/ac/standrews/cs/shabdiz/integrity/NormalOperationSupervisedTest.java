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

package uk.ac.standrews.cs.shabdiz.integrity;

import java.io.File;
import net.schmizz.sshj.userauth.keyprovider.OpenSSHKeyFile;
import net.schmizz.sshj.userauth.method.AuthMethod;
import net.schmizz.sshj.userauth.method.AuthPublickey;
import net.schmizz.sshj.userauth.password.PasswordFinder;
import net.schmizz.sshj.userauth.password.Resource;
import org.junit.BeforeClass;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.standrews.cs.shabdiz.ApplicationState;
import uk.ac.standrews.cs.shabdiz.host.SSHHost;
import uk.ac.standrews.cs.shabdiz.host.exec.AgentBasedJavaProcessBuilder;
import uk.ac.standrews.cs.shabdiz.job.WorkerNetwork;
import uk.ac.standrews.cs.shabdiz.util.Input;
import uk.ac.standrews.cs.test.category.Ignore;

/**
 * Tests whether a deployed job returns expected result.
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
@Category(Ignore.class)
public class NormalOperationSupervisedTest extends NormalOperationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(NormalOperationSupervisedTest.class);

    @BeforeClass
    public static void setUp() throws Exception {

        final OpenSSHKeyFile key_provider = new OpenSSHKeyFile();
        key_provider.init(new File(System.getProperty("user.home") + File.separator + ".ssh", "id_rsa"), new PasswordFinder() {

            @Override
            public char[] reqPassword(final Resource<?> resource) {

                return Input.readPassword("private key passphrase");
            }

            @Override
            public boolean shouldRetry(final Resource<?> resource) {

                return false;
            }
        });
        AuthMethod public_key_authenticator = new AuthPublickey(key_provider);

        LOGGER.info("Local temp directory: {}", System.getProperty("java.io.tmpdir"));

        host = new SSHHost("blub.cs.st-andrews.ac.uk", public_key_authenticator);
        AgentBasedJavaProcessBuilder.clearCachedFilesOnHost(host);
        network = new WorkerNetwork();
        network.add(host);
        network.addCurrentJVMClasspath();
        network.deployAll();
        network.awaitAnyOfStates(ApplicationState.RUNNING);
        worker = network.first().getApplicationReference();
    }

}
