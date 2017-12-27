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
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.MessageBuilder.Formatting;
import net.dv8tion.jda.core.MessageBuilder.SplitPolicy;
import net.dv8tion.jda.core.entities.User;

public class Reminder implements Serializable {
    private static final long serialVersionUID = 1L;

    private transient Hilda hilda;
    private transient RemindMePlugin plugin;
    private transient ScheduledFuture<?> future;

    private String id;
    private String userId;
    private long creationTime;
    private long expiryTime;
    private String message;
    private boolean done = false;

    /**
     * Instantiates an empty Reminder. <b>Only use this where reminders are being loaded from disk.</b>
     */
    public Reminder() {

    }

    public Reminder(final Hilda hilda, final RemindMePlugin plugin) {
        this.hilda = hilda;
        this.plugin = plugin;
    }

    public void check() {
        if (this.hasExpired()) {
            this.finish();
        }
    }

    public void end() {
        this.done = true;
        this.plugin.remove(this);

        if (this.future != null) {
            this.future.cancel(false);
        }
    }

    public void finish() {
        final User user = this.hilda.getBot().getUserById(this.userId);

        if (this.done || user == null) {
            this.end();
            return;
        }

        if (!this.hasExpired()) {
            return;
        }

        final MessageBuilder mb = new MessageBuilder();
        final long ago = System.currentTimeMillis() - this.creationTime;

        mb.append(":hourglass: ");
        mb.append(user.getName(), Formatting.BOLD).append(", you asked me to remind you ");
        mb.append(this.message, Formatting.BOLD).append(" ").append(Util.getFriendlyTime(ago)).append(" ago.");

        final long difference = System.currentTimeMillis() - this.expiryTime;

        if (difference > 60000) {
            mb.append(" Because something went wrong, I didn't get to remind you until ").append(Util.getFriendlyTime(difference));
            mb.append(" later than I was meant to. Sorry.");
        }

        user.openPrivateChannel().queue(c -> mb.buildAll(SplitPolicy.SPACE).forEach(m -> c.sendMessage(m).queue()));

        this.end();
    }

    public long getCreation() {
        return this.creationTime;
    }

    public long getExpiry() {
        return this.expiryTime;
    }

    public ScheduledFuture<?> getFuture() {
        return this.future;
    }

    public String getId() {
        return this.id;
    }

    public String getMessage() {
        return this.message;
    }

    public String getUserId() {
        return this.userId;
    }

    public boolean hasExpired() {
        return System.currentTimeMillis() >= this.expiryTime;
    }

    public void setCreation(final long creation) {
        this.creationTime = creation;
    }

    public void setExpiry(final long expiry) {
        this.expiryTime = expiry;
    }

    public void setFuture(final ScheduledFuture<?> future) {
        this.future = future;
    }

    public void setHilda(final Hilda hilda) {
        this.hilda = hilda;
    }

    public void setMessage(final String message) {
        this.message = new MessageBuilder().append(message).build().getContentStripped();
    }

    public void setPlugin(final RemindMePlugin plugin) {
        this.plugin = plugin;
    }

    public void setUser(final String id) {
        this.userId = id;
    }

}
