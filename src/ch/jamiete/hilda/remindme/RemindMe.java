/**
 * *****************************************************************************
 * Copyright 2017 jamietech
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * *****************************************************************************
 */
package ch.jamiete.hilda.remindme;

import java.io.Serializable;
import java.util.concurrent.ScheduledFuture;
import ch.jamiete.hilda.Hilda;
import ch.jamiete.hilda.Util;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

public class RemindMe implements Serializable {

    private static final long serialVersionUID = 1L;

    transient Hilda hilda;
    transient RemindMePlugin plugin;
    private transient ScheduledFuture<?> future;

    private String id;
    private String userId;
    private String channelId;
    private long createDate;
    private long remindMeDate;
    private String message;
    private boolean done = false;

    public RemindMe() {
    }

    public RemindMe(final Hilda hilda, final RemindMePlugin plugin) {
        this.hilda = hilda;
        this.plugin = plugin;
    }

    public void setDetails(String userId, String channelId) {
        this.userId = userId;
        this.channelId = channelId;
    }

    private boolean isValid() {
        User user = hilda.getBot().getUserById(userId);
        TextChannel channel = hilda.getBot().getTextChannelById(channelId);
        if (user != null && channel != null && channel.getGuild() != null) {
            Member mem = channel.getGuild().getMember(user);
            if (mem != null && mem.hasPermission(channel, Permission.MESSAGE_READ)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if user and channel are still valid
     */
    public void check() {
        if (!isValid()) {
            finish();
        }
    }

    public void finish() {
        User user = hilda.getBot().getUserById(userId);
        TextChannel channel = hilda.getBot().getTextChannelById(channelId);

        if (!isValid() || done) {
            done = true;
            plugin.remove(this);
            return;
        }

        if (System.currentTimeMillis() < remindMeDate) {
            return;
        }

        String msg = " You told me to remind you " + Util.getFriendlyTime(System.currentTimeMillis() - createDate) + "!";
        if (this.message != null && !this.message.isEmpty()) {
            msg += " " + this.message;
        }
        channel.sendMessage(user.getAsMention() + msg).submit();
        done = true;
        plugin.remove(this);
    }

    public void setHilda(Hilda hilda) {
        this.hilda = hilda;
    }

    public void setPlugin(RemindMePlugin plugin) {
        this.plugin = plugin;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public long getRemindMeDate() {
        return remindMeDate;
    }

    public void setRemindMeDate(long remindMeDate) {
        this.remindMeDate = remindMeDate;
    }

    public long getCreateDate() {
        return createDate;
    }

    public void setCreateDate(long createDate) {
        this.createDate = createDate;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setFuture(ScheduledFuture<?> future) {
        this.future = future;
    }

    public ScheduledFuture<?> getFuture() {
        return future;
    }

}
