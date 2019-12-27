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

public class Suspend extends MasterToSlaveCallable<Void, IOException> {

    private boolean superuser;
    private boolean ignoreSessions;

    public Suspend(){}

    public Suspend(boolean superuser, boolean ignoreSessions) {
        this.superuser = superuser;
        this.ignoreSessions = ignoreSessions;
    }

    @Override
    public Void call() throws IOException {
        String suspendCommand = "systemctl suspend";
        if (superuser) {
            suspendCommand = "sudo " + suspendCommand;
        }
        if (ignoreSessions) {
            suspendCommand = suspendCommand + " -i";
        }
        Runtime.getRuntime().exec(suspendCommand);
        return null;
    }

}
