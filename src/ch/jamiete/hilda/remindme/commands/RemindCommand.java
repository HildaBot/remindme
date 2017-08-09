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
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import ch.jamiete.hilda.Hilda;
import ch.jamiete.hilda.Util;
import ch.jamiete.hilda.commands.ChannelCommand;
import ch.jamiete.hilda.remindme.RemindMePlugin;
import ch.jamiete.hilda.remindme.RemindMeTimer;
import ch.jamiete.hilda.remindme.Reminder;
import ch.jamiete.hilda.remindme.time.TimeBundle;
import ch.jamiete.hilda.remindme.time.TimeParseException;
import ch.jamiete.hilda.remindme.time.TimeParser;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.MessageBuilder.Formatting;
import net.dv8tion.jda.core.entities.Message;

public class RemindCommand extends ChannelCommand {
    private final RemindMePlugin plugin;

    public RemindCommand(final Hilda hilda, final RemindMePlugin plugin) {
        super(hilda);

        this.plugin = plugin;
        this.setName("remind");
        this.setAliases(Arrays.asList(new String[] { "remindme" }));
        this.setDescription("Remind me system.");
    }

    @Override
    public void execute(final Message message, String[] args, final String label) {
        if (args.length == 0) {
            this.usage(message, "<time...> <reminder...>", label);
            return;
        }

        if (args[0].equalsIgnoreCase("me")) {
            args = Arrays.copyOfRange(args, 1, args.length);
        }

        TimeBundle bundle = null;
        final long creation = System.currentTimeMillis();

        try {
            bundle = TimeParser.getTimeBundle(args);
        } catch (final TimeParseException e) {
            this.reply(message, e.getMessage());
            return;
        }

        if (bundle.getRejects().length == 0) {
            this.usage(message, "<time...> <reminder...>", label);
            return;
        }

        if (bundle.getTime() < 0) {
            this.reply(message, "You can't set a reminder for the past!");
            return;
        }

        if (bundle.getTime() > RemindMePlugin.MAXIMUM_LENGTH) {
            this.reply(message, "You can't set a reminder that far in the future! Please make it less than " + Util.getFriendlyTime(RemindMePlugin.MAXIMUM_LENGTH) + " in the future.");
            return;
        }

        final Reminder reminder = new Reminder(this.hilda, this.plugin);
        reminder.setCreation(creation);
        reminder.setExpiry(creation + bundle.getTime());
        reminder.setMessage(Util.combineSplit(0, bundle.getRejects(), " ").trim());
        reminder.setUser(message.getAuthor().getId());

        final ScheduledFuture<?> future = this.hilda.getExecutor().schedule(new RemindMeTimer(reminder), bundle.getTime(), TimeUnit.MILLISECONDS);
        reminder.setFuture(future);

        this.plugin.addReminder(reminder);

        final MessageBuilder mb = new MessageBuilder();

        mb.append(":hourglass_flowing_sand: OK ").append(message.getAuthor().getName(), Formatting.BOLD).append(", ");
        mb.append("I'll remind you about ").append(Util.sanitise(reminder.getMessage()), Formatting.BOLD).append(" in ");
        mb.append(Util.getFriendlyTime(bundle.getTime()), Formatting.BOLD).append("!");

        this.reply(message, mb.build());
    }

}
