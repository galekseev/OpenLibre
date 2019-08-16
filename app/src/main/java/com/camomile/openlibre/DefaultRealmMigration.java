package com.camomile.openlibre;

import java.util.concurrent.TimeUnit;

import io.realm.DynamicRealm;
import io.realm.DynamicRealmObject;
import io.realm.RealmMigration;
import io.realm.RealmObjectSchema;
import io.realm.RealmSchema;

class DefaultRealmMigration implements RealmMigration {
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
            RealmObjectSchema rawTagDataSchema = schema.get("RawTagData");
            rawTagDataSchema
                    .removeField("readingData")
                    .addField("id_temp", String.class)
                    .transform(new RealmObjectSchema.Function() {
                        @Override
                        public void apply(DynamicRealmObject obj) {
                            // cut off prefix "raw_tag_"
                            obj.set("id_temp", obj.getString("id").substring(8));
                        }
                    })
                    .removeField("id")
                    .renameField("id_temp", "id")
                    .addPrimaryKey("id");

            RealmObjectSchema readingDataSchema = schema.get("ReadingData");
            readingDataSchema
                    .removeField("rawTagData")
                    .addField("id_temp", String.class)
                    .transform(new RealmObjectSchema.Function() {
                        @Override
                        public void apply(DynamicRealmObject obj) {
                            // cut off prefix "reading_"
                            obj.set("id_temp", obj.getString("id").substring(8));
                        }
                    })
                    .removeField("id")
                    .renameField("id_temp", "id")
                    .addPrimaryKey("id");

            oldVersion++;
        }

        // Migrate from version 1 to version 2
        if (oldVersion == 1) {
            RealmObjectSchema glucoseDataSchema = schema.get("GlucoseData");
            glucoseDataSchema
                    .addField("date", long.class)
                    .transform(new RealmObjectSchema.Function() {
                        @Override
                        public void apply(DynamicRealmObject obj) {
                            // calculate date value
                            obj.set("date", obj.getObject("sensor").getLong("startDate") +
                                    TimeUnit.MINUTES.toMillis(obj.getInt("ageInSensorMinutes")));
                        }
                    });

            //oldVersion++;
        }
    }
}
