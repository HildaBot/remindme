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
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import ch.jamiete.hilda.Hilda;
import ch.jamiete.hilda.plugins.HildaPlugin;
import ch.jamiete.hilda.remindme.commands.RemindMeBaseCommand;

public class RemindMePlugin extends HildaPlugin {
    public static final long MAXIMUM_LENGTH = 129600000; // 36 hours

    private final ArrayList<RemindMe> remindMes = new ArrayList<RemindMe>();

    public RemindMePlugin(final Hilda hilda) {
        super(hilda);
    }

    public void addRemindMe(final RemindMe remindme) {
        this.remindMes.add(remindme);
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

        for (final RemindMe remindMe : this.remindMes) {
            if (remindMe.getId().equals(possibleid)) {
                return this.getFreshID();
            }
        }

        Hilda.getLogger().fine("Found an ID: " + possibleid);

        return possibleid;
    }

    public RemindMe getRemindMeByID(final String id) {
        return this.remindMes.stream().filter(v -> v.getId().equalsIgnoreCase(id)).findFirst().orElse(null);
    }

    public List<RemindMe> getRemindMes() {
        return Collections.unmodifiableList(this.remindMes);
    }

    @Override
    public void onDisable() {
        this.save();
    }

    @Override
    public void save() {
        final File folder = new File("data");

        if (!folder.isDirectory()) {
            folder.mkdir();
        }

        try {
            final File file = new File(folder, "remindmes.hilda");

            if (!file.exists()) {
                file.createNewFile();
            }

            final FileOutputStream stream = new FileOutputStream("data/remindmes.hilda", false);
            final ObjectOutputStream obj = new ObjectOutputStream(stream);

            obj.writeObject(this.remindMes);

            Hilda.getLogger().info("Saved " + this.remindMes.size() + " remind-mes to disk");

            obj.close();
            stream.close();
        } catch (final Exception e) {
            Hilda.getLogger().log(Level.SEVERE, "Failed to save remind-mes to disk", e);
        }
    }

    @Override
    public void onEnable() {
        this.getHilda().getCommandManager().registerChannelCommand(new RemindMeBaseCommand(this.getHilda(), this));

        final File file = new File("data/remindmes.hilda");

        final ArrayList<RemindMe> loaded = new ArrayList<RemindMe>();
        int ended = 0;
        int rejected = 0;


        if (file.exists()) {
            try {
                final FileInputStream stream = new FileInputStream(file);
                final ObjectInputStream obj = new ObjectInputStream(stream);

                final ArrayList<RemindMe> list = (ArrayList<RemindMe>) obj.readObject();

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

        for (final RemindMe remindme : loaded) {
            remindme.setHilda(this.getHilda());
            remindme.setPlugin(this);

            if (this.getHilda().getBot().getTextChannelById(remindme.getChannelId()) == null) {
                rejected++;
                continue;
            }

            if (System.currentTimeMillis() >= remindme.getRemindMeDate()) {
                remindme.finish();
                ended++;
                continue;
            } else {
                final long remaining = remindme.getRemindMeDate() - System.currentTimeMillis();
                final ScheduledFuture<?> future = this.getHilda().getExecutor().schedule(new RemindMeTimer(remindme), remaining, TimeUnit.MILLISECONDS);
                remindme.setFuture(future);
            }

            this.remindMes.add(remindme);

            remindme.check();
        }

        Hilda.getLogger().info("Loaded " + this.remindMes.size() + " current remind-mes from disk");

        if (ended > 0) {
            Hilda.getLogger().info("Loaded and ended " + ended + " expired remind-mes from disk");
        }

        if (rejected > 0) {
            Hilda.getLogger().info("Loaded and rejected " + rejected + " malformed remind-mes from disk");
        }
    }

    public void remove(final RemindMe remindme) {
        this.remindMes.remove(remindme);
    }

}
