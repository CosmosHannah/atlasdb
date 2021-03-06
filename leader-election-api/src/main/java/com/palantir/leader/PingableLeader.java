/*
 * Copyright 2015 Palantir Technologies
 *
 * Licensed under the BSD-3 License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.palantir.leader;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/leader")
public interface PingableLeader {

    /**
     * If this call returns then the server is reachable.
     *
     * @return true if the remote server thinks it is the leader, otherwise false
     */
    @GET
    @Path("ping")
    @Produces(MediaType.APPLICATION_JSON)
    boolean ping();

    /**
     * @return a unique string identifier for the leader election service
     */
    @GET
    @Path("uuid")
    @Produces(MediaType.TEXT_PLAIN)
    String getUUID();
}
