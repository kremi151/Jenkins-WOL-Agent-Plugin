/*
 * Copyright 2019 Michel Kremer (kremi151)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package lu.kremi151.jenkins.wolagent.slave;

import hudson.model.Node;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.SlaveComputer;
import lu.kremi151.jenkins.wolagent.launcher.WOLLauncher;

/**
 * Extension of {@link SlaveComputer} which enforces a {@link WOLLauncher} to be used as the
 * launcher.
 */
public class WOLSlaveComputer extends SlaveComputer {

    /**
     * Creates an instance of {@link WOLSlaveComputer}.
     * @param slave the slave of type {@link WOLSlave}
     */
    public WOLSlaveComputer(final WOLSlave slave) {
        super(slave);
    }

    @Override
    protected final ComputerLauncher grabLauncher(final Node node) {
        if (!WOLSlave.class.isAssignableFrom(node.getClass())) {
            throw new IllegalArgumentException("Provided node must be of type "
                    + WOLSlave.class.getName());
        }
        WOLSlave slave = (WOLSlave) node;
        return new WOLLauncher(
                WOLSlave.ensureNotNullWithDefault(slave.getLauncher()),
                slave.getMacAddress(),
                slave.getBroadcastIP(),
                slave.getPingInterval(),
                slave.getConnectionTimeout(),
                slave.getCommandBeforeDisconnect()
        );
    }

}
