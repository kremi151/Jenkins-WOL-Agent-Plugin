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

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.TaskListener;
import hudson.plugins.sshslaves.SSHLauncher;
import hudson.plugins.sshslaves.verifiers.SshHostKeyVerificationStrategy;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.DelegatingComputerLauncher;
import hudson.slaves.SlaveComputer;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

public class WOLAgentLauncher extends DelegatingComputerLauncher {

    public WOLAgentLauncher(
            @NonNull String host,
            int port,
            String credentialsId,
            String jvmOptions,
            String javaPath,
            String prefixStartSlaveCmd,
            String suffixStartSlaveCmd,
            Integer launchTimeoutSeconds,
            Integer maxNumRetries,
            Integer retryWaitTime,
            SshHostKeyVerificationStrategy sshHostKeyVerificationStrategy
    ) {
        super(new SSHLauncher(host, port, credentialsId, jvmOptions, javaPath, prefixStartSlaveCmd, suffixStartSlaveCmd, launchTimeoutSeconds, maxNumRetries, retryWaitTime, sshHostKeyVerificationStrategy));
    }

    @DataBoundConstructor
    public WOLAgentLauncher(@NonNull String host, int port, String credentialsId) {
        super(new SSHLauncher(host, port, credentialsId));
    }

    @Override
    public boolean isLaunchSupported() {
        return launcher.isLaunchSupported();
    }

    @Override
    public void launch(SlaveComputer computer, TaskListener listener) throws IOException, InterruptedException {

        super.launch(computer, listener);
    }

}
