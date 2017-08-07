/*******************************************************************************
 * Copyright 2017 jamietech
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
 *******************************************************************************/
package ch.jamiete.hilda.remindme;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import ch.jamiete.hilda.Hilda;
import ch.jamiete.hilda.plugins.HildaPlugin;
import ch.jamiete.hilda.remindme.commands.RemindCommand;

public class RemindMePlugin extends HildaPlugin {
    public static final long MAXIMUM_LENGTH = 604800000; // 1 week

    private final List<Reminder> reminders = Collections.synchronizedList(new ArrayList<Reminder>());

    public RemindMePlugin(final Hilda hilda) {
        super(hilda);
    }

    public void addReminder(final Reminder remindme) {
        this.reminders.add(remindme);
    }

    /**
     * Gets a unique ID that isn't currently registered.
     * @return A unique ID
     */
    public String getFreshID() {
        Hilda.getLogger().fine("Generating remind-me ID...");

        final String alphabet = "abcdefghijkmnpqrstuvwxyz";
        final String numbers = "23456789";
        final Random random = new Random();

        final String possibleid = String.valueOf(alphabet.charAt(random.nextInt(alphabet.length()))) + String.valueOf(numbers.charAt(random.nextInt(numbers.length())));

        for (final Reminder remindMe : this.reminders) {
            if (remindMe.getId().equals(possibleid)) {
                return this.getFreshID();
            }
        }

        Hilda.getLogger().fine("Found an ID: " + possibleid);

        return possibleid;
    }

    public Reminder getRemindMeByID(final String id) {
        return this.reminders.stream().filter(v -> v.getId().equalsIgnoreCase(id)).findFirst().orElse(null);
    }

    public List<Reminder> getRemindMes() {
        return Collections.unmodifiableList(this.reminders);
    }

    @Override
    public void onDisable() {
        this.save();
    }

    @Override
    public void onEnable() {
        this.getHilda().getCommandManager().registerChannelCommand(new RemindCommand(this.getHilda(), this));

        final File file = new File("data/reminders.hilda");

        final ArrayList<Reminder> loaded = new ArrayList<Reminder>();
        int ended = 0;
        int rejected = 0;

        if (file.exists()) {
            try {
                final FileInputStream stream = new FileInputStream(file);
                final ObjectInputStream obj = new ObjectInputStream(stream);

                @SuppressWarnings("unchecked")
                final List<Reminder> list = (List<Reminder>) obj.readObject();

                if (list != null && !list.isEmpty()) {
                    loaded.addAll(list);
                }

                obj.close();
                stream.close();

                file.delete();
            } catch (final Exception e) {
                Hilda.getLogger().log(Level.SEVERE, "Failed to load remind-mes from disk", e);
            }
        }

        for (final Reminder reminder : loaded) {
            reminder.setHilda(this.getHilda());
            reminder.setPlugin(this);

            if (this.getHilda().getBot().getUserById(reminder.getUserId()) == null) {
                rejected++;
                continue;
            }

            if (reminder.hasExpired()) {
                reminder.finish();
                ended++;
                continue;
            } else {
                final long remaining = reminder.getExpiry() - System.currentTimeMillis();
                final ScheduledFuture<?> future = this.getHilda().getExecutor().schedule(new RemindMeTimer(reminder), remaining, TimeUnit.MILLISECONDS);
                reminder.setFuture(future);
            }

            this.reminders.add(reminder);

            reminder.check();
        }

        Hilda.getLogger().info("Loaded " + this.reminders.size() + " current reminders from disk");

        if (ended > 0) {
            Hilda.getLogger().info("Loaded and ended " + ended + " expired reminders from disk");
        }

        if (rejected > 0) {
            Hilda.getLogger().info("Loaded and rejected " + rejected + " malformed reminders from disk");
        }
    }

    public void remove(final Reminder remindme) {
        this.reminders.remove(remindme);
    }

    @Override
    public void save() {
        final File folder = new File("data");

        if (!folder.isDirectory()) {
            folder.mkdir();
        }

        try {
            final File file = new File(folder, "reminders.hilda");

            if (!file.exists()) {
                file.createNewFile();
            }

            final FileOutputStream stream = new FileOutputStream("data/reminders.hilda", false);
            final ObjectOutputStream obj = new ObjectOutputStream(stream);

            obj.writeObject(this.reminders);

            Hilda.getLogger().info("Saved " + this.reminders.size() + " reminders to disk");

            obj.close();
            stream.close();
        } catch (final Exception e) {
            Hilda.getLogger().log(Level.SEVERE, "Failed to save reminders to disk", e);
        }
    }

}
