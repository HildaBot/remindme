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
package ch.jamiete.hilda.remindme.commands;

import java.util.Arrays;
import ch.jamiete.hilda.Hilda;
import ch.jamiete.hilda.Util;
import ch.jamiete.hilda.commands.ChannelSeniorCommand;
import ch.jamiete.hilda.remindme.RemindMe;
import ch.jamiete.hilda.remindme.RemindMePlugin;
import ch.jamiete.hilda.remindme.RemindMeTimer;
import ch.jamiete.hilda.remindme.TimeUtils;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.core.entities.Message;

public class RemindMeBaseCommand extends ChannelSeniorCommand {

    private final RemindMePlugin plugin;

    public RemindMeBaseCommand(final Hilda hilda, final RemindMePlugin plugin) {
        super(hilda);

        this.plugin = plugin;
        this.setName("remindme");
        this.setAliases(Arrays.asList(new String[]{"rm"}));
        this.setDescription("Remind me system.");

        // do subcommands?
    }

    @Override
    public void execute(Message message, String[] args, String label) {
        try {
            if (args.length == 0) {
                this.reply(message, "No args");
                return;
            }

            final RemindMe reminder = new RemindMe(this.hilda, this.plugin);
            reminder.setCreateDate(System.currentTimeMillis());
            reminder.setRemindMeDate(System.currentTimeMillis() + TimeUtils.parseTime(new String[]{args[0]}));
            reminder.setMessage(Util.combineSplit(1, args, " ").trim());
            reminder.setDetails(message.getAuthor().getId(), message.getTextChannel().getId());

            long duration = reminder.getRemindMeDate() - reminder.getCreateDate();
            if (duration < 0) {
                this.reply(message, "Time must be in the future");
                return;
            }

            final ScheduledFuture<?> future = this.hilda.getExecutor().schedule(new RemindMeTimer(reminder), duration, TimeUnit.MILLISECONDS);
            reminder.setFuture(future);

            this.plugin.addRemindMe(reminder);
            this.reply(message, message.getAuthor().getAsMention() + " Will remind you in " + Util.getFriendlyTime(duration));
        } catch (TimeUtils.ParseTimeException ex) {
            this.reply(message, message.getAuthor().getAsMention() + " " + ex.getMessage());
        }
    }

}
