package lu.kremi151.jenkins.wolagent.slave;

import hudson.model.Node;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.SlaveComputer;
import lu.kremi151.jenkins.wolagent.launcher.WOLLauncher;

public class WOLSlaveComputer extends SlaveComputer {

    public WOLSlaveComputer(WOLSlave slave) {
        super(slave);
    }

    @Override
    protected ComputerLauncher grabLauncher(Node node) {
        if (!WOLSlave.class.isAssignableFrom(node.getClass())) {
            throw new IllegalArgumentException("Provided node must be of type " + WOLSlave.class.getName());
        }
        WOLSlave slave = (WOLSlave) node;
        return new WOLLauncher(
                WOLSlave.ensureNotNullWithDefault(slave.getLauncher()),
                slave.getMacAddress(),
                slave.getPingInterval(),
                slave.getConnectionTimeout(),
                slave.isAutoSuspend(),
                slave.isSuspendAsSuperuser(),
                slave.isIgnoreSessionsOnSuspend()
        );
    }

}
