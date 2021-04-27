package com.camomile.openlibre;

import java.util.Calendar;
import java.util.TimeZone;

import io.realm.DynamicRealm;
import io.realm.DynamicRealmObject;
import io.realm.RealmMigration;
import io.realm.RealmObjectSchema;
import io.realm.RealmSchema;

public class RawDataRealmMigration implements RealmMigration {
    static final RealmObjectSchema.Function timezoneTransformFunction = new RealmObjectSchema.Function() {
        @Override
        public void apply(DynamicRealmObject obj) {
            // set timezone offset value
            long date = obj.getLong("date");
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"));
            cal.set(2017, Calendar.JANUARY, 30, 0, 0, 0);
            TimeZone timeZone; // the timezone during data aquisition was not saved before schema version 2, so we have to guess the correct value for old data
            if (date < cal.getTime().getTime()) { // before 30.01.2017 the only existing app installation was exclusively used in germany...
                timeZone = TimeZone.getTimeZone("Europe/Berlin");
            } else {
                timeZone = TimeZone.getDefault(); // after that, use the device's current timezone
            }
            int timezoneOffsetInMinutes = timeZone.getOffset(date) / 1000 / 60;
            obj.set("timezoneOffsetInMinutes", timezoneOffsetInMinutes);
        }
    };

    @Override
    public void migrate(DynamicRealm realm, long oldVersion, long newVersion) {
        // During a migration, a DynamicRealm is exposed. A DynamicRealm is an untyped variant of a normal Realm, but
        // with the same object creation and query capabilities.
        // A DynamicRealm uses Strings instead of Class references because the Classes might not even exist or have been
        // renamed.

        // Access the Realm schema in order to create, modify or delete classes and their fields.
        RealmSchema schema = realm.getSchema();

        // Migrate from version 0 to version 1
        if (oldVersion == 0) {

            oldVersion++;
        }

        // Migrate from version 1 to version 2
        if (oldVersion == 1) {
            RealmObjectSchema rawTagDataSchema = schema.get("RawTagData");
            rawTagDataSchema
                    .addField("timezoneOffsetInMinutes", int.class)
                    .transform(timezoneTransformFunction);

            //oldVersion++;
        }

        // Migrate from version 2 to version 3
        if (oldVersion == 2) {
            RealmObjectSchema rawTagDataSchema = schema.get("RawTagData");
            rawTagDataSchema
                    .addField("tagId", String.class)
                    .transform(new RealmObjectSchema.Function() {
                        @Override
                        public void apply(DynamicRealmObject obj) {
                            String tagId = obj.getString("id").split("_")[1];
                            obj.set("tagId", tagId);
                        }
                    })
                    .removeField("sensor");
            schema.remove("SensorData");

            //oldVersion++;
        }
    }
}
