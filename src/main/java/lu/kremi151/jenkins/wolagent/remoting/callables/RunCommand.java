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

package lu.kremi151.jenkins.wolagent.remoting.callables;

import jenkins.security.MasterToSlaveCallable;

import java.io.IOException;

/**
 * {@link hudson.remoting.Callable} implementation which performs a command on a machine.
 */
public class RunCommand extends MasterToSlaveCallable<Void, IOException> {

    private String command;

    /**
     * Creates an instance of {@link RunCommand} without command.
     */
    public RunCommand() { }

    /**
     * Creates an instance of {@link RunCommand} with command.
     * @param command the command to execute
     */
    public RunCommand(final String command) {
        this.command = command;
    }

    @Override
    public final Void call() throws IOException {
        Runtime.getRuntime().exec(command);
        return null;
    }

}
