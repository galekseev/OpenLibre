package com.camomile.openlibre;

import io.realm.DynamicRealm;
import io.realm.RealmMigration;
import io.realm.RealmObjectSchema;
import io.realm.RealmSchema;

import static com.camomile.openlibre.RawDataRealmMigration.timezoneTransformFunction;

public class UserDataRealmMigration implements RealmMigration {

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
            RealmObjectSchema bloodGlucoseDataSchema = schema.get("BloodGlucoseData");
            bloodGlucoseDataSchema
                    .addField("timezoneOffsetInMinutes", int.class)
                    .transform(timezoneTransformFunction);

            //oldVersion++;
        }
    }
}
