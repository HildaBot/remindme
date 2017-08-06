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

public class TimeUtils {

    public static final String TIMEPARSE_INFO = "Example: 1h for one hour. Valid types: [s]econds, [m]inutes, [h]ours, [d]ays, [w]eeks";

    public static class ParseTimeException extends Exception {

        public ParseTimeException(String message) {
            super(message);
        }

    }

    public static long parseTime(String[] args) throws ParseTimeException {
        if (args.length == 0) {
            throw new ParseTimeException("Invalid time parameter. " + TIMEPARSE_INFO);
        }
        long time = 0;
        for (String str : args) {
            try {
                long unit = Long.parseLong(str.substring(0, str.length() - 1));
                String unitType = str.substring(str.length() - 1, str.length());
                switch (unitType) {
                    case "s":
                        time += unit * 1000;
                        break;
                    case "m":
                        time += unit * 1000 * 60;
                        break;
                    case "h":
                        time += unit * 1000 * 60 * 60;
                        break;
                    case "d":
                        time += unit * 1000 * 60 * 60 * 24;
                        break;
                    case "w":
                        time += unit * 1000 * 60 * 60 * 24 * 7;
                        break;
                    default:
                        throw new ParseTimeException("Invalid unit type " + unitType + ". " + TIMEPARSE_INFO);
                }
            } catch (NumberFormatException ex) {
                throw new ParseTimeException("Could not parse number " + str + ". " + TIMEPARSE_INFO);
            }
        }
        return time;
    }
}
