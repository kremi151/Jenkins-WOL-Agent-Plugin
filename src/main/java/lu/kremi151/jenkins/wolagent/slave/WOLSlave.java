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

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Slave;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.JNLPLauncher;
import hudson.util.FormValidation;
import lu.kremi151.jenkins.wolagent.Messages;
import lu.kremi151.jenkins.wolagent.launcher.WOLLauncher;
import lu.kremi151.jenkins.wolagent.util.HostHelper;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Information about a Hudson agent node that relies on Wake-on-LAN.
 */
public final class WOLSlave extends Slave implements Serializable {

    private static final Logger LOGGER = java.util.logging.Logger.getLogger(
            WOLSlave.class.getName());

    private String macAddress;
    private String broadcastIP;

    private ComputerLauncher launcher;

    private int pingInterval;
    private int connectionTimeout;

    private String commandBeforeDisconnect;

    /**
     * Creates an instance of {@link WOLSlave}.
     * @param name                      The name of the {@link WOLSlave}.
     * @param remoteFS                  The file system root on the target machine from the point
     *                                  of view of this node.
     * @param launcher                  The starter that will startup this agent, either an
     *                                  instance of {@link WOLLauncher} or the real one.
     * @param macAddress                The MAC address to be used to broadcast the magic packet.
     * @param broadcastIP               The broadcast IP address to be used to broadcast the magic
     *                                  packet.
     * @param pingInterval              The interval in milliseconds to ping the target machine
     *                                  until it is available.
     * @param connectionTimeout         The maximal amount of time to wait in milliseconds until
     *                                  the machine is available.
     * @param commandBeforeDisconnect   The optional command to execute on the target machine
     *                                  before the agent disconnects.
     * @throws Descriptor.FormException Thrown in case of a Stapler related error.
     * @throws IOException              Thrown in case of an I/O error.
     */
    @DataBoundConstructor
    public WOLSlave(
            @Nonnull final String name,
            final String remoteFS,
            final ComputerLauncher launcher,
            final String macAddress,
            final String broadcastIP,
            final int pingInterval,
            final int connectionTimeout,
            final String commandBeforeDisconnect
    ) throws Descriptor.FormException, IOException {
        super(name, remoteFS, null);
        this.macAddress = macAddress;
        this.broadcastIP = broadcastIP;
        this.pingInterval = pingInterval;
        this.connectionTimeout = connectionTimeout;
        this.commandBeforeDisconnect = commandBeforeDisconnect;

        ComputerLauncher unpacked = WOLLauncher.unpackLauncher(launcher);
        LOGGER.log(Level.INFO,
                "Construct delegate launcher of type {0}",
                WOLLauncher.getLauncherClassName(unpacked));
        this.launcher = ensureNotNullWithDefault(unpacked);
    }

    @Override
    public ComputerLauncher getLauncher() {
        LOGGER.log(Level.INFO, "Get launcher");
        ComputerLauncher launcher = WOLLauncher.unpackLauncher(this.launcher);
        return ensureNotNullWithDefault(launcher);
    }

    @Override
    @DataBoundSetter
    public void setLauncher(final ComputerLauncher launcher) {
        ComputerLauncher unpacked = WOLLauncher.unpackLauncher(launcher);
        LOGGER.log(Level.INFO, "Set launcher of type {0}",
                WOLLauncher.getLauncherClassName(unpacked));
        this.launcher = ensureNotNullWithDefault(unpacked);
    }

    /**
     * Set the MAC address to use for sending the magic packet.
     * @param macAddress The MAC address.
     */
    @DataBoundSetter
    public void setMacAddress(final String macAddress) {
        LOGGER.log(Level.INFO, "Set mac address to {0}", macAddress);
        this.macAddress = macAddress;
    }

    /**
     * Gets the MAC address to use for sending the magic packet.
     * @return The MAC address.
     */
    public String getMacAddress() {
        return macAddress;
    }

    /**
     * Sets the optional command to execute on the target machine before the agent disconnects.
     * @param commandBeforeDisconnect The optional command.
     */
    @DataBoundSetter
    public void setCommandBeforeDisconnect(@Nullable final String commandBeforeDisconnect) {
        LOGGER.log(Level.INFO, "Set command before disconnect to {0}", commandBeforeDisconnect);
        this.commandBeforeDisconnect = commandBeforeDisconnect;
    }

    /**
     * Gets the optional command to execute on the target machine before the agent disconnects.
     * @return The optional command.
     */
    @Nullable
    public String getCommandBeforeDisconnect() {
        return commandBeforeDisconnect;
    }

    /**
     * Sets the interval in milliseconds to ping the target machine until it is available.
     * @param pingInterval The interval in milliseconds.
     */
    @DataBoundSetter
    public void setPingInterval(final int pingInterval) {
        LOGGER.log(Level.INFO, "Set ping interval to {0}", pingInterval);
        this.pingInterval = pingInterval;
    }

    /**
     * Gets the interval in milliseconds to ping the target machine until it is available.
     * @return The interval in milliseconds.
     */
    public int getPingInterval() {
        return pingInterval;
    }

    /**
     * Sets the maximal amount of time to wait in milliseconds until the machine is available.
     * @param connectionTimeout The time to wait in milliseconds.
     */
    @DataBoundSetter
    public void setConnectionTimeout(final int connectionTimeout) {
        LOGGER.log(Level.INFO, "Set connection timeout to {0}", connectionTimeout);
        this.connectionTimeout = connectionTimeout;
    }

    /**
     * Gets the maximal amount of time to wait in milliseconds until the machine is available.
     * @return The time to wait in milliseconds.
     */
    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * Gets the broadcast IP address to use for sending the magic packet.
     * @return The broadcast IP address.
     */
    public String getBroadcastIP() {
        return broadcastIP;
    }

    /**
     * Sets the broadcast IP address to use for sending the magic packet.
     * @param broadcastIP The broadcast IP address.
     */
    @DataBoundSetter
    public void setBroadcastIP(final String broadcastIP) {
        this.broadcastIP = broadcastIP;
    }

    /**
     * Ensures that a non-null instance of {@link ComputerLauncher} is returned.
     * If {@code launcher} is not null, it is returned.
     * If {@code launcher} is null, a new instance of {@link JNLPLauncher} is returned.
     * @param launcher The input launcher. Will be returned if not null.
     * @return An instance of {@link ComputerLauncher}.
     */
    static ComputerLauncher ensureNotNullWithDefault(@Nullable final ComputerLauncher launcher) {
        if (launcher != null) {
            return launcher;
        }
        // This is the most simplest launcher to configure, so we use it as a fallback
        return new JNLPLauncher(false);
    }

    @Override
    public Computer createComputer() {
        LOGGER.log(Level.INFO,
                "Create WOLSlave computer with name {0} and type {1}",
                new Object[]{name, launcher});
        return new WOLSlaveComputer(this);
    }

    /**
     * A descriptor for {@link WOLSlave}.
     */
    @Extension
    @Symbol("wolSlave")
    public static class Descriptor extends SlaveDescriptor {

        @Override
        @Nonnull
        public final String getDisplayName() {
            return Messages.WOLSlave_SlaveDescription();
        }

        @Override
        public final boolean isInstantiable() {
            return true;
        }

        /**
         * Checks whether the given MAC address has a valid format.
         * @param macAddress The MAC address.
         * @return The result as {@link FormValidation}.
         */
        public final FormValidation doCheckMacAddress(@QueryParameter final String macAddress) {
            if (StringUtils.isBlank(macAddress)
                    || !macAddress.matches("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$")) {
                return FormValidation.error(Messages.WOLSlave_InvalidMACAddress());
            }
            return FormValidation.ok();
        }

        /**
         * Checks whether the given broadcast IP address has a valid format.
         * @param broadcastIP The broadcast IP address.
         * @return The result as {@link FormValidation}.
         */
        public final FormValidation doCheckBroadcastIP(@QueryParameter final String broadcastIP) {
            if (StringUtils.isBlank(broadcastIP) || !HostHelper.isIpAddress(broadcastIP)) {
                return FormValidation.error(Messages.WOLSlave_InvalidIPAddress());
            }
            return FormValidation.ok();
        }

        /**
         * Checks whether the given input string symbolizes a positive integer number.
         * @param str The input string to check.
         * @return In case of a validation error, a human readable error message,
         *         {@code null} otherwise.
         */
        @Nullable
        private String validatePositiveIntegerInput(final String str) {
            if (StringUtils.isBlank(str) || !str.matches("^[0-9]+$")) {
                return Messages.WOLSlave_InputMustBeInteger();
            }
            try {
                int number = Integer.parseInt(str);
                if (number <= 0) {
                    return Messages.WOLSlave_InputNumberMustBeStrictlyPositive();
                }
            } catch (NumberFormatException e) {
                return Messages.WOLSlave_InputMustBeInteger();
            }
            return null;
        }

        /**
         * Checks whether the given string is a valid configuration for the ping interval.
         * @param pingInterval The input string to check.
         * @return The result as {@link FormValidation}.
         */
        public final FormValidation doCheckPingInterval(@QueryParameter final String pingInterval) {
            String errorMessage = validatePositiveIntegerInput(pingInterval);
            if (errorMessage != null) {
                return FormValidation.error(errorMessage);
            }
            return FormValidation.ok();
        }

        /**
         * Checks whether the given string is a valid configuration for the connection timeout.
         * @param connectionTimeout The input string to check.
         * @return The result as {@link FormValidation}.
         */
        public final FormValidation doCheckConnectionTimeout(
                @QueryParameter final String connectionTimeout) {
            String errorMessage = validatePositiveIntegerInput(connectionTimeout);
            if (errorMessage != null) {
                return FormValidation.error(errorMessage);
            }
            return FormValidation.ok();
        }

    }
}
