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

package lu.kremi151.jenkins.wolagent.launcher;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.DescriptorVisibilityFilter;
import hudson.model.TaskListener;
import hudson.remoting.Channel;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.DelegatingComputerLauncher;
import hudson.slaves.SlaveComputer;
import lu.kremi151.jenkins.wolagent.remoting.callables.RunCommand;
import lu.kremi151.jenkins.wolagent.util.HostHelper;
import lu.kremi151.jenkins.wolagent.util.WakeOnLAN;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Wake-on-LAN delegate launcher implementation.
 */
public class WOLLauncher extends DelegatingComputerLauncher {

    private static final Logger LOGGER = Logger.getLogger(WOLLauncher.class.getName());

    private static final long COOLDOWN_MS = 5000L;

    private transient String macAddress;
    private transient String broadcastIP;

    private transient int pingInterval;
    private transient int connectionTimeout;

    @Nullable
    private transient String commandBeforeDisconnect;

    /**
     * Creates an instance of {@link WOLLauncher}.
     * @param launcher the delegate {@link ComputerLauncher} to be used once the target machine has
     *                 been woken up.
     */
    @DataBoundConstructor
    public WOLLauncher(final ComputerLauncher launcher) {
        super(launcher);
    }

    /**
     * Creates an instance of {@link WOLLauncher}.
     * @param launcher                The delegate {@link ComputerLauncher} to be used once the
     *                                target machine has been woken up.
     * @param macAddress              The MAC address of the target machine.
     * @param broadcastIP             The broadcast IP address to be used for the Wake-on-LAN call.
     * @param pingInterval            The interval in milliseconds to ping the target machine until
     *                                it is available.
     * @param connectionTimeout       The maximal amount of time to wait in milliseconds until the
     *                                machine is available.
     * @param commandBeforeDisconnect The optional command to execute on the target machine before
     *                                the agent disconnects.
     */
    public WOLLauncher(
            final ComputerLauncher launcher,
            final String macAddress,
            final String broadcastIP,
            final int pingInterval,
            final int connectionTimeout,
            @Nullable final String commandBeforeDisconnect
    ) {
        this(launcher);
        this.macAddress = macAddress;
        this.broadcastIP = broadcastIP;
        this.pingInterval = pingInterval;
        this.connectionTimeout = connectionTimeout;
        this.commandBeforeDisconnect = commandBeforeDisconnect;
    }

    /**
     * Ping the target machine until it becomes available.
     * @param host The target hostname or IP address.
     * @throws InterruptedException Thrown if the pinging thread gets interrupted.
     * @throws IOException Thrown if an I/O error occurs.
     */
    private void ping(@Nullable final String host) throws InterruptedException, IOException {
        if (host == null) {
            // No host specified, so we apply a cooldown of 5 seconds
            Thread.sleep(COOLDOWN_MS);
            return;
        }
        final InetAddress address = InetAddress.getByName(host);

        final ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            final Future future = executorService.submit(() -> {
                while (true) {
                    try {
                        if (address.isReachable(pingInterval)) {
                            break;
                        }
                        Thread.sleep((long) pingInterval);
                    } catch (IOException | InterruptedException e) { }
                }
            });
            future.get(connectionTimeout, TimeUnit.MILLISECONDS);
        } catch (ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        } finally {
            if (!executorService.isTerminated()) {
                executorService.shutdown();
            }
        }
    }

    @Override
    public final void launch(final SlaveComputer computer, final TaskListener listener)
            throws IOException, InterruptedException {
        String host = null;
        try {
            host = HostHelper.tryInferHost(launcher);
            listener.getLogger().println("Inferred host name: " + host);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            LOGGER.log(Level.WARNING, "Unable to infer host via reflection from launcher", e);
            listener.getLogger().printf("Unable to infer hostname from %s (%s)\n",
                    launcher.toString(), e.getMessage());
            listener.getLogger().println("Using static cooldown instead of pinging");
        }

        if (StringUtils.isNotBlank(host) && StringUtils.isBlank(broadcastIP)) {
            listener.getLogger().println(
                    "No explicit broadcast IP specified, try to guess from the inferred host");
            try {
                broadcastIP = HostHelper.tryGuessBroadcastIp(host);
                listener.getLogger().println("Guessed broadcast IP: " + broadcastIP);
            } catch (Exception e) {
                listener.getLogger()
                        .printf("Unable to guess broadcast IP from inferred host (%s)\n",
                                e.getMessage());
            }
        }
        if (StringUtils.isBlank(broadcastIP)) {
            listener.getLogger()
                    .println("Unable to guess broadcast IP, defaulting to 192.168.0.255");
            broadcastIP = "192.168.0.255";
        }

        listener.getLogger().println("Sending magic packet, time to wake up");
        WakeOnLAN.sendMagicPacket(broadcastIP, macAddress);

        listener.getLogger().println("Pinging node");
        ping(host);

        listener.getLogger().println("Launching agent");
        super.launch(computer, listener);
    }

    /**
     * Performs the optionally configured command on the target machine before the agent
     * disconnects.
     * @param computer              The target {@link SlaveComputer}.
     * @param listener              The {@link TaskListener}, used for logging purposes.
     * @throws IOException          Thrown if an I/O error occurs.
     * @throws InterruptedException Thrown if the calling thread is interrupted before
     *                              the execution completes.
     */
    private void executePreDisconnectCommand(
            final SlaveComputer computer, final TaskListener listener)
            throws IOException, InterruptedException {
        if (StringUtils.isBlank(commandBeforeDisconnect)) {
            return;
        }
        final Channel channel = computer.getChannel();
        if (channel == null) {
            listener.error("Cannot send suspend command, channel is null");
            return;
        }
        listener.getLogger().printf("Execute command before disconnecting: %s\n",
                commandBeforeDisconnect);
        channel.call(new RunCommand(commandBeforeDisconnect));
    }

    @Override
    public final void beforeDisconnect(
            final SlaveComputer computer, final TaskListener listener) {
        try {
            executePreDisconnectCommand(computer, listener);
        } catch (IOException | InterruptedException e) {
            LOGGER.log(Level.WARNING,
                    "An exception occurred while requesting suspending on remote", e);
            listener.getLogger().printf(
                    "Could not execute suspend command on remote (%s)\n",
                    e.getMessage());
        }
        super.beforeDisconnect(computer, listener);
    }

    /**
     * @return the configured interval in milliseconds for pinging the target machine
     */
    public final int getPingInterval() {
        return pingInterval;
    }

    /**
     * Sets the interval to ping the target machine until it becomes available.
     * @param pingInterval the interval in milliseconds
     */
    public final void setPingInterval(final int pingInterval) {
        this.pingInterval = pingInterval;
    }

    /**
     * @return the timeout in milliseconds for a machine to become available
     */
    public final int getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * Sets the timeout for a machine to become available.
     * @param connectionTimeout the timeout in milliseconds
     */
    public final void setConnectionTimeout(final int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    /**
     * @return the MAC address of the target machine
     */
    public final String getMacAddress() {
        return macAddress;
    }

    /**
     * Sets the MAC address of the target machine.
     * @param macAddress the MAC address
     */
    public final void setMacAddress(final String macAddress) {
        this.macAddress = macAddress;
    }

    /**
     * @return the optional command to be executed before the agent disconnects
     */
    @Nullable
    public final String getCommandBeforeDisconnect() {
        return commandBeforeDisconnect;
    }

    /**
     * Sets the optional command to be executed before the agent disconnects.
     * @param commandBeforeDisconnect the command line to be performed
     */
    public final void setCommandBeforeDisconnect(@Nullable final String commandBeforeDisconnect) {
        this.commandBeforeDisconnect = commandBeforeDisconnect;
    }

    /**
     * Helper method to retrieve the class name of a nullable {@link ComputerLauncher} instance.
     * Used for logging purposes.
     * @param launcher the {@link ComputerLauncher} instance
     * @return the class name or {@value "null"} is the instance is {@code null}
     */
    public static String getLauncherClassName(@Nullable final ComputerLauncher launcher) {
        if (launcher == null) {
            return null;
        }
        return launcher.getClass().getName();
    }

    /**
     * Unpacks an instance of {@link WOLLauncher} in order to get the delegate
     * {@link ComputerLauncher}.
     * @param launcher an instance of {@link WOLLauncher} or {@link ComputerLauncher}
     * @return the delegate {@link ComputerLauncher} if instance is of type {@link WOLLauncher}
     *         and if it has a delegate launcher configured, {@code null} otherwise
     */
    @Nullable
    public static ComputerLauncher unpackLauncher(
            @Nullable final ComputerLauncher launcher) {
        ComputerLauncher baseLauncher = launcher;
        // Unpack WOLLauncher until we reach the base delegate launcher
        while (baseLauncher != null && baseLauncher.getClass() == WOLLauncher.class) {
            LOGGER.log(Level.WARNING, "Got launcher of type {0}, unpacking it",
                    baseLauncher.getClass().getName());
            baseLauncher = ((WOLLauncher) baseLauncher).getLauncher();
            LOGGER.log(Level.WARNING,
                    "Unwrapped launcher is of type {0}",
                    getLauncherClassName(baseLauncher));
        }
        return baseLauncher;
    }

    /**
     * Metadata about a configurable {@link WOLLauncher} instance.
     * See {@link hudson.model.Descriptor}.
     */
    @Extension
    public static final class DescriptorImpl extends Descriptor<ComputerLauncher> {

        /**
         * Creates an instance of {@link DescriptorImpl}.
         */
        public DescriptorImpl() {
            super(WOLLauncher.class);
        }

        @Override
        public String getDisplayName() {
            return null;
        }

    }

    /**
     * Custom extension of {@link DescriptorVisibilityFilter} in order to hide the configuration
     * of {@link WOLLauncher} from users.
     */
    @Extension
    public static final class DescriptorVisibilityFilterImpl extends DescriptorVisibilityFilter {

        @Override
        public boolean filter(
                @CheckForNull final Object context, @Nonnull final Descriptor descriptor) {
            return descriptor.clazz != WOLLauncher.class;
        }

        @Override
        public boolean filterType(
                @Nonnull final Class<?> contextClass, @Nonnull final Descriptor descriptor) {
            return descriptor.clazz != WOLLauncher.class;
        }

    }
}
